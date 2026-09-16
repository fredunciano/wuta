package com.example.ui.util

import android.content.Context
import android.content.Intent
import com.example.data.model.HydrationSummary

object ShareUtil {
    fun generateShareMessage(summary: HydrationSummary): String {
        val pct = if (summary.dailyTargetMl > 0) {
            ((summary.todayTotalMl.toFloat() / summary.dailyTargetMl.toFloat()) * 100).toInt()
        } else 0

        val progressBarLength = 10
        val filledBlocks = ((pct / 100f) * progressBarLength).toInt().coerceIn(0, progressBarLength)
        val emptyBlocks = progressBarLength - filledBlocks
        val visualBar = "█".repeat(filledBlocks) + "░".repeat(emptyBlocks)

        val milestoneEmoji = if (pct >= 100) "🏆" else "💧"

        return buildString {
            append("$milestoneEmoji Daily Hydration Check-in $milestoneEmoji\n\n")
            append("Intake: ${summary.todayTotalMl} / ${summary.dailyTargetMl} ml ($pct%)\n")
            append("Progress: [$visualBar]\n")
            if (summary.currentStreakDays > 0) {
                append("🔥 Streak: ${summary.currentStreakDays} day${if (summary.currentStreakDays > 1) "s" else ""}\n")
            }
            append("⚡ Effective Hydration: ${summary.todayEffectiveMl} ml\n\n")
            append("Staying healthy and energized with Water Tracker! 🌊\n")
            append("#HydrationGoal #Health #WaterTracker")
        }
    }

    fun shareHydrationProgress(context: Context, summary: HydrationSummary) {
        val message = generateShareMessage(summary)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My Daily Hydration Progress 💧")
            putExtra(Intent.EXTRA_TEXT, message)
        }
        val chooser = Intent.createChooser(intent, "Share Hydration Accountability")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
