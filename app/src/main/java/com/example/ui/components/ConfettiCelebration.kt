package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class ConfettiParticle(
    val startX: Float,
    val speedY: Float,
    val speedX: Float,
    val color: Color,
    val size: Float,
    val rotationSpeed: Float,
    val initialAngle: Float
)

@Composable
fun ConfettiCelebration(
    trigger: Boolean,
    modifier: Modifier = Modifier,
    onAnimationEnd: () -> Unit = {}
) {
    if (!trigger) return

    val progress = remember { Animatable(0f) }
    val colors = listOf(
        Color(0xFF00B0FF),
        Color(0xFF00E676),
        Color(0xFFFFD600),
        Color(0xFFFF4081),
        Color(0xFF7C4DFF),
        Color(0xFF00E5FF)
    )

    val particles = remember {
        List(60) {
            ConfettiParticle(
                startX = Random.nextFloat(),
                speedY = 0.8f + Random.nextFloat() * 0.6f,
                speedX = (Random.nextFloat() - 0.5f) * 0.4f,
                color = colors[Random.nextInt(colors.size)],
                size = 8f + Random.nextFloat() * 12f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                initialAngle = Random.nextFloat() * 360f
            )
        }
    }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2400, easing = LinearEasing)
        )
        onAnimationEnd()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val currentProgress = progress.value

        particles.forEach { p ->
            val x = (p.startX + p.speedX * currentProgress) * w
            val y = currentProgress * p.speedY * h
            val angle = p.initialAngle + p.rotationSpeed * currentProgress
            val alpha = (1f - currentProgress * 0.9f).coerceIn(0f, 1f)

            rotate(degrees = angle, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - p.size / 2, y - p.size / 2),
                    size = Size(p.size, p.size * 0.6f)
                )
            }
        }
    }
}
