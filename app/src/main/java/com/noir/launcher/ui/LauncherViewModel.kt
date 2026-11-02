package com.noir.launcher.ui

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noir.launcher.data.AppRepository
import com.noir.launcher.data.SettingsStore
import com.noir.launcher.data.WeatherClient
import com.noir.launcher.model.AppInfo
import com.noir.launcher.model.HomeStatus
import com.noir.launcher.model.IconStyle
import com.noir.launcher.model.LauncherSettings
import com.noir.launcher.model.Place
import com.noir.launcher.model.Weather
import com.noir.launcher.system.SystemControls
import com.noir.launcher.ui.icons.MonoIconProcessor
import com.noir.launcher.util.parseCoords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository(app)
    private val store = SettingsStore(app)

    val apps: StateFlow<List<AppInfo>> = repo.apps
    val settings: StateFlow<LauncherSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherSettings())

    /** Apps minus the ones the user hid. Search in the drawer still reaches hidden apps. */
    val visibleApps: StateFlow<List<AppInfo>> = combine(apps, settings) { list, s ->
        if (s.hidden.isEmpty()) list else list.filter { it.key !in s.hidden }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hiddenApps: StateFlow<List<AppInfo>> = combine(apps, settings) { list, s ->
        list.filter { it.key in s.hidden }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favorites: StateFlow<List<AppInfo>> = combine(apps, settings) { list, s ->
        if (!s.loaded || list.isEmpty()) emptyList()
        else (s.favorites ?: defaultFavoriteKeys(list))
            .mapNotNull { k -> list.firstOrNull { it.key == k } }
            .take(MAX_FAVORITES)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val status: StateFlow<HomeStatus> = flow {
        val bm = app.getSystemService(BatteryManager::class.java)
        while (true) {
            val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
            emit(HomeStatus(LocalDateTime.now(), pct, bm.isCharging))
            delay(60_000L - System.currentTimeMillis() % 60_000L + 50L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeStatus(LocalDateTime.now(), 0, false))

    val weather: StateFlow<Weather?> = settings
        .map { Triple(it.weatherEnabled, it.weatherCoords, it.loaded) }
        .distinctUntilChanged()
        .flatMapLatest { (enabled, coords, _) ->
            val ll = parseCoords(coords)
            if (!enabled || ll == null) flowOf(null)
            else flow {
                var last: Weather? = null
                while (true) {
                    last = WeatherClient.fetch(ll.first, ll.second) ?: last
                    emit(last)
                    delay(30 * 60_000L)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val iconCache = LruCache<String, ImageBitmap>(200)

    init {
        viewModelScope.launch { apps.drop(1).collect { iconCache.evictAll() } }
        SystemControls.initTorch(app)
    }

    suspend fun loadIcon(app: AppInfo, style: IconStyle): ImageBitmap? {
        val key = "${app.key}|${style.name}"
        iconCache.get(key)?.let { return it }
        return withContext(Dispatchers.Default) {
            val d = repo.iconDrawable(app) ?: return@withContext null
            MonoIconProcessor.render(d, style).asImageBitmap().also { iconCache.put(key, it) }
        }
    }

    /** Icon for a package name (used by notifications), in the current icon style. */
    suspend fun loadPackageIcon(pkg: String, style: IconStyle): ImageBitmap? {
        val key = "pkg:$pkg|${style.name}"
        iconCache.get(key)?.let { return it }
        return withContext(Dispatchers.Default) {
            try {
                val d = app.packageManager.getApplicationIcon(pkg)
                MonoIconProcessor.render(d, style).asImageBitmap().also { iconCache.put(key, it) }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun launch(a: AppInfo) = repo.launch(a)
    fun showInfo(a: AppInfo) = repo.showAppInfo(a)
    fun isPinned(a: AppInfo) = favorites.value.any { it.key == a.key }
    fun canPinMore() = favorites.value.size < MAX_FAVORITES

    fun togglePin(a: AppInfo) = viewModelScope.launch {
        val cur = favorites.value.map { it.key }
        val next = when {
            a.key in cur -> cur - a.key
            cur.size < MAX_FAVORITES -> cur + a.key
            else -> cur
        }
        store.setFavorites(next)
    }

    fun isHidden(a: AppInfo) = a.key in settings.value.hidden

    fun toggleHidden(a: AppInfo) = viewModelScope.launch {
        val cur = settings.value.hidden
        store.setHidden(if (a.key in cur) cur - a.key else cur + a.key)
    }

    fun setStr(key: String, v: String) { viewModelScope.launch { store.setStr(key, v) } }
    fun setBool(key: String, v: Boolean) { viewModelScope.launch { store.setBool(key, v) } }
    fun setFloat(key: String, v: Float) { viewModelScope.launch { store.setFloat(key, v) } }

    fun addWidgetId(id: Int) {
        viewModelScope.launch {
            val cur = settings.value.widgetIds
            if (id !in cur) store.setWidgets(cur + id)
        }
    }

    fun removeWidgetId(id: Int) {
        viewModelScope.launch { store.setWidgets(settings.value.widgetIds - id) }
    }

    fun pickPlace(p: Place) {
        viewModelScope.launch {
            store.setPlace("${p.lat}, ${p.lon}", p.name)
            store.setBool(com.noir.launcher.data.Keys.WX, true)
        }
    }

    suspend fun searchPlaces(q: String): List<Place> = WeatherClient.search(q)

    /** Uses the coarse last-known fix; returns false when nothing is available yet. */
    fun useDeviceLocation(onDone: (Boolean) -> Unit) {
        val loc = SystemControls.lastLocation(app)
        if (loc != null) {
            viewModelScope.launch {
                store.setPlace("${loc.latitude}, ${loc.longitude}", "My location")
                store.setBool(com.noir.launcher.data.Keys.WX, true)
                onDone(true)
            }
        } else {
            SystemControls.freshLocation(app) { fresh ->
                if (fresh == null) onDone(false)
                else viewModelScope.launch {
                    store.setPlace("${fresh.latitude}, ${fresh.longitude}", "My location")
                    store.setBool(com.noir.launcher.data.Keys.WX, true)
                    onDone(true)
                }
            }
        }
    }

    private fun defaultFavoriteKeys(list: List<AppInfo>): List<String> {
        val pm = app.packageManager
        fun pkgOf(i: Intent): String? =
            pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
                ?.takeIf { it != "android" }
        val pkgs = listOfNotNull(
            pkgOf(Intent(Intent.ACTION_DIAL)),
            pkgOf(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)),
            pkgOf(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)),
            pkgOf(Intent(MediaStore.ACTION_IMAGE_CAPTURE)),
            pkgOf(Intent(Settings.ACTION_SETTINGS)),
        ).distinct()
        return pkgs.mapNotNull { p -> list.firstOrNull { it.component.packageName == p }?.key }
    }

    override fun onCleared() {
        repo.close()
        super.onCleared()
    }

    companion object { const val MAX_FAVORITES = 7 }
}
