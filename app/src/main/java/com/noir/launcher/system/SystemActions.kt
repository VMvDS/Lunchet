package com.noir.launcher.system

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.noir.launcher.data.SettingsStore
import com.noir.launcher.model.IslandEvent
import com.noir.launcher.model.NoirBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

object SystemActions {
    /** Prefers the accessibility global action; falls back to the greylisted StatusBarManager call. */
    fun expandNotifications(ctx: Context) {
        if (LockAccessibilityService.action(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)) return
        try {
            val sb = ctx.getSystemService("statusbar") ?: return
            sb.javaClass.getMethod("expandNotificationsPanel").invoke(sb)
        } catch (e: Throwable) {
            Log.w("Noir", "expandNotificationsPanel unavailable", e)
        }
    }

    fun uninstall(ctx: Context, pkg: String) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) { }
    }

    private fun launchIntent(ctx: Context, i: Intent) {
        try {
            ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Log.w("Noir", "cannot open $i", e)
        }
    }

    fun openAccessibilitySettings(ctx: Context) = launchIntent(ctx, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    fun openDefaultHomeSettings(ctx: Context) = launchIntent(ctx, Intent(Settings.ACTION_HOME_SETTINGS))
    fun openNotificationAccess(ctx: Context) = launchIntent(ctx, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    fun openPolicyAccess(ctx: Context) = launchIntent(ctx, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    fun openWriteSettings(ctx: Context) =
        launchIntent(ctx, Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}")))

    fun chooseWallpaper(ctx: Context) {
        val i = Intent(Intent.ACTION_SET_WALLPAPER)
        launchIntent(ctx, Intent.createChooser(i, null))
    }
}

/**
 * Noir system service. Always did the double-tap lock; now also exposes global actions
 * (shade, recents, screenshot, power menu) and hosts the Dynamic-Island style overlay.
 * The class name is unchanged so an already-enabled service stays enabled after the update.
 */
class LockAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var island: IslandOverlay? = null

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pct = batteryPct()
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED ->
                    NoirBus.events.tryEmit(IslandEvent("charge", "Charging", "$pct%"))
                Intent.ACTION_POWER_DISCONNECTED ->
                    NoirBus.events.tryEmit(IslandEvent("unplug", "Unplugged", "$pct%"))
            }
        }
    }

    private fun batteryPct(): Int {
        val i = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        return if (level < 0 || scale <= 0) 0 else level * 100 / scale
    }

    override fun onServiceConnected() {
        instance = this
        val overlay = IslandOverlay(this)
        island = overlay
        val store = SettingsStore(this)
        scope.launch { store.settings.collect { overlay.applySettings(it) } }
        scope.launch { NoirBus.events.collect { overlay.onEvent(it) } }
        scope.launch { NoirBus.media.collect { overlay.onMedia(it) } }
        scope.launch { NoirBus.islandPreview.collect { overlay.setPreview(it) } }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        ContextCompat.registerReceiver(this, powerReceiver, f, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        try { unregisterReceiver(powerReceiver) } catch (e: Exception) { }
        island?.destroy()
        island = null
        scope.cancel()
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile private var instance: LockAccessibilityService? = null
        val isRunning: Boolean get() = instance != null

        fun action(a: Int): Boolean = instance?.performGlobalAction(a) == true

        fun lock(): Boolean =
            Build.VERSION.SDK_INT >= 28 && action(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)

        fun screenshot(): Boolean =
            Build.VERSION.SDK_INT >= 28 && action(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
    }
}
