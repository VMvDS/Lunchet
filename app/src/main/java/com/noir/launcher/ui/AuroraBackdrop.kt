package com.noir.launcher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Slow, soft light pools on pure black. Low alpha keeps OLED pixels mostly off. */
@Composable
fun AuroraBackdrop(accent: Color, modifier: Modifier = Modifier) {
    val tr = rememberInfiniteTransition(label = "aurora")
    val a by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by tr.animateFloat(1f, 0f, infiniteRepeatable(tween(21000, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Color.Black)
        drawRect(
            Brush.radialGradient(
                listOf(accent.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(w * (0.15f + 0.7f * a), h * (0.14f + 0.12f * b)),
                radius = w * 0.95f,
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(Color(0xFF3B5BFF).copy(alpha = 0.13f), Color.Transparent),
                center = Offset(w * (0.9f - 0.6f * b), h * (0.62f + 0.14f * a)),
                radius = w * 1.05f,
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(Color(0xFFFF4FA3).copy(alpha = 0.08f), Color.Transparent),
                center = Offset(w * (0.3f + 0.3f * a), h * 0.95f),
                radius = w * 0.9f,
            )
        )
    }
}
