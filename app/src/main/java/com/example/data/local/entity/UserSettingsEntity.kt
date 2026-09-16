package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1, // Single row settings
    val dailyTargetMl: Int = 2500,
    val reminderIntervalMinutes: Int = 90, // e.g. every 90 minutes
    val reminderStartHour: Int = 8, // 8:00 AM
    val reminderEndHour: Int = 22, // 10:00 PM
    val isRemindersEnabled: Boolean = true,
    val darkModeSetting: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK", "OLED"
    val encryptedProfileInfo: String = "", // Encrypted weight, activity level, notes
    val lastSyncTimestamp: Long = 0L,
    val cupPresetsCsv: String = "150,250,330,500,750,1000"
)
