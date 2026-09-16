package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.UserSettingsDao
import com.example.data.local.dao.WaterLogDao
import com.example.data.local.entity.UserSettingsEntity
import com.example.data.local.entity.WaterLogEntity

@Database(
    entities = [WaterLogEntity::class, UserSettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun waterLogDao(): WaterLogDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "water_tracker_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
