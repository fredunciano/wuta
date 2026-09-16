package com.example.data.repository

import com.example.data.crypto.CryptoManager
import com.example.data.local.dao.UserSettingsDao
import com.example.data.local.dao.WaterLogDao
import com.example.data.local.entity.UserSettingsEntity
import com.example.data.local.entity.WaterLogEntity
import com.example.data.model.BeverageBreakdown
import com.example.data.model.BeverageType
import com.example.data.model.DailyProgress
import com.example.data.model.HourlyIntake
import com.example.data.model.HydrationSummary
import com.example.data.model.WaterLog
import com.example.data.sync.CloudSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WaterRepository(
    private val waterLogDao: WaterLogDao,
    private val userSettingsDao: UserSettingsDao,
    val syncManager: CloudSyncManager
) {
    private val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    fun getTodayEpochDay(): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / 86400000L
    }

    val userSettings: Flow<UserSettingsEntity> = userSettingsDao.getSettings().map { settings ->
        settings ?: UserSettingsEntity().also { defaultSettings ->
            userSettingsDao.insertOrUpdate(defaultSettings)
        }
    }

    fun getTodayLogs(): Flow<List<WaterLog>> {
        val todayEpochDay = getTodayEpochDay()
        return waterLogDao.getLogsForDay(todayEpochDay).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getAllLogs(): Flow<List<WaterLog>> {
        return waterLogDao.getAllLogs().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getHydrationSummary(): Flow<HydrationSummary> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val todayStartMillis = cal.timeInMillis
        val todayEndMillis = todayStartMillis + 86400000L - 1L

        // 7 days ago start
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val sevenDaysAgoStartMillis = cal.timeInMillis

        val sevenDaysLogsFlow = waterLogDao.getLogsBetween(sevenDaysAgoStartMillis, todayEndMillis)

        return combine(sevenDaysLogsFlow, userSettings) { logsList, settings ->
            val target = settings.dailyTargetMl

            // Today's logs
            val todayLogs = logsList.filter { it.timestamp in todayStartMillis..todayEndMillis }
            val todayTotal = todayLogs.sumOf { it.amountMl }
            val todayEffective = todayLogs.sumOf { it.effectiveHydrationMl }

            // Hourly Intakes (0 to 23)
            val hourlyMap = mutableMapOf<Int, Int>()
            for (h in 0..23) hourlyMap[h] = 0
            todayLogs.forEach { log ->
                val logCal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                val hour = logCal.get(Calendar.HOUR_OF_DAY)
                hourlyMap[hour] = (hourlyMap[hour] ?: 0) + log.amountMl
            }
            val hourlyIntakes = (6..23).map { hour -> // Display waking hours 6 AM to 11 PM
                HourlyIntake(hour = hour, amountMl = hourlyMap[hour] ?: 0)
            }

            // 7-day progress history
            val weeklyHistory = mutableListOf<DailyProgress>()
            val dayCal = Calendar.getInstance()
            for (i in 6 downTo 0) {
                dayCal.timeInMillis = todayStartMillis
                dayCal.add(Calendar.DAY_OF_YEAR, -i)
                val dayStart = dayCal.timeInMillis
                val dayEnd = dayStart + 86400000L - 1L
                val epochDay = dayStart / 86400000L

                val dayLogs = logsList.filter { it.timestamp in dayStart..dayEnd }
                val dayTotal = dayLogs.sumOf { it.amountMl }
                val pct = if (target > 0) (dayTotal.toFloat() / target.toFloat()).coerceIn(0f, 1.5f) else 0f
                val dayLabel = if (i == 0) "Today" else dayFormat.format(Date(dayStart))

                weeklyHistory.add(
                    DailyProgress(
                        dateEpochDay = epochDay,
                        dayName = dayLabel,
                        totalMl = dayTotal,
                        targetMl = target,
                        percentage = pct,
                        isGoalMet = dayTotal >= target && target > 0
                    )
                )
            }

            // Streak calculation across the weekly history
            var currentStreak = 0
            var bestStreak = 0
            var tempStreak = 0

            // If today is met or in progress
            for (progress in weeklyHistory) {
                if (progress.isGoalMet) {
                    tempStreak++
                    if (tempStreak > bestStreak) bestStreak = tempStreak
                } else {
                    tempStreak = 0
                }
            }
            currentStreak = tempStreak

            // Beverage breakdown for today
            val totalLogged = todayLogs.sumOf { it.amountMl }
            val beverageMap = todayLogs.groupBy { BeverageType.fromName(it.beverageType) }
            val beverageBreakdowns = BeverageType.entries.mapNotNull { type ->
                val typeLogs = beverageMap[type] ?: emptyList()
                val sum = typeLogs.sumOf { it.amountMl }
                if (sum > 0) {
                    val pct = if (totalLogged > 0) sum.toFloat() / totalLogged.toFloat() else 0f
                    BeverageBreakdown(type = type, totalMl = sum, percentageOfTotal = pct)
                } else null
            }

            val avg7Days = if (weeklyHistory.isNotEmpty()) {
                weeklyHistory.map { it.totalMl }.average().toInt()
            } else 0

            HydrationSummary(
                todayTotalMl = todayTotal,
                todayEffectiveMl = todayEffective,
                dailyTargetMl = target,
                currentStreakDays = currentStreak,
                bestStreakDays = bestStreak.coerceAtLeast(currentStreak),
                averageIntakeLast7Days = avg7Days,
                hourlyIntakes = hourlyIntakes,
                weeklyHistory = weeklyHistory,
                beverageBreakdowns = beverageBreakdowns
            )
        }
    }

    suspend fun logWater(
        amountMl: Int,
        beverageType: BeverageType = BeverageType.WATER,
        note: String = "",
        customTimestamp: Long = System.currentTimeMillis()
    ): Long {
        val effective = (amountMl * beverageType.hydrationFactor).toInt()
        val epochDay = getTodayEpochDay()
        val encryptedNote = if (note.isNotBlank()) CryptoManager.encrypt(note) else ""

        val entity = WaterLogEntity(
            amountMl = amountMl,
            effectiveHydrationMl = effective,
            beverageType = beverageType.name,
            timestamp = customTimestamp,
            encryptedNote = encryptedNote,
            isSynced = false,
            dateEpochDay = epochDay
        )

        val id = waterLogDao.insertLog(entity)
        syncManager.checkPendingOfflineQueue()
        return id
    }

    suspend fun deleteLog(id: Long) {
        waterLogDao.deleteLogById(id)
        syncManager.checkPendingOfflineQueue()
    }

    suspend fun updateSettings(settings: UserSettingsEntity) {
        userSettingsDao.updateSettings(settings)
    }

    private fun WaterLogEntity.toDomainModel(): WaterLog {
        val decryptedNote = if (encryptedNote.isNotBlank()) {
            CryptoManager.decrypt(encryptedNote)
        } else ""

        return WaterLog(
            id = id,
            amountMl = amountMl,
            effectiveHydrationMl = effectiveHydrationMl,
            beverageType = BeverageType.fromName(beverageType),
            timestamp = timestamp,
            note = decryptedNote,
            isSynced = isSynced
        )
    }
}
