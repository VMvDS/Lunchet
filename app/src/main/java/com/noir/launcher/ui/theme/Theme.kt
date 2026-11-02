package com.noir.launcher.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.noir.launcher.model.FontKind
import com.noir.launcher.model.LauncherSettings

object Noir {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF050505)
    val Card = Color(0xFF0B0B0B)
    val Chip = Color(0xFF151515)
    val Ink = Color(0xFFF4F4F2)
    val Grey = Color(0xFF777777)
    val Hairline = Color(0xFF1C1C1C)
    val Line = Color(0xFF2A2A2A)
}

/** Type scale built from the chosen font family and the user's text-size slider. */
class NoirType(kind: FontKind, s: Float) {
    private fun fam(name: String, style: Int): FontFamily = FontFamily(Typeface.create(name, style))

    private val display: FontFamily
    private val light: FontFamily
    private val body: FontFamily

    init {
        when (kind) {
            FontKind.SERIF -> { display = fam("serif", Typeface.BOLD); light = fam("serif", Typeface.NORMAL); body = fam("serif", Typeface.NORMAL) }
            FontKind.MONO -> { display = fam("monospace", Typeface.BOLD); light = fam("monospace", Typeface.NORMAL); body = fam("monospace", Typeface.NORMAL) }
            FontKind.CONDENSED -> { display = fam("sans-serif-condensed", Typeface.BOLD); light = fam("sans-serif-condensed", Typeface.NORMAL); body = fam("sans-serif-condensed", Typeface.NORMAL) }
            FontKind.SYSTEM -> { display = fam("sans-serif-condensed", Typeface.BOLD); light = fam("sans-serif-light", Typeface.NORMAL); body = fam("sans-serif", Typeface.NORMAL) }
        }
    }

    val micro = TextStyle(fontFamily = body, fontSize = (9f * s).sp, letterSpacing = 2.2.sp, color = Noir.Grey)
    val meta = TextStyle(fontFamily = body, fontSize = (10f * s).sp, letterSpacing = 1.6.sp, color = Noir.Grey)
    val row = TextStyle(fontFamily = light, fontSize = (17f * s).sp, letterSpacing = 0.4.sp, color = Noir.Ink)
    val small = TextStyle(fontFamily = light, fontSize = (13f * s).sp, letterSpacing = 0.3.sp, color = Noir.Ink)
    val clock = TextStyle(fontFamily = light, fontSize = (52f * s).sp, letterSpacing = (-1).sp, color = Noir.Ink)
    val bigClock = TextStyle(fontFamily = light, fontSize = (112f * s).sp, letterSpacing = (-4).sp, color = Noir.Ink)
    val day = TextStyle(fontFamily = display, fontSize = (62f * s).sp, letterSpacing = (-1).sp, color = Noir.Ink)
    val drawer = TextStyle(fontFamily = light, fontSize = (21f * s).sp, color = Noir.Ink)
    val title = TextStyle(fontFamily = display, fontSize = (34f * s).sp, color = Noir.Ink)
    val numeral = TextStyle(fontFamily = light, fontSize = (44f * s).sp, letterSpacing = (-1).sp, color = Noir.Ink)
}

val LocalType = staticCompositionLocalOf { NoirType(FontKind.SYSTEM, 1f) }
val LocalAccent = staticCompositionLocalOf { Noir.Ink }
val LocalHaptics = staticCompositionLocalOf { true }

@Composable
fun NoirTheme(settings: LauncherSettings, content: @Composable () -> Unit) {
    val type = remember(settings.font, settings.textScale) { NoirType(settings.font, settings.textScale) }
    val accent = Color(settings.accent.argb)
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Noir.Black, surface = Noir.Surface,
            onBackground = Noir.Ink, onSurface = Noir.Ink,
            primary = Noir.Ink, onPrimary = Noir.Black, secondary = Noir.Grey,
        ),
    ) {
        CompositionLocalProvider(
            LocalType provides type,
            LocalAccent provides accent,
            LocalHaptics provides settings.haptics,
        ) { content() }
    }
}
