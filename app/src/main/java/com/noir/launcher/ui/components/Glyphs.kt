package com.noir.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.noir.launcher.ui.theme.Noir

/** Thin-line icon set, SVG path data on a 24x24 grid, so every glyph matches the line-art theme. */
enum class G(val d: String, val fill: Boolean = false) {
    WIFI("M5 12.55 a11 11 0 0 1 14.08 0 M1.42 9 a16 16 0 0 1 21.16 0 M8.53 16.11 a6 6 0 0 1 6.95 0 M12 20 h0.01"),
    BLUETOOTH("M6.5 6.5 l11 11 L12 23 V1 l5.5 5.5 -11 11"),
    PLANE("M21 16 v-2 l-8 -5 V3.5 a1.5 1.5 0 0 0 -3 0 V9 l-8 5 v2 l8 -2.5 V19 l-2 1.5 V22 l3.5 -1 3.5 1 v-1.5 L13 19 v-5.5 z"),
    TORCH("M9 2 h6 l-1 6 h-4 z M10 8 h4 v13 a1 1 0 0 1 -1 1 h-2 a1 1 0 0 1 -1 -1 z M12 11 v3"),
    MOON("M21 12.79 A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79 z"),
    ROTATE("M23 4 v6 h-6 M20.49 15 a9 9 0 1 1 -2.12 -9.36 L23 10"),
    SHOT("M8 3 H5 a2 2 0 0 0 -2 2 v3 M21 8 V5 a2 2 0 0 0 -2 -2 h-3 M3 16 v3 a2 2 0 0 0 2 2 h3 M16 21 h3 a2 2 0 0 0 2 -2 v-3 M9 9 h6 v6 H9 z"),
    LOCK("M5 11 h14 a2 2 0 0 1 2 2 v7 a2 2 0 0 1 -2 2 H5 a2 2 0 0 1 -2 -2 v-7 a2 2 0 0 1 2 -2 z M7 11 V7 a5 5 0 0 1 10 0 v4"),
    CAMERA("M23 19 a2 2 0 0 1 -2 2 H3 a2 2 0 0 1 -2 -2 V8 a2 2 0 0 1 2 -2 h4 l2 -3 h6 l2 3 h4 a2 2 0 0 1 2 2 z M8 13 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0"),
    CALC("M6 2 h12 a2 2 0 0 1 2 2 v16 a2 2 0 0 1 -2 2 H6 a2 2 0 0 1 -2 -2 V4 a2 2 0 0 1 2 -2 z M8 6 h8 M8 11 h0.01 M12 11 h0.01 M16 11 h0.01 M8 15 h0.01 M12 15 h0.01 M16 15 h0.01 M8 18 h0.01 M12 18 h0.01 M16 18 h0.01"),
    TIMER("M2.5 12 a9.5 9.5 0 1 0 19 0 a9.5 9.5 0 1 0 -19 0 M12 6 v6 l4 2"),
    POWER("M18.36 6.64 a9 9 0 1 1 -12.73 0 M12 2 v10"),
    RECENTS("M7 6 V5 a2 2 0 0 1 2 -2 h8 a2 2 0 0 1 2 2 v10 a2 2 0 0 1 -2 2 h-1 M5 8 h10 a2 2 0 0 1 2 2 v9 a2 2 0 0 1 -2 2 H5 a2 2 0 0 1 -2 -2 v-9 a2 2 0 0 1 2 -2 z"),
    SLIDERS("M4 21 v-7 M4 10 V3 M12 21 v-9 M12 8 V3 M20 21 v-5 M20 12 V3 M1 14 h6 M9 8 h6 M17 16 h6"),
    SUN("M7 12 a5 5 0 1 0 10 0 a5 5 0 1 0 -10 0 M12 1 v2 M12 21 v2 M4.22 4.22 l1.42 1.42 M18.36 18.36 l1.42 1.42 M1 12 h2 M21 12 h2 M4.22 19.78 l1.42 -1.42 M18.36 5.64 l1.42 -1.42"),
    VOLUME("M11 5 L6 9 H2 v6 h4 l5 4 V5 z M19.07 4.93 a10 10 0 0 1 0 14.14 M15.54 8.46 a5 5 0 0 1 0 7.07"),
    BELL("M18 8 A6 6 0 0 0 6 8 c0 7 -3 9 -3 9 h18 s-3 -2 -3 -9 M13.73 21 a2 2 0 0 1 -3.46 0"),
    CLOSE("M18 6 L6 18 M6 6 l12 12"),
    PLUS("M12 5 v14 M5 12 h14"),
    PLAY("M7 4 l13 8 -13 8 z", true),
    PAUSE("M7 4 h3 v16 H7 z M14 4 h3 v16 h-3 z", true),
    PREV("M19 4 L8 12 l11 8 z M4 4 h2.5 v16 H4 z", true),
    NEXT("M5 4 l11 8 -11 8 z M17.5 4 H20 v16 h-2.5 z", true),
}

@Composable
fun Glyph(
    kind: G,
    modifier: Modifier = Modifier,
    color: Color = Noir.Ink,
    box: Dp = 24.dp,
    stroke: Float = 1.5f,
) {
    val path = remember(kind) { PathParser().parsePathString(kind.d).toPath() }
    Canvas(modifier.size(box)) {
        val u = size.minDimension / 24f
        scale(u, Offset.Zero) {
            if (kind.fill) drawPath(path, color)
            else drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
