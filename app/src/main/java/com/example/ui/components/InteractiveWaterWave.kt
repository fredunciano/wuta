package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BeverageType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class Bubble(
    val id: Int,
    val xRatio: Float,
    val radius: Float,
    val speed: Float,
    var currentYRatio: Float,
    val alpha: Float
)

@Composable
fun InteractiveWaterWave(
    currentMl: Int,
    targetMl: Int,
    beverageType: BeverageType,
    modifier: Modifier = Modifier,
    onContainerClick: () -> Unit = {}
) {
    val fillRatio = if (targetMl > 0) (currentMl.toFloat() / targetMl.toFloat()).coerceIn(0f, 1.15f) else 0f

    // Animated water level
    val animatedFillRatio by animateFloatAsState(
        targetValue = fillRatio,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 120f),
        label = "WaterLevelAnimation"
    )

    // Continuous wave oscillations
    val infiniteTransition = rememberInfiniteTransition(label = "WaveTransition")
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase1"
    )
    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase2"
    )

    // Interactive ripple splash modulation
    var splashBoost by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Persistent rising bubbles
    val bubbles = remember {
        mutableStateListOf<Bubble>().apply {
            repeat(12) { i ->
                add(
                    Bubble(
                        id = i,
                        xRatio = 0.15f + Random.nextFloat() * 0.7f,
                        radius = 2.5f + Random.nextFloat() * 4.5f,
                        speed = 0.003f + Random.nextFloat() * 0.006f,
                        currentYRatio = Random.nextFloat(),
                        alpha = 0.35f + Random.nextFloat() * 0.45f
                    )
                )
            }
        }
    }

    val primaryBeverageColor = beverageType.defaultColor
    val backWaveColor = primaryBeverageColor.copy(alpha = 0.35f)
    val frontWaveColor = primaryBeverageColor.copy(alpha = 0.85f)
    val deepLiquidColor = Color(0xFF004977)

    val percentageInt = ((currentMl.toFloat() / targetMl.coerceAtLeast(1).toFloat()) * 100).toInt()

    Box(
        modifier = modifier
            .testTag("interactive_water_wave_container")
            .semantics {
                contentDescription = "Hydration level: $currentMl of $targetMl ml ($percentageInt%)"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = currentMl.toFloat(),
                    range = 0f..targetMl.toFloat()
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                splashBoost = 15f
                coroutineScope.launch {
                    delay(300)
                    splashBoost = 0f
                }
                onContainerClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val width = size.width
            val height = size.height

            // Container outline: Sleek rounded flask / tumbler with Immersive curve
            val containerCornerRadius = 32.dp.toPx()
            val containerPadding = 4.dp.toPx()
            val containerRect = RoundRect(
                left = containerPadding,
                top = containerPadding,
                right = width - containerPadding,
                bottom = height - containerPadding,
                cornerRadius = CornerRadius(containerCornerRadius, containerCornerRadius)
            )

            val clipContainerPath = Path().apply {
                addRoundRect(containerRect)
            }

            // Draw Subtle dark glass vessel background
            drawRoundRect(
                color = Color(0xFF1A1C1E),
                topLeft = Offset(containerRect.left, containerRect.top),
                size = Size(containerRect.width, containerRect.height),
                cornerRadius = CornerRadius(containerCornerRadius, containerCornerRadius)
            )

            // Draw tick marks on container for scale measurement (500ml, 1000ml, 1500ml, 2000ml, 2500ml)
            val ticks = 5
            for (i in 1..ticks) {
                val tickY = height - containerPadding - (i.toFloat() / (ticks + 0.5f)) * (height - 2 * containerPadding)
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(width - containerPadding - 18.dp.toPx(), tickY),
                    end = Offset(width - containerPadding - 6.dp.toPx(), tickY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Clip liquid to vessel shape
            clipPath(clipContainerPath) {
                val baseWaterY = height - (animatedFillRatio * (height - containerPadding * 2))

                if (animatedFillRatio > 0.01f) {
                    val waveAmplitude = (12.dp.toPx() + splashBoost) * (1f - (animatedFillRatio - 0.5f) * (animatedFillRatio - 0.5f) * 2f).coerceIn(0.2f, 1.2f)

                    // 1. Back Wave
                    val backWavePath = Path().apply {
                        moveTo(0f, height)
                        lineTo(0f, baseWaterY)
                        val step = 8f
                        var x = 0f
                        while (x <= width) {
                            val y = baseWaterY + sin((x / width * 2 * PI + wavePhase2).toDouble()).toFloat() * waveAmplitude * 0.8f
                            lineTo(x, y)
                            x += step
                        }
                        lineTo(width, height)
                        close()
                    }
                    drawPath(
                        path = backWavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(backWaveColor, deepLiquidColor),
                            startY = baseWaterY - waveAmplitude,
                            endY = height
                        )
                    )

                    // 2. Rising bubble particles
                    bubbles.forEach { bubble ->
                        bubble.currentYRatio -= bubble.speed
                        if (bubble.currentYRatio < 0f) {
                            bubble.currentYRatio = 1f
                        }
                        val bubbleY = height - (bubble.currentYRatio * (height - baseWaterY))
                        if (bubbleY > baseWaterY) {
                            val bubbleX = bubble.xRatio * width
                            drawCircle(
                                color = Color.White.copy(alpha = bubble.alpha),
                                radius = bubble.radius,
                                center = Offset(bubbleX, bubbleY)
                            )
                        }
                    }

                    // 3. Front Wave
                    val frontWavePath = Path().apply {
                        moveTo(0f, height)
                        lineTo(0f, baseWaterY)
                        val step = 6f
                        var x = 0f
                        while (x <= width) {
                            val y = baseWaterY + sin((x / width * 2 * PI + wavePhase1).toDouble()).toFloat() * waveAmplitude
                            lineTo(x, y)
                            x += step
                        }
                        lineTo(width, height)
                        close()
                    }
                    drawPath(
                        path = frontWavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(frontWaveColor, deepLiquidColor),
                            startY = baseWaterY - waveAmplitude,
                            endY = height
                        )
                    )

                    // Wave Crest Highlight
                    drawPath(
                        path = frontWavePath,
                        color = Color.White.copy(alpha = 0.45f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
                    )
                }
            }

            // Outer glass border & highlights
            drawRoundRect(
                color = Color(0xFF2E3033),
                topLeft = Offset(containerRect.left, containerRect.top),
                size = Size(containerRect.width, containerRect.height),
                cornerRadius = CornerRadius(containerCornerRadius, containerCornerRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            )

            // Curved glass specular highlight on left
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = Offset(containerPadding + 6.dp.toPx(), containerPadding + 12.dp.toPx()),
                size = Size(5.dp.toPx(), height - containerPadding * 2 - 24.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
        }

        // Inner Centered Content Display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = "$percentageInt%",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Text(
                text = "$currentMl",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 46.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Text(
                text = "of $targetMl ml goal",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            val statusText = when {
                percentageInt >= 100 -> "🎉 Goal Crushed! Fantastic!"
                percentageInt >= 75 -> "🌟 Almost there! Final stretch!"
                percentageInt >= 50 -> "🌊 Halfway hydrated! Keep it up!"
                percentageInt >= 25 -> "💧 Good momentum! Drink up!"
                else -> "🌱 Start your day with fresh water"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (percentageInt >= 100) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
