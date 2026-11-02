package com.noir.launcher.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.noir.launcher.ui.theme.Noir
import kotlin.math.sin
import kotlin.random.Random

/** Procedural OLED coastline: near-black land mass, hairline ridge, faint surf strokes. Drawn once, cached. */
@Composable
fun CoastBackdrop(modifier: Modifier = Modifier) {
    Spacer(
        modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val rnd = Random(11)
            val steps = 64
            val edge = List(steps + 1) { i ->
                val t = i / steps.toFloat()
                Offset(w * (0.86f - 0.34f * t + 0.05f * sin(t * 11f)) + (rnd.nextFloat() - 0.5f) * w * 0.02f, h * t)
            }
            val land = Path().apply {
                moveTo(w, 0f)
                edge.forEach { lineTo(it.x, it.y) }
                lineTo(w, h)
                close()
            }
            val ridge = Path().apply {
                moveTo(edge[0].x, edge[0].y)
                edge.drop(1).forEach { lineTo(it.x, it.y) }
            }
            val fill = Brush.horizontalGradient(listOf(Color(0xFF0B0B0B), Color(0xFF151515)), startX = w * 0.5f, endX = w)
            val foam = edge.map { Triple(it, rnd.nextFloat() * w * 0.07f + 4f, rnd.nextFloat() * 0.14f + 0.04f) }
            onDrawBehind {
                drawPath(land, fill)
                foam.forEach { (o, len, a) ->
                    drawLine(Noir.Ink.copy(alpha = a), Offset(o.x, o.y), Offset(o.x - len, o.y + 2f), strokeWidth = 1f)
                }
                drawPath(ridge, Noir.Ink.copy(alpha = 0.28f), style = Stroke(width = 1.5f))
            }
        }
    )
}
