package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyProgress
import com.example.data.model.HourlyIntake
import com.example.data.model.HydrationSummary

@Composable
fun HydrationStatsOverview(
    summary: HydrationSummary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Streak Card
        Surface(
            modifier = Modifier
                .weight(1f)
                .testTag("stats_streak_card"),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF6D00).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak Icon",
                            tint = Color(0xFFFF6D00),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STREAK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${summary.currentStreakDays} Days",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Best: ${summary.bestStreakDays} days",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Weekly Avg Card
        Surface(
            modifier = Modifier
                .weight(1f)
                .testTag("stats_weekly_avg_card"),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Average Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "7-DAY AVG",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${summary.averageIntakeLast7Days} ml",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                val targetMetPct = if (summary.dailyTargetMl > 0) {
                    ((summary.averageIntakeLast7Days.toFloat() / summary.dailyTargetMl.toFloat()) * 100).toInt()
                } else 0
                Text(
                    text = "$targetMetPct% of daily goal",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WeeklyTrendChart(
    weeklyHistory: List<DailyProgress>,
    targetMl: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_trend_chart_card"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weekly Hydration Trend",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Daily intake vs. ${targetMl}ml target",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bars container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val maxScaleMl = (weeklyHistory.maxOfOrNull { it.totalMl } ?: targetMl).coerceAtLeast(targetMl).coerceAtLeast(1)

                weeklyHistory.forEach { dayProgress ->
                    val barHeightRatio = (dayProgress.totalMl.toFloat() / maxScaleMl.toFloat()).coerceIn(0.04f, 1f)
                    val animatedRatio by animateFloatAsState(
                        targetValue = barHeightRatio,
                        animationSpec = tween(durationMillis = 600),
                        label = "BarHeight_${dayProgress.dayName}"
                    )

                    val isTargetReached = dayProgress.isGoalMet

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = "${dayProgress.dayName}: ${dayProgress.totalMl}ml (${if (isTargetReached) "Goal reached" else "Below goal"})"
                            }
                    ) {
                        // Goal reached indicator check
                        if (isTargetReached) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(14.dp))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Vertical Bar
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Target line indicator in background
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(animatedRatio)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        if (isTargetReached) {
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFF4CAF50),
                                                    Color(0xFF75D1FF)
                                                )
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.primaryContainer
                                                )
                                            )
                                        }
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = dayProgress.dayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (dayProgress.dayName == "Today") FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (dayProgress.dayName == "Today") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "${(dayProgress.totalMl / 1000f).let { String.format("%.1f", it) }}L",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyIntakeChart(
    hourlyIntakes: List<HourlyIntake>,
    modifier: Modifier = Modifier
) {
    var selectedHour by remember { mutableStateOf<HourlyIntake?>(null) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hourly_intake_chart_card"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Hourly Distribution",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedHour?.let { "${it.hour}:00 - ${it.amountMl}ml logged" }
                            ?: "Tap any bar to inspect intake by hour",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedHour != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxHourlyMl = (hourlyIntakes.maxOfOrNull { it.amountMl } ?: 250).coerceAtLeast(250)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                hourlyIntakes.forEach { intake ->
                    val ratio = (intake.amountMl.toFloat() / maxHourlyMl.toFloat()).coerceIn(0.06f, 1f)
                    val isSelected = selectedHour?.hour == intake.hour
                    val hasIntake = intake.amountMl > 0

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedHour = if (selectedHour?.hour == intake.hour) null else intake
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(ratio)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.tertiary
                                        else if (hasIntake) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Show every 3 hours label to keep it neat
                        val showLabel = intake.hour % 3 == 0
                        Text(
                            text = if (showLabel) "${intake.hour}" else "·",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BeverageBreakdownSection(
    summary: HydrationSummary,
    modifier: Modifier = Modifier
) {
    if (summary.beverageBreakdowns.isEmpty()) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("beverage_breakdown_card"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Beverage Types Logged Today",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            summary.beverageBreakdowns.forEach { breakdown ->
                val pctInt = (breakdown.percentageOfTotal * 100).toInt()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(breakdown.type.defaultColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = breakdown.type.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${breakdown.totalMl} ml ($pctInt%)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { breakdown.percentageOfTotal },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = breakdown.type.defaultColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}
