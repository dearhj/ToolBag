package com.android.toolbag.bean

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [StepCount::class, TurnOnTime::class], version = 1)
abstract class StepCountDatabase : RoomDatabase() {
    abstract fun stepCountDao(): StepCountDao

    companion object {
        @Volatile
        private var INSTANCE: StepCountDatabase? = null

        fun getDatabase(context: Context): StepCountDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StepCountDatabase::class.java,
                    "data.db"
                ).allowMainThreadQueries().fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}