package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_logs")
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountMl: Int,
    val effectiveHydrationMl: Int,
    val beverageType: String,
    val timestamp: Long,
    val encryptedNote: String = "",
    val isSynced: Boolean = false,
    val dateEpochDay: Long // Day identifier (e.g. timestamp / 86400000)
)
