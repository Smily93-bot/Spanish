package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProgressEntity::class,
        TabletProgressEntity::class,
        ArcadeScoreEntity::class,
        ArcadeModeStatsEntity::class,
        WordMasteryEntity::class,
        MilestoneGoalEntity::class
    ],
    version = 1,
    // Schema export is off: debug and release KSP tasks wrote the same schema file concurrently and
    // corrupted it. Re-enable with the androidx.room Gradle plugin before adding the first migration.
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    val dao: AppDao get() = appDao()

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spanish_blaster.db"
                )
                    // Add Migration objects here when the schema version increases.
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { instance = it }
            }
    }
}
