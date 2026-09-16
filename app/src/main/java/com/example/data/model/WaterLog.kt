package com.example.data.model

data class WaterLog(
    val id: Long = 0,
    val amountMl: Int,
    val effectiveHydrationMl: Int = (amountMl * 1.0f).toInt(),
    val beverageType: BeverageType = BeverageType.WATER,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val isSynced: Boolean = false
)
