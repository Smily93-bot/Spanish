package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // User Progress
    @Query("SELECT * FROM user_progress WHERE id = 1 LIMIT 1")
    fun getUserProgress(): Flow<UserProgressEntity?>

    @Query("SELECT * FROM user_progress WHERE id = 1 LIMIT 1")
    suspend fun fetchUserProgress(): UserProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserProgress(progress: UserProgressEntity)

    // Reading Tablets Progress
    @Query("SELECT * FROM tablet_level_progress")
    fun getAllTabletProgress(): Flow<List<TabletProgressEntity>>

    @Query("SELECT * FROM tablet_level_progress WHERE tabletId = :tabletId LIMIT 1")
    suspend fun getTabletProgressById(tabletId: String): TabletProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTabletProgress(tabletProgress: TabletProgressEntity)

    // Vocabulary Arcade High Scores & Runs
    @Query("SELECT * FROM arcade_scores ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentArcadeScores(limit: Int = 15): Flow<List<ArcadeScoreEntity>>

    @Query("SELECT * FROM arcade_scores ORDER BY score DESC, timestamp DESC LIMIT :limit")
    fun getTopArcadeHighScores(limit: Int = 10): Flow<List<ArcadeScoreEntity>>

    @Query("SELECT * FROM arcade_scores WHERE gameMode = :gameMode ORDER BY score DESC, timestamp DESC LIMIT :limit")
    fun getTopArcadeHighScoresForMode(gameMode: String, limit: Int = 10): Flow<List<ArcadeScoreEntity>>

    @Query("SELECT MAX(score) FROM arcade_scores")
    fun getOverallPersonalBestScore(): Flow<Int?>

    @Query("SELECT MAX(score) FROM arcade_scores WHERE gameMode = :gameMode")
    fun getPersonalBestScoreForMode(gameMode: String): Flow<Int?>

    @Query("SELECT * FROM arcade_scores ORDER BY score DESC, timestamp DESC LIMIT :limit")
    suspend fun fetchTopArcadeScores(limit: Int = 10): List<ArcadeScoreEntity>

    @Query("SELECT * FROM arcade_scores WHERE gameMode = :gameMode ORDER BY score DESC, timestamp DESC LIMIT :limit")
    suspend fun fetchTopArcadeScoresForMode(gameMode: String, limit: Int = 10): List<ArcadeScoreEntity>

    @Query("SELECT MAX(score) FROM arcade_scores")
    suspend fun fetchPersonalBestScore(): Int?

    @Query("SELECT MAX(score) FROM arcade_scores WHERE gameMode = :gameMode")
    suspend fun fetchPersonalBestScoreForMode(gameMode: String): Int?

    @Insert
    suspend fun insertArcadeScore(score: ArcadeScoreEntity): Long

    // Arcade Mode Aggregated Stats
    @Query("SELECT * FROM arcade_mode_stats")
    fun getAllArcadeStats(): Flow<List<ArcadeModeStatsEntity>>

    @Query("SELECT * FROM arcade_mode_stats WHERE gameMode = :gameMode LIMIT 1")
    suspend fun getStatsForMode(gameMode: String): ArcadeModeStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateArcadeStats(stats: ArcadeModeStatsEntity)

    // Word Mastery Lexicon
    @Query("SELECT * FROM word_mastery ORDER BY timesCorrect DESC, spanishWord ASC")
    fun getAllWordMastery(): Flow<List<WordMasteryEntity>>

    @Query("SELECT * FROM word_mastery WHERE spanishWord = :word LIMIT 1")
    suspend fun getWordMastery(word: String): WordMasteryEntity?

    @Query("SELECT COUNT(*) FROM word_mastery WHERE masteryLevel >= 3")
    suspend fun countMasteredWords(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWordMastery(wordMastery: WordMasteryEntity)

    // Milestone Goals
    @Query("SELECT * FROM milestone_goals ORDER BY isUnlocked ASC, targetValue ASC")
    fun getAllMilestones(): Flow<List<MilestoneGoalEntity>>

    @Query("SELECT * FROM milestone_goals")
    suspend fun fetchAllMilestones(): List<MilestoneGoalEntity>

    @Query("SELECT * FROM milestone_goals WHERE goalId = :goalId LIMIT 1")
    suspend fun getMilestoneById(goalId: String): MilestoneGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMilestone(goal: MilestoneGoalEntity)
}
