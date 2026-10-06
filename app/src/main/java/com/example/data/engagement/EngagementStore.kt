package com.example.data.engagement

import android.content.Context
import android.content.SharedPreferences
import com.example.widget.StreakWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** The three steps of the daily path on the home screen, in the order they are suggested. */
enum class PathStep { WORDS, GRAMMAR, STORY }

/** Daily reminder preferences. */
data class ReminderSettings(val enabled: Boolean = false, val hour: Int = 19)

/**
 * Persists the streak, Word Galaxy memory cards and reminder settings.
 *
 * The streak lives in SharedPreferences so the home-screen widget and the reminder receiver can read it
 * without starting the app's database; memory cards live in a small text file.
 */
class EngagementStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val cardsFile = File(appContext.filesDir, "word_galaxy.txt")
    private val lock = Mutex()

    private val _streak = MutableStateFlow(readStreak(prefs))
    val streak: StateFlow<StreakState> = _streak.asStateFlow()

    private val _cards = MutableStateFlow<Map<Int, WordCard>>(emptyMap())
    val cards: StateFlow<Map<Int, WordCard>> = _cards.asStateFlow()

    private val _reminder = MutableStateFlow(
        ReminderSettings(prefs.getBoolean(K_REMIND_ON, false), prefs.getInt(K_REMIND_HOUR, 19))
    )
    val reminder: StateFlow<ReminderSettings> = _reminder.asStateFlow()

    private val _pathDone = MutableStateFlow(readPath())
    /** Steps of today's path already completed. */
    val pathDone: StateFlow<Set<PathStep>> = _pathDone.asStateFlow()

    private val _grammarStars = MutableStateFlow(readGrammarStars())
    /** Best stars (0–3) per grammar topic id. */
    val grammarStars: StateFlow<Map<String, Int>> = _grammarStars.asStateFlow()

    private fun readGrammarStars(): Map<String, Int> =
        prefs.getString(K_GRAMMAR_STARS, "").orEmpty().split(',').mapNotNull { entry ->
            val i = entry.lastIndexOf(':')
            if (i <= 0) null else entry.substring(0, i) to (entry.substring(i + 1).toIntOrNull() ?: 0)
        }.toMap()

    /** Keeps the best result; returns true when [stars] beats the previous best. */
    fun saveGrammarStars(topicId: String, stars: Int): Boolean {
        val old = _grammarStars.value[topicId] ?: 0
        if (stars <= old) return false
        val next = _grammarStars.value + (topicId to stars)
        prefs.edit().putString(K_GRAMMAR_STARS, next.entries.joinToString(",") { "${it.key}:${it.value}" }).apply()
        _grammarStars.value = next
        return true
    }

    private var cardsLoaded = false

    private fun readPath(): Set<PathStep> =
        if (prefs.getLong(K_PATH_DAY, -1) != today()) emptySet()
        else prefs.getString(K_PATH_DONE, "").orEmpty().split(',')
            .mapNotNull { name -> PathStep.entries.firstOrNull { it.name == name } }.toSet()

    fun completeStep(step: PathStep) {
        val next = readPath() + step
        prefs.edit().putLong(K_PATH_DAY, today()).putString(K_PATH_DONE, next.joinToString(",") { it.name }).apply()
        _pathDone.value = next
    }

    suspend fun loadCards() = lock.withLock {
        if (cardsLoaded) return@withLock
        _cards.value = withContext(Dispatchers.IO) { readCards() }
        cardsLoaded = true
    }

    /** Re-applies missed days, e.g. when the app opens on a new day. */
    suspend fun refreshDay() = lock.withLock {
        _pathDone.value = readPath()
        val rolled = _streak.value.rollTo(today())
        if (rolled != _streak.value) saveStreak(rolled)
    }

    suspend fun addXp(xp: Int): StreakEvent = lock.withLock {
        val (next, event) = _streak.value.addXp(today(), xp)
        saveStreak(next)
        event
    }

    suspend fun setDailyGoal(goal: Int) = lock.withLock {
        saveStreak(_streak.value.copy(dailyGoal = goal))
    }

    suspend fun setReminder(settings: ReminderSettings) = lock.withLock {
        prefs.edit().putBoolean(K_REMIND_ON, settings.enabled).putInt(K_REMIND_HOUR, settings.hour).apply()
        _reminder.value = settings
    }

    /** Stores updated memory cards and refreshes the widget's review counts. */
    suspend fun updateCards(updated: Collection<WordCard>) = lock.withLock {
        if (updated.isEmpty()) return@withLock
        val next = _cards.value + updated.associateBy { it.rank }
        _cards.value = next
        withContext(Dispatchers.IO) { writeCards(next.values) }
        prefs.edit()
            .putString(K_DUE_HISTOGRAM, dueHistogram(next.values))
            .putInt(K_LEARNED, next.values.count { it.introduced })
            .putInt(K_MEMORIZED, next.values.count { it.memorized })
            .apply()
        StreakWidget.requestUpdate(appContext)
    }

    /** Words the widget shows as "word of the day", precomputed so the widget never parses vocab. */
    fun saveWordOfDay(entries: Map<Long, Pair<String, String>>) {
        prefs.edit().putString(K_WORD_OF_DAY, entries.entries.joinToString("\n") { (day, w) ->
            "$day\t${w.first}\t${w.second}"
        }).apply()
        StreakWidget.requestUpdate(appContext)
    }

    /** The widget and reminder show their text in the player's helper language. */
    fun setHelperArabic(arabic: Boolean) {
        prefs.edit().putBoolean(K_HELPER_ARABIC, arabic).apply()
        StreakWidget.requestUpdate(appContext)
    }

    private fun saveStreak(state: StreakState) {
        _streak.value = state
        writeStreak(prefs, state)
        StreakWidget.requestUpdate(appContext)
    }

    private fun readCards(): Map<Int, WordCard> {
        if (!cardsFile.exists()) return emptyMap()
        return cardsFile.readLines().mapNotNull { line ->
            val f = line.split(',')
            if (f.size < 5) null else runCatching {
                WordCard(f[0].toInt(), f[1].toInt(), f[2].toLong(), f[3].toInt(), f[4].toInt())
            }.getOrNull()
        }.associateBy { it.rank }
    }

    private fun writeCards(cards: Collection<WordCard>) {
        val tmp = File(cardsFile.parentFile, cardsFile.name + ".tmp")
        tmp.writeText(cards.sortedBy { it.rank }.joinToString("\n") { "${it.rank},${it.box},${it.dueDay},${it.seen},${it.correct}" })
        if (!tmp.renameTo(cardsFile)) {
            cardsFile.writeText(tmp.readText())
            tmp.delete()
        }
    }

    companion object {
        private const val PREFS = "engagement"
        private const val K_STREAK = "streak"
        private const val K_BEST = "best_streak"
        private const val K_LAST_GOAL = "last_goal_day"
        private const val K_XP_DAY = "xp_day"
        private const val K_XP = "today_xp"
        private const val K_GOAL = "daily_goal"
        private const val K_FREEZES = "freezes"
        private const val K_GOAL_DAYS = "goal_days"
        private const val K_FROZEN_DAYS = "frozen_days"
        private const val K_REMIND_ON = "reminder_on"
        private const val K_REMIND_HOUR = "reminder_hour"
        private const val K_DUE_HISTOGRAM = "due_histogram"
        private const val K_LEARNED = "words_learned"
        private const val K_MEMORIZED = "words_memorized"
        private const val K_WORD_OF_DAY = "word_of_day"
        private const val K_PATH_DAY = "path_day"
        private const val K_PATH_DONE = "path_done"
        private const val K_GRAMMAR_STARS = "grammar_stars"
        private const val K_HELPER_ARABIC = "helper_arabic"

        @Volatile private var instance: EngagementStore? = null

        fun get(context: Context): EngagementStore =
            instance ?: synchronized(this) { instance ?: EngagementStore(context).also { instance = it } }

        fun today(): Long = LocalDate.now().toEpochDay()

        fun prefs(context: Context): SharedPreferences =
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun readStreak(prefs: SharedPreferences) = StreakState(
            streak = prefs.getInt(K_STREAK, 0),
            bestStreak = prefs.getInt(K_BEST, 0),
            lastGoalDay = prefs.getLong(K_LAST_GOAL, -1),
            xpDay = prefs.getLong(K_XP_DAY, -1),
            todayXp = prefs.getInt(K_XP, 0),
            dailyGoal = prefs.getInt(K_GOAL, StreakState.DEFAULT_GOAL),
            freezes = prefs.getInt(K_FREEZES, 1),
            goalDays = prefs.getString(K_GOAL_DAYS, "").orEmpty().toDays(),
            frozenDays = prefs.getString(K_FROZEN_DAYS, "").orEmpty().toDays()
        )

        private fun writeStreak(prefs: SharedPreferences, s: StreakState) {
            prefs.edit()
                .putInt(K_STREAK, s.streak)
                .putInt(K_BEST, s.bestStreak)
                .putLong(K_LAST_GOAL, s.lastGoalDay)
                .putLong(K_XP_DAY, s.xpDay)
                .putInt(K_XP, s.todayXp)
                .putInt(K_GOAL, s.dailyGoal)
                .putInt(K_FREEZES, s.freezes)
                .putString(K_GOAL_DAYS, s.goalDays.joinToString(","))
                .putString(K_FROZEN_DAYS, s.frozenDays.joinToString(","))
                .apply()
        }

        fun reminderSettings(prefs: SharedPreferences) =
            ReminderSettings(prefs.getBoolean(K_REMIND_ON, false), prefs.getInt(K_REMIND_HOUR, 19))

        /** Words due for review on [day], from the histogram the app saves. */
        fun dueCount(prefs: SharedPreferences, day: Long): Int =
            prefs.getString(K_DUE_HISTOGRAM, "").orEmpty().split(',').sumOf { entry ->
                val (d, n) = entry.split(':').let { if (it.size == 2) it else return@sumOf 0 }
                if ((d.toLongOrNull() ?: Long.MAX_VALUE) <= day) n.toIntOrNull() ?: 0 else 0
            }

        fun helperArabic(prefs: SharedPreferences) = prefs.getBoolean(K_HELPER_ARABIC, true)

        fun learnedCount(prefs: SharedPreferences) = prefs.getInt(K_LEARNED, 0)

        fun wordOfDay(prefs: SharedPreferences, day: Long): Pair<String, String>? =
            prefs.getString(K_WORD_OF_DAY, "").orEmpty().lineSequence()
                .map { it.split('\t') }
                .firstOrNull { it.size == 3 && it[0].toLongOrNull() == day }
                ?.let { it[1] to it[2] }

        fun dueHistogram(cards: Collection<WordCard>): String =
            cards.filter { it.introduced }.groupingBy { it.dueDay }.eachCount()
                .entries.joinToString(",") { "${it.key}:${it.value}" }

        private fun String.toDays(): Set<Long> = split(',').mapNotNull { it.toLongOrNull() }.toSet()
    }
}
