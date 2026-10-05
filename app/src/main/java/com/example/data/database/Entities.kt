package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val currentXp: Int = 0,
    val xpToNextLevel: Int = 300,
    val starCredits: Int = 100,
    val shipTier: Int = 1,
    val completedTabletsCount: Int = 0,
    val totalWordsMastered: Int = 0,
    val soundEnabled: Boolean = true,
    val speechSpeed: Float = 0.95f
)

@Entity(tableName = "tablet_level_progress")
data class TabletProgressEntity(
    @PrimaryKey val tabletId: String,
    val tabletTitle: String,
    val isCompleted: Boolean = false,
    val timesCompleted: Int = 0,
    val bestScore: Int = 0,
    val questionsAnsweredCorrectly: Int = 0,
    val totalQuestions: Int = 3,
    val lastCompletedTimestamp: Long = 0L
)

@Entity(tableName = "arcade_scores")
data class ArcadeScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameMode: String, // TRANSLATION, SYNONYM, ANTONYM, CLOZE, REACTOR
    val score: Int,
    val wordsBlasted: Int,
    val maxComboStreak: Int,
    val rankGrade: String, // S+, S, A, B, C
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "arcade_mode_stats")
data class ArcadeModeStatsEntity(
    @PrimaryKey val gameMode: String,
    val totalGamesPlayed: Int = 0,
    val totalWordsBlasted: Int = 0,
    val highScore: Int = 0,
    val bestComboStreak: Int = 0
)

@Entity(tableName = "word_mastery")
data class WordMasteryEntity(
    @PrimaryKey val spanishWord: String,
    val englishWord: String,
    val category: String,
    val timesEncountered: Int = 1,
    val timesCorrect: Int = 1,
    val masteryLevel: Int = 1,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "milestone_goals")
data class MilestoneGoalEntity(
    @PrimaryKey val goalId: String,
    val title: String,
    val description: String,
    val currentProgress: Int = 0,
    val targetValue: Int = 1,
    val isUnlocked: Boolean = false,
    val rewardCredits: Int = 50,
    val unlockedTimestamp: Long = 0L
)
