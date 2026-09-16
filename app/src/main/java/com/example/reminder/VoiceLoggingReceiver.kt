package com.example.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.WaterTrackerApplication
import com.example.data.gemini.GeminiAssistantService
import com.example.data.local.AppDatabase
import com.example.data.model.BeverageType
import com.example.data.repository.WaterRepository
import com.example.data.sync.CloudSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Background BroadcastReceiver for Google Assistant "Hey Google" voice commands
 * and App Actions shortcuts.
 * Logs water intake in the background without requiring the main app UI to open.
 */
class VoiceLoggingReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_VOICE_LOG = "com.aistudio.watertracker.ACTION_VOICE_LOG"
        const val EXTRA_QUERY = "query"
        const val EXTRA_AMOUNT_ML = "amountMl"
        const val EXTRA_BEVERAGE = "beverage"
        const val VOICE_NOTIFICATION_ID = 2002
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                val db = AppDatabase.getInstance(context)
                val syncManager = CloudSyncManager(
                    context,
                    db.waterLogDao(),
                    db.userSettingsDao(),
                    scope
                )
                val repository = WaterRepository(db.waterLogDao(), db.userSettingsDao(), syncManager)
                val geminiService = GeminiAssistantService()

                val query = intent.getStringExtra(EXTRA_QUERY)
                    ?: intent.getStringExtra("foodOrWater.name")
                    ?: intent.getStringExtra("name")
                    ?: intent.getStringExtra("healthMetric.name")
                val directAmount = intent.getIntExtra(EXTRA_AMOUNT_ML, 0)
                val beverageExtra = intent.getStringExtra(EXTRA_BEVERAGE)

                val summary = repository.getHydrationSummary().firstOrNull()
                val currentTotal = summary?.todayTotalMl ?: 0
                val targetMl = summary?.dailyTargetMl ?: 2500
                val streakDays = summary?.currentStreakDays ?: 0

                var amountToLog = directAmount
                var beverageType = BeverageType.WATER
                var replyMessage = ""

                if (amountToLog > 0) {
                    if (!beverageExtra.isNullOrBlank()) {
                        beverageType = try {
                            BeverageType.valueOf(beverageExtra.uppercase())
                        } catch (e: Exception) {
                            BeverageType.WATER
                        }
                    }
                    replyMessage = "Logged +${amountToLog}ml ${beverageType.displayName} via Google Assistant."
                } else if (!query.isNullOrBlank()) {
                    val result = geminiService.processUserSpeechOrText(
                        input = query,
                        currentTotalMl = currentTotal,
                        targetMl = targetMl,
                        streakDays = streakDays
                    )
                    amountToLog = result.amountMl ?: 250
                    beverageType = result.toBeverageType()
                    replyMessage = result.assistantReply
                } else {
                    // Default fallback
                    amountToLog = 250
                    beverageType = BeverageType.WATER
                    replyMessage = "Logged a standard glass of water (250ml)."
                }

                // Log into database
                repository.logWater(
                    amountMl = amountToLog,
                    beverageType = beverageType,
                    note = "Logged via Google Assistant Voice"
                )

                val newTotal = currentTotal + amountToLog
                val percent = if (targetMl > 0) (newTotal * 100 / targetMl) else 0

                // Post a confirmation notification
                showVoiceLogConfirmationNotification(
                    context = context,
                    amountMl = amountToLog,
                    beverageName = beverageType.displayName,
                    newTotalMl = newTotal,
                    targetMl = targetMl,
                    percent = percent,
                    replyText = replyMessage
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "🤖 Hey Google: +${amountToLog}ml ${beverageType.displayName} logged! ($newTotal/$targetMl ml)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Voice log error: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showVoiceLogConfirmationNotification(
        context: Context,
        amountMl: Int,
        beverageName: String,
        newTotalMl: Int,
        targetMl: Int,
        percent: Int,
        replyText: String
    ) {
        ReminderScheduler.createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            VOICE_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setContentTitle("💧 +${amountMl}ml $beverageName Logged!")
            .setContentText("Today: $newTotalMl / $targetMl ml ($percent%) • $replyText")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "🤖 Google Assistant Voice Log:\n\n" +
                            "• Added: +${amountMl}ml $beverageName\n" +
                            "• Today's Total: $newTotalMl / $targetMl ml ($percent%)\n" +
                            "• $replyText"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(VOICE_NOTIFICATION_ID, notification)
    }
}
