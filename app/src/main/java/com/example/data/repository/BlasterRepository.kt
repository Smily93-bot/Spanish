package com.example.data.repository

import com.example.flavor.tl
import android.content.Context
import com.example.data.content.SpanishContent
import com.example.data.database.*
import com.example.data.model.SHIP_TIERS
import com.example.data.model.ReadingTablet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Outcome of a finished arcade run or tablet, shown to the player as rewards. */
data class RewardResult(
    val xpGained: Int,
    val creditsGained: Int,
    val leveledUp: Boolean,
    val newLevel: Int,
    val isNewPersonalBest: Boolean = false,
    val rankGrade: String = "",
    val unlockedMilestones: List<MilestoneGoalEntity> = emptyList()
)

/**
 * Single source of truth: owns the Room database and the bundled offline content.
 * Everything runs locally — there are no network calls and no API keys.
 */
class BlasterRepository(private val dao: AppDao) {

    private val writeLock = Mutex()
    @Volatile private var content: SpanishContent? = null

    val userProgress: Flow<UserProgressEntity?> = dao.getUserProgress()
    val tabletProgress: Flow<List<TabletProgressEntity>> = dao.getAllTabletProgress()
    val recentArcadeScores: Flow<List<ArcadeScoreEntity>> = dao.getRecentArcadeScores()
    val topArcadeHighScores: Flow<List<ArcadeScoreEntity>> = dao.getTopArcadeHighScores()
    val overallPersonalBest: Flow<Int> = dao.getOverallPersonalBestScore().map { it ?: 0 }
    val arcadeStats: Flow<List<ArcadeModeStatsEntity>> = dao.getAllArcadeStats()
    val wordMastery: Flow<List<WordMasteryEntity>> = dao.getAllWordMastery()
    val milestones: Flow<List<MilestoneGoalEntity>> = dao.getAllMilestones()

    suspend fun loadContent(context: Context): SpanishContent = withContext(Dispatchers.IO) {
        content ?: SpanishContent.load(context).also { content = it }
    }

    /** Creates the player profile and milestone list on first launch. */
    suspend fun ensureInitialized() = writeLock.withLock {
        if (dao.fetchUserProgress() == null) dao.insertOrUpdateUserProgress(UserProgressEntity())
        val existing = dao.fetchAllMilestones().associateBy { it.goalId }
        MILESTONES.forEach { goal -> if (goal.goalId !in existing) dao.insertOrUpdateMilestone(goal) }
    }

    // ------------------------------------------------------------------ Progression

    private suspend fun progress(): UserProgressEntity = dao.fetchUserProgress() ?: UserProgressEntity()

    /** Adds XP and credits, levelling up as many times as needed. Caller must hold [writeLock]. */
    private suspend fun grant(xp: Int, credits: Int): Pair<UserProgressEntity, Boolean> {
        var p = progress()
        var level = p.level
        var currentXp = p.currentXp + xp
        var toNext = p.xpToNextLevel
        var leveledUp = false
        while (currentXp >= toNext) {
            currentXp -= toNext
            level += 1
            toNext = xpForLevel(level)
            leveledUp = true
        }
        p = p.copy(
            level = level,
            currentXp = currentXp,
            xpToNextLevel = toNext,
            starCredits = p.starCredits + credits + if (leveledUp) 50 else 0
        )
        dao.insertOrUpdateUserProgress(p)
        return p to leveledUp
    }

    suspend fun recordArcadeRun(gameMode: String, score: Int, wordsBlasted: Int, maxStreak: Int): RewardResult =
        writeLock.withLock {
            val previousBest = dao.fetchPersonalBestScoreForMode(gameMode) ?: 0
            val grade = rankGrade(score)
            dao.insertArcadeScore(
                ArcadeScoreEntity(
                    gameMode = gameMode,
                    score = score,
                    wordsBlasted = wordsBlasted,
                    maxComboStreak = maxStreak,
                    rankGrade = grade
                )
            )
            val stats = dao.getStatsForMode(gameMode) ?: ArcadeModeStatsEntity(gameMode)
            dao.insertOrUpdateArcadeStats(
                stats.copy(
                    totalGamesPlayed = stats.totalGamesPlayed + 1,
                    totalWordsBlasted = stats.totalWordsBlasted + wordsBlasted,
                    highScore = maxOf(stats.highScore, score),
                    bestComboStreak = maxOf(stats.bestComboStreak, maxStreak)
                )
            )
            val xp = 20 + score / 8
            val credits = 10 + score / 25
            val (p, leveledUp) = grant(xp, credits)
            val unlocked = refreshMilestones()
            RewardResult(
                xpGained = xp,
                creditsGained = credits,
                leveledUp = leveledUp,
                newLevel = p.level,
                isNewPersonalBest = score > previousBest && score > 0,
                rankGrade = grade,
                unlockedMilestones = unlocked
            )
        }

    suspend fun completeTablet(tablet: ReadingTablet, correct: Int, total: Int, bonusCredits: Int = 0): RewardResult = writeLock.withLock {
        val existing = dao.getTabletProgressById(tablet.id)
        val score = if (total == 0) 100 else correct * 100 / total
        val firstClear = existing?.isCompleted != true
        dao.insertOrUpdateTabletProgress(
            TabletProgressEntity(
                tabletId = tablet.id,
                tabletTitle = tablet.title,
                isCompleted = true,
                timesCompleted = (existing?.timesCompleted ?: 0) + 1,
                bestScore = maxOf(existing?.bestScore ?: 0, score),
                questionsAnsweredCorrectly = maxOf(existing?.questionsAnsweredCorrectly ?: 0, correct),
                totalQuestions = total,
                lastCompletedTimestamp = System.currentTimeMillis()
            )
        )
        val xp = (if (firstClear) 150 else 40) + correct * 15
        val credits = (if (firstClear) 120 else 25) + bonusCredits
        val (p0, leveledUp) = grant(xp, credits)
        if (firstClear) dao.insertOrUpdateUserProgress(p0.copy(completedTabletsCount = p0.completedTabletsCount + 1))
        val unlocked = refreshMilestones()
        RewardResult(xp, credits, leveledUp, p0.level, rankGrade = rankGrade(score * 20), unlockedMilestones = unlocked)
    }

    /** Small XP reward for practice modes that don't produce an arcade score. */
    suspend fun rewardPractice(xp: Int, credits: Int): RewardResult = writeLock.withLock {
        val (p, leveledUp) = grant(xp, credits)
        RewardResult(xp, credits, leveledUp, p.level, unlockedMilestones = refreshMilestones())
    }

    suspend fun recordWordResult(spanish: String, english: String, category: String, correct: Boolean) =
        writeLock.withLock {
            val key = spanish.trim()
            val old = dao.getWordMastery(key)
            val encountered = (old?.timesEncountered ?: 0) + 1
            val timesCorrect = (old?.timesCorrect ?: 0) + if (correct) 1 else 0
            val misses = encountered - timesCorrect
            val mastery = (1 + timesCorrect / 2 - misses / 3).coerceIn(1, 5)
            dao.insertOrUpdateWordMastery(
                WordMasteryEntity(
                    spanishWord = key,
                    englishWord = english,
                    category = category,
                    timesEncountered = encountered,
                    timesCorrect = timesCorrect,
                    masteryLevel = mastery,
                    lastSeenTimestamp = System.currentTimeMillis()
                )
            )
            val p = progress()
            val mastered = dao.countMasteredWords()
            if (p.totalWordsMastered != mastered) dao.insertOrUpdateUserProgress(p.copy(totalWordsMastered = mastered))
        }

    /** Buys the next ship tier. Returns false when credits are insufficient or the ship is maxed. */
    suspend fun upgradeShip(): Boolean = writeLock.withLock {
        val p = progress()
        val next = SHIP_TIERS.firstOrNull { it.tier == p.shipTier + 1 } ?: return@withLock false
        if (p.starCredits < next.cost) return@withLock false
        dao.insertOrUpdateUserProgress(p.copy(starCredits = p.starCredits - next.cost, shipTier = next.tier))
        refreshMilestones()
        true
    }

    suspend fun setSoundEnabled(enabled: Boolean) = writeLock.withLock {
        dao.insertOrUpdateUserProgress(progress().copy(soundEnabled = enabled))
    }

    suspend fun setSpeechSpeed(speed: Float) = writeLock.withLock {
        dao.insertOrUpdateUserProgress(progress().copy(speechSpeed = speed))
    }

    suspend fun fetchLocalHighScores(filter: String): List<ArcadeScoreEntity> =
        if (filter == "ALL") dao.fetchTopArcadeScores(10) else dao.fetchTopArcadeScoresForMode(filter, 10)

    suspend fun fetchPersonalBest(filter: String): Int =
        (if (filter == "ALL") dao.fetchPersonalBestScore() else dao.fetchPersonalBestScoreForMode(filter)) ?: 0

    // ------------------------------------------------------------------ Milestones

    /** Recomputes milestone progress; returns milestones unlocked by this call. Caller must hold [writeLock]. */
    private suspend fun refreshMilestones(): List<MilestoneGoalEntity> {
        val p = progress()
        val bestScore = dao.fetchPersonalBestScore() ?: 0
        val gamesPlayed = MODES.sumOf { dao.getStatsForMode(it)?.totalGamesPlayed ?: 0 }
        val mastered = dao.countMasteredWords()
        val unlocked = mutableListOf<MilestoneGoalEntity>()
        var bonus = 0
        dao.fetchAllMilestones().forEach { goal ->
            val value = when (goal.goalId.substringBefore('_')) {
                "games" -> gamesPlayed
                "score" -> bestScore
                "tablets" -> p.completedTabletsCount
                "words" -> mastered
                "level" -> p.level
                "ship" -> p.shipTier
                else -> goal.currentProgress
            }
            val nowUnlocked = value >= goal.targetValue
            if (value != goal.currentProgress || nowUnlocked != goal.isUnlocked) {
                val updated = goal.copy(
                    currentProgress = minOf(value, goal.targetValue),
                    isUnlocked = goal.isUnlocked || nowUnlocked,
                    unlockedTimestamp = if (!goal.isUnlocked && nowUnlocked) System.currentTimeMillis() else goal.unlockedTimestamp
                )
                dao.insertOrUpdateMilestone(updated)
                if (!goal.isUnlocked && nowUnlocked) {
                    unlocked += updated
                    bonus += goal.rewardCredits
                }
            }
        }
        if (bonus > 0) {
            val latest = progress()
            dao.insertOrUpdateUserProgress(latest.copy(starCredits = latest.starCredits + bonus))
        }
        return unlocked
    }

    companion object {
        val MODES = listOf("TRANSLATION", "SYNONYM", "ANTONYM", "CLOZE", "REACTOR")

        /** Arabic descriptions for [MILESTONES] (the stored description is English). */
        val MILESTONE_DESCRIPTIONS_AR = mapOf(
            "games_1" to "العبي أول جولة في الألعاب",
            "games_10" to "العبي 10 جولات",
            "games_50" to "العبي 50 جولة",
            "score_500" to "اجمعي 500 نقطة في جولة واحدة",
            "score_2000" to "اجمعي 2000 نقطة في جولة واحدة",
            "tablets_1" to "أكملي أول بعثة مع ليا",
            "tablets_6" to "أكملي 6 بعثات",
            "tablets_12" to "أكملي البعثات الاثنتي عشرة",
            "words_25" to "أتقني 25 كلمة",
            "words_100" to "أتقني 100 كلمة",
            "words_300" to "أتقني 300 كلمة",
            "level_5" to "بلغي المستوى 5",
            "level_10" to "بلغي المستوى 10",
            "ship_3" to "رقّي سفينتك إلى الفئة 3"
        )

        fun xpForLevel(level: Int) = 300 + (level - 1) * 150

        fun rankGrade(score: Int) = when {
            score >= 2000 -> "S+"
            score >= 1200 -> "S"
            score >= 700 -> "A"
            score >= 300 -> "B"
            else -> "C"
        }

        val MILESTONES = listOf(
            MilestoneGoalEntity("games_1", tl("Primer vuelo"), "Play your first arcade game", targetValue = 1, rewardCredits = 50),
            MilestoneGoalEntity("games_10", tl("Piloto constante"), "Play 10 arcade games", targetValue = 10, rewardCredits = 120),
            MilestoneGoalEntity("games_50", tl("Leyenda del arcade"), "Play 50 arcade games", targetValue = 50, rewardCredits = 400),
            MilestoneGoalEntity("score_500", tl("Lluvia de meteoros"), "Score 500 points in one run", targetValue = 500, rewardCredits = 80),
            MilestoneGoalEntity("score_2000", tl("Rango S+"), "Score 2000 points in one run", targetValue = 2000, rewardCredits = 300),
            MilestoneGoalEntity("tablets_1", tl("Primera señal"), "Complete your first expedition with Lía", targetValue = 1, rewardCredits = 80),
            MilestoneGoalEntity("tablets_6", tl("Mitad del atlas"), "Complete 6 expeditions", targetValue = 6, rewardCredits = 250),
            MilestoneGoalEntity("tablets_12", tl("Vuelo a casa"), "Complete all 12 expeditions", targetValue = 12, rewardCredits = 600),
            MilestoneGoalEntity("words_25", tl("Léxico en marcha"), "Master 25 words", targetValue = 25, rewardCredits = 100),
            MilestoneGoalEntity("words_100", tl("Cien palabras"), "Master 100 words", targetValue = 100, rewardCredits = 250),
            MilestoneGoalEntity("words_300", tl("Diccionario viviente"), "Master 300 words", targetValue = 300, rewardCredits = 500),
            MilestoneGoalEntity("level_5", tl("Nivel 5"), "Reach explorer level 5", targetValue = 5, rewardCredits = 100),
            MilestoneGoalEntity("level_10", tl("Nivel 10"), "Reach explorer level 10", targetValue = 10, rewardCredits = 250),
            MilestoneGoalEntity("ship_3", tl("Nave mejorada"), "Upgrade your ship to tier 3", targetValue = 3, rewardCredits = 150)
        )
    }
}
