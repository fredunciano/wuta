package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.local.AppDatabase
import com.example.data.model.BeverageType
import com.example.data.repository.WaterRepository
import com.example.data.sync.CloudSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        when (intent.action) {
            ReminderScheduler.ACTION_WATER_REMINDER -> {
                ReminderScheduler.showHydrationNotification(context)

                // Reschedule next reminder based on saved settings
                scope.launch {
                    val db = AppDatabase.getInstance(context)
                    val settings = db.userSettingsDao().getSettingsSync()
                    if (settings != null && settings.isRemindersEnabled) {
                        ReminderScheduler.scheduleReminder(
                            context = context,
                            intervalMinutes = settings.reminderIntervalMinutes,
                            startHour = settings.reminderStartHour,
                            endHour = settings.reminderEndHour,
                            isEnabled = true
                        )
                    }
                    pendingResult.finish()
                }
            }

            ReminderScheduler.ACTION_QUICK_LOG -> {
                val amount = intent.getIntExtra(ReminderScheduler.EXTRA_AMOUNT, 250)
                scope.launch {
                    val db = AppDatabase.getInstance(context)
                    val syncManager = CloudSyncManager(
                        context,
                        db.waterLogDao(),
                        db.userSettingsDao(),
                        scope
                    )
                    val repo = WaterRepository(db.waterLogDao(), db.userSettingsDao(), syncManager)
                    repo.logWater(amount, BeverageType.WATER, "Quick logged via notification")

                    // Dismiss notification
                    val notificationManager =
                        androidx.core.app.NotificationManagerCompat.from(context)
                    notificationManager.cancel(ReminderScheduler.NOTIFICATION_ID)

                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(
                            context,
                            "💧 +${amount}ml water logged!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    pendingResult.finish()
                }
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                scope.launch {
                    val db = AppDatabase.getInstance(context)
                    val settings = db.userSettingsDao().getSettingsSync()
                    if (settings != null && settings.isRemindersEnabled) {
                        ReminderScheduler.scheduleReminder(
                            context = context,
                            intervalMinutes = settings.reminderIntervalMinutes,
                            startHour = settings.reminderStartHour,
                            endHour = settings.reminderEndHour,
                            isEnabled = true
                        )
                    }
                    pendingResult.finish()
                }
            }

            else -> {
                pendingResult.finish()
            }
        }
    }
}
