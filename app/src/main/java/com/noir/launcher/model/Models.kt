package com.noir.launcher.model

import android.app.PendingIntent
import android.content.ComponentName
import android.graphics.Bitmap
import android.os.UserHandle

data class AppInfo(
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
) {
    val key: String get() = component.flattenToString()
}

enum class IconStyle { LINE, GLYPH, ORIGINAL, TILE }
enum class HomeLayout { EDITORIAL, STACK, MINIMAL }
enum class FontKind { SYSTEM, SERIF, MONO, CONDENSED }
enum class AccentKind(val argb: Long) {
    PAPER(0xFFF4F4F2),
    ICE(0xFFBFE6F2),
    AMBER(0xFFFFC46B),
    CRIMSON(0xFFE0334C),
    MINT(0xFF9CF0C6),
    VIOLET(0xFFC4B5FD),
}
enum class Backdrop { COAST, WALLPAPER, PURE, AURORA }
enum class DrawerStyle { LIST, GRID }
enum class SwipeDownMode { NOIR, SYSTEM }
enum class DoubleTapAction { LOCK, CONTROL, NONE }

data class LauncherSettings(
    val loaded: Boolean = false,
    val userName: String = "",
    // look
    val layout: HomeLayout = HomeLayout.EDITORIAL,
    val accent: AccentKind = AccentKind.PAPER,
    val font: FontKind = FontKind.SYSTEM,
    val iconStyle: IconStyle = IconStyle.TILE,
    val iconSize: Float = 30f,
    val textScale: Float = 1f,
    val backdrop: Backdrop = Backdrop.COAST,
    val dim: Float = 0.35f,
    // home
    val use24h: Boolean = true,
    val showBatteryMeta: Boolean = true,
    val haptics: Boolean = true,
    val drawerStyle: DrawerStyle = DrawerStyle.LIST,
    val weatherEnabled: Boolean = false,
    val weatherCoords: String = "",
    val weatherCity: String = "",
    val fahrenheit: Boolean = false,
    /** null = never customised (defaults are derived), empty = user cleared everything. */
    val favorites: List<String>? = null,
    val hidden: List<String> = emptyList(),
    val widgetIds: List<Int> = emptyList(),
    val notes: String = "",
    // gestures
    val swipeDown: SwipeDownMode = SwipeDownMode.NOIR,
    val doubleTap: DoubleTapAction = DoubleTapAction.LOCK,
    // island
    val islandEnabled: Boolean = true,
    val islandAlways: Boolean = false,
    val islandMedia: Boolean = true,
    val islandCharging: Boolean = true,
    val islandNotifs: Boolean = true,
    val islandTop: Float = 10f,
    val islandSize: Float = 32f,
    val islandScale: Float = 1f,
)

data class Weather(val tempC: Int, val humidity: Int, val windKmh: Int, val description: String)

data class Place(val name: String, val region: String, val lat: Double, val lon: Double)

data class HomeStatus(
    val time: java.time.LocalDateTime,
    val batteryPct: Int,
    val charging: Boolean,
)

data class NotifItem(
    val key: String,
    val pkg: String,
    val appName: String,
    val title: String,
    val text: String,
    val time: Long,
    val intent: PendingIntent?,
    val clearable: Boolean,
)

data class MediaState(
    val title: String,
    val artist: String,
    val playing: Boolean,
    val pkg: String,
    val art: Bitmap?,
)

/** kind: "charge", "unplug", "notif", "media". */
data class IslandEvent(
    val kind: String,
    val title: String,
    val sub: String,
    val pkg: String? = null,
    val intent: PendingIntent? = null,
)
