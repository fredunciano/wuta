package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.WaterRepository
import com.example.data.sync.CloudSyncManager
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WaterTrackerApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getInstance(this) }
    val syncManager by lazy {
        CloudSyncManager(
            this,
            database.waterLogDao(),
            database.userSettingsDao(),
            applicationScope
        )
    }
    val geminiAssistantService by lazy {
        com.example.data.gemini.GeminiAssistantService()
    }
    val repository by lazy {
        WaterRepository(
            database.waterLogDao(),
            database.userSettingsDao(),
            syncManager
        )
    }

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createNotificationChannel(this)

        applicationScope.launch {
            val settings = database.userSettingsDao().getSettingsSync()
            if (settings != null && settings.isRemindersEnabled) {
                ReminderScheduler.scheduleReminder(
                    context = this@WaterTrackerApplication,
                    intervalMinutes = settings.reminderIntervalMinutes,
                    startHour = settings.reminderStartHour,
                    endHour = settings.reminderEndHour,
                    isEnabled = true
                )
            }
        }
    }
}
