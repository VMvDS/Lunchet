package com.noir.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import com.noir.launcher.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** App discovery via LauncherApps with live package-change callbacks. */
class AppRepository(context: Context) {
    private val ctx = context.applicationContext
    private val launcherApps = ctx.getSystemService(LauncherApps::class.java)
    private val user = Process.myUserHandle()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var infos: Map<String, LauncherActivityInfo> = emptyMap()
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: android.os.UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: android.os.UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: android.os.UserHandle) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: android.os.UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: android.os.UserHandle, replacing: Boolean) = refresh()
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        refresh()
    }

    fun refresh() {
        scope.launch {
            val list = launcherApps.getActivityList(null, user)
                .filter { it.componentName.packageName != ctx.packageName }
            infos = list.associateBy { it.componentName.flattenToString() }
            _apps.value = list
                .map { AppInfo(it.label.toString(), it.componentName, user) }
                .sortedWith { a, b -> a.label.compareTo(b.label, ignoreCase = true) }
        }
    }

    fun iconDrawable(app: AppInfo): Drawable? = infos[app.key]?.getIcon(0)

    fun launch(app: AppInfo) {
        try {
            launcherApps.startMainActivity(app.component, app.user, null, null)
        } catch (e: Exception) {
            refresh() // stale entry (app just removed or disabled)
        }
    }

    fun showAppInfo(app: AppInfo) {
        try {
            launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
        } catch (e: Exception) { }
    }

    fun close() {
        launcherApps.unregisterCallback(callback)
        scope.cancel()
    }
}
