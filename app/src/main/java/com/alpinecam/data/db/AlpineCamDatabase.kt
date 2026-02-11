package com.alpinecam.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SkierGroup::class,
        Skier::class,
        TrainingSession::class,
        SessionSkier::class,
        VideoRecording::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AlpineCamDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun skierDao(): SkierDao
    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun videoRecordingDao(): VideoRecordingDao

    companion object {
        @Volatile
        private var INSTANCE: AlpineCamDatabase? = null

        fun getInstance(context: Context): AlpineCamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlpineCamDatabase::class.java,
                    "alpinecam.db",
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
