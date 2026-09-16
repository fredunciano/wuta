package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import java.util.Calendar
import kotlin.random.Random

object ReminderScheduler {
    const val CHANNEL_ID = "hydration_reminders_channel"
    const val NOTIFICATION_ID = 1001
    const val ACTION_WATER_REMINDER = "com.aistudio.watertracker.ACTION_WATER_REMINDER"
    const val ACTION_QUICK_LOG = "com.aistudio.watertracker.ACTION_QUICK_LOG"
    const val EXTRA_AMOUNT = "extra_amount"

    private val hydrationTips = listOf(
        "💧 Time for a refreshing glass! Stay hydrated and energized.",
        "🌊 Drinking water boosts your concentration and mood.",
        "🧠 Hydration helps your brain operate at peak performance.",
        "✨ A sip now keeps fatigue away. You're doing great!",
        "⚡ Power up! Consistent hydration maintains physical endurance.",
        "🌿 Pure hydration for a healthy glow. Drink a cup now!",
        "🎯 Every glass brings you closer to your daily hydration goal."
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Hydration Reminders"
            val descriptionText = "Smart reminders to drink water and log your daily intake"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(
        context: Context,
        intervalMinutes: Int,
        startHour: Int,
        endHour: Int,
        isEnabled: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = ACTION_WATER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!isEnabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)

        // Check if currently inside active window
        if (currentHour < startHour) {
            cal.set(Calendar.HOUR_OF_DAY, startHour)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
        } else if (currentHour >= endHour) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, startHour)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
        } else {
            cal.add(Calendar.MINUTE, intervalMinutes)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent
            )
        }
    }

    fun showHydrationNotification(context: Context, customMessage: String? = null) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action to Quick Log 250ml directly from notification!
        val quickLogIntent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = ACTION_QUICK_LOG
            putExtra(EXTRA_AMOUNT, 250)
        }
        val quickLogPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            quickLogIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tip = customMessage ?: hydrationTips[Random.nextInt(hydrationTips.size)]

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setContentTitle("💧 Time to Hydrate!")
            .setContentText(tip)
            .setStyle(NotificationCompat.BigTextStyle().bigText(tip))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_input_add,
                "+250ml Logged",
                quickLogPendingIntent
            )

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Notifications permission denied
        }
    }
}
