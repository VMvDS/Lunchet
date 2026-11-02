package com.noir.launcher.system

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.noir.launcher.model.NoirBus

/** Thin wrappers over the toggles Android lets a normal app control. Everything is failure-tolerant. */
object SystemControls {
    private var torchId: String? = null
    private var torchInit = false

    fun initTorch(ctx: Context) {
        if (torchInit) return
        torchInit = true
        try {
            val cm = ctx.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            torchId = cm.cameraIdList.firstOrNull { id ->
                val c = cm.getCameraCharacteristics(id)
                c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
            cm.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId == torchId) NoirBus.torch.value = enabled
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) { }
    }

    fun toggleTorch(ctx: Context) {
        val id = torchId ?: return
        try {
            val cm = ctx.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cm.setTorchMode(id, !NoirBus.torch.value)
        } catch (e: Exception) { }
    }

    fun wifiOn(ctx: Context): Boolean = try {
        (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
    } catch (e: Exception) { false }

    @SuppressLint("MissingPermission")
    fun bluetoothOn(ctx: Context): Boolean = try {
        ctx.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
    } catch (e: Exception) { false }

    fun dndGranted(ctx: Context): Boolean =
        ctx.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true

    fun dndOn(ctx: Context): Boolean = try {
        val f = ctx.getSystemService(NotificationManager::class.java)?.currentInterruptionFilter
        f != null && f != NotificationManager.INTERRUPTION_FILTER_ALL && f != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    } catch (e: Exception) { false }

    fun setDnd(ctx: Context, on: Boolean) {
        try {
            ctx.getSystemService(NotificationManager::class.java)?.setInterruptionFilter(
                if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL
            )
        } catch (e: Exception) { }
    }

    fun canWriteSettings(ctx: Context): Boolean = Settings.System.canWrite(ctx)

    fun autoRotateOn(ctx: Context): Boolean = try {
        Settings.System.getInt(ctx.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1
    } catch (e: Exception) { true }

    fun setAutoRotate(ctx: Context, on: Boolean) {
        try {
            Settings.System.putInt(ctx.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (on) 1 else 0)
        } catch (e: Exception) { }
    }

    fun brightness(ctx: Context): Float = try {
        Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
    } catch (e: Exception) { 0.5f }

    fun setBrightness(ctx: Context, f: Float) {
        try {
            val v = (f.coerceIn(0f, 1f) * 255f).toInt().coerceIn(4, 255)
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, v)
        } catch (e: Exception) { }
    }

    private fun audio(ctx: Context): AudioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun volume(ctx: Context, stream: Int): Float {
        val a = audio(ctx)
        val max = a.getStreamMaxVolume(stream).coerceAtLeast(1)
        return a.getStreamVolume(stream).toFloat() / max
    }

    fun setVolume(ctx: Context, stream: Int, f: Float) {
        try {
            val a = audio(ctx)
            val max = a.getStreamMaxVolume(stream)
            a.setStreamVolume(stream, (f.coerceIn(0f, 1f) * max).toInt(), 0)
        } catch (e: Exception) { }
    }

    private fun launchIntent(ctx: Context, i: Intent) {
        try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { }
    }

    fun openWifi(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 29) launchIntent(ctx, Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
        else launchIntent(ctx, Intent(Settings.ACTION_WIFI_SETTINGS))
    }
    fun openBluetooth(ctx: Context) = launchIntent(ctx, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    fun openAirplane(ctx: Context) = launchIntent(ctx, Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
    fun openCamera(ctx: Context) = launchIntent(ctx, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
    fun openTimer(ctx: Context) = launchIntent(ctx, Intent(AlarmClock.ACTION_SHOW_TIMERS))
    fun openCalculator(ctx: Context) =
        launchIntent(ctx, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR))

    /** Best last-known coarse fix, or null. Needs ACCESS_COARSE_LOCATION to be granted first. */
    @SuppressLint("MissingPermission")
    fun lastLocation(ctx: Context): Location? {
        if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
        return try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                .mapNotNull { p -> try { lm.getLastKnownLocation(p) } catch (e: Exception) { null } }
                .maxByOrNull { it.time }
        } catch (e: Exception) { null }
    }

    @SuppressLint("MissingPermission")
    fun freshLocation(ctx: Context, onResult: (Location?) -> Unit) {
        if (Build.VERSION.SDK_INT < 30 ||
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) { onResult(null); return }
        try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, ctx.mainExecutor) { loc -> onResult(loc) }
        } catch (e: Exception) { onResult(null) }
    }
}
