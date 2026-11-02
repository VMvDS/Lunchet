package com.noir.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.noir.launcher.model.AccentKind
import com.noir.launcher.model.Backdrop
import com.noir.launcher.model.DoubleTapAction
import com.noir.launcher.model.DrawerStyle
import com.noir.launcher.model.FontKind
import com.noir.launcher.model.HomeLayout
import com.noir.launcher.model.IconStyle
import com.noir.launcher.model.LauncherSettings
import com.noir.launcher.model.SwipeDownMode
import com.noir.launcher.util.FavoritesCodec
import com.noir.launcher.util.IntListCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

internal val Context.noirDataStore by preferencesDataStore(name = "noir_settings")

object Keys {
    const val NAME = "name"
    const val ICON = "icon_style"
    const val LAYOUT = "layout"
    const val ACCENT = "accent"
    const val FONT = "font"
    const val ICON_SIZE = "icon_size"
    const val TEXT_SCALE = "text_scale"
    const val BACKDROP = "backdrop"
    const val DIM = "dim"
    const val H24 = "use_24h"
    const val BATTERY_META = "battery_meta"
    const val HAPTICS = "haptics"
    const val DRAWER = "drawer_style"
    const val WX = "weather_enabled"
    const val COORDS = "weather_coords"
    const val CITY = "weather_city"
    const val FAHRENHEIT = "fahrenheit"
    const val FAVS = "favorites"
    const val HIDDEN = "hidden_apps"
    const val WIDGETS = "widget_ids"
    const val NOTES = "notes"
    const val SWIPE_DOWN = "swipe_down"
    const val DOUBLE_TAP = "double_tap"
    const val ISLAND = "island_enabled"
    const val ISLAND_ALWAYS = "island_always"
    const val ISLAND_MEDIA = "island_media"
    const val ISLAND_CHARGING = "island_charging"
    const val ISLAND_NOTIFS = "island_notifs"
    const val ISLAND_TOP = "island_top"
    const val ISLAND_SIZE = "island_size"
    const val ISLAND_SCALE = "island_scale"
    // v1 key, still read so old installs keep their wallpaper choice
    const val OLD_WALL = "show_wallpaper"
}

private fun Preferences.str(k: String): String? = this[stringPreferencesKey(k)]
private fun Preferences.bool(k: String): Boolean? = this[booleanPreferencesKey(k)]
private fun Preferences.flt(k: String): Float? = this[floatPreferencesKey(k)]

private inline fun <reified E : Enum<E>> enumOf(raw: String?, def: E): E =
    enumValues<E>().firstOrNull { it.name == raw } ?: def

class SettingsStore(private val context: Context) {

    val settings: Flow<LauncherSettings> = context.noirDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            val oldWall = p.bool(Keys.OLD_WALL) ?: false
            val d = LauncherSettings()
            LauncherSettings(
                loaded = true,
                userName = p.str(Keys.NAME) ?: "",
                layout = enumOf(p.str(Keys.LAYOUT), d.layout),
                accent = enumOf(p.str(Keys.ACCENT), d.accent),
                font = enumOf(p.str(Keys.FONT), d.font),
                iconStyle = enumOf(p.str(Keys.ICON), d.iconStyle),
                iconSize = p.flt(Keys.ICON_SIZE) ?: d.iconSize,
                textScale = p.flt(Keys.TEXT_SCALE) ?: d.textScale,
                backdrop = enumOf(p.str(Keys.BACKDROP), if (oldWall) Backdrop.WALLPAPER else Backdrop.COAST),
                dim = p.flt(Keys.DIM) ?: d.dim,
                use24h = p.bool(Keys.H24) ?: d.use24h,
                showBatteryMeta = p.bool(Keys.BATTERY_META) ?: d.showBatteryMeta,
                haptics = p.bool(Keys.HAPTICS) ?: d.haptics,
                drawerStyle = enumOf(p.str(Keys.DRAWER), d.drawerStyle),
                weatherEnabled = p.bool(Keys.WX) ?: false,
                weatherCoords = p.str(Keys.COORDS) ?: "",
                weatherCity = p.str(Keys.CITY) ?: "",
                fahrenheit = p.bool(Keys.FAHRENHEIT) ?: false,
                favorites = p.str(Keys.FAVS)?.let { FavoritesCodec.decode(it) },
                hidden = FavoritesCodec.decode(p.str(Keys.HIDDEN) ?: ""),
                widgetIds = IntListCodec.decode(p.str(Keys.WIDGETS) ?: ""),
                notes = p.str(Keys.NOTES) ?: "",
                swipeDown = enumOf(p.str(Keys.SWIPE_DOWN), d.swipeDown),
                doubleTap = enumOf(p.str(Keys.DOUBLE_TAP), d.doubleTap),
                islandEnabled = p.bool(Keys.ISLAND) ?: d.islandEnabled,
                islandAlways = p.bool(Keys.ISLAND_ALWAYS) ?: d.islandAlways,
                islandMedia = p.bool(Keys.ISLAND_MEDIA) ?: d.islandMedia,
                islandCharging = p.bool(Keys.ISLAND_CHARGING) ?: d.islandCharging,
                islandNotifs = p.bool(Keys.ISLAND_NOTIFS) ?: d.islandNotifs,
                islandTop = p.flt(Keys.ISLAND_TOP) ?: d.islandTop,
                islandSize = p.flt(Keys.ISLAND_SIZE) ?: d.islandSize,
                islandScale = p.flt(Keys.ISLAND_SCALE) ?: d.islandScale,
            )
        }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.noirDataStore.edit { block(it) }
    }

    suspend fun setStr(key: String, v: String) = edit { it[stringPreferencesKey(key)] = v }
    suspend fun setBool(key: String, v: Boolean) = edit { it[booleanPreferencesKey(key)] = v }
    suspend fun setFloat(key: String, v: Float) = edit { it[floatPreferencesKey(key)] = v }
    suspend fun setFavorites(keys: List<String>) = setStr(Keys.FAVS, FavoritesCodec.encode(keys))
    suspend fun setHidden(keys: List<String>) = setStr(Keys.HIDDEN, FavoritesCodec.encode(keys))
    suspend fun setWidgets(ids: List<Int>) = setStr(Keys.WIDGETS, IntListCodec.encode(ids))
    suspend fun setPlace(coords: String, city: String) = edit {
        it[stringPreferencesKey(Keys.COORDS)] = coords
        it[stringPreferencesKey(Keys.CITY)] = city
    }
}
