package com.example.data.model

data class HourlyIntake(
    val hour: Int, // 0 to 23
    val amountMl: Int
)

data class DailyProgress(
    val dateEpochDay: Long,
    val dayName: String,
    val totalMl: Int,
    val targetMl: Int,
    val percentage: Float,
    val isGoalMet: Boolean
)

data class BeverageBreakdown(
    val type: BeverageType,
    val totalMl: Int,
    val percentageOfTotal: Float
)

data class HydrationSummary(
    val todayTotalMl: Int = 0,
    val todayEffectiveMl: Int = 0,
    val dailyTargetMl: Int = 2500,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val averageIntakeLast7Days: Int = 0,
    val hourlyIntakes: List<HourlyIntake> = emptyList(),
    val weeklyHistory: List<DailyProgress> = emptyList(),
    val beverageBreakdowns: List<BeverageBreakdown> = emptyList()
) {
    val progressPercentage: Float
        get() = if (dailyTargetMl > 0) (todayTotalMl.toFloat() / dailyTargetMl.toFloat()).coerceIn(0f, 2.0f) else 0f

    val isGoalReached: Boolean
        get() = todayTotalMl >= dailyTargetMl && dailyTargetMl > 0
}
