package com.noir.launcher.system

import android.app.Notification
import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.noir.launcher.model.IslandEvent
import com.noir.launcher.model.MediaState
import com.noir.launcher.model.NoirBus
import com.noir.launcher.model.NotifItem

/** Feeds the Notification Center, the media card and the island. Needs "Notification access". */
class NoirNotificationService : NotificationListenerService() {
    private val handler = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null
    private var lastTitle: String = ""
    private var lastPlaying = false

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        bindMedia(list)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publishMedia()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publishMedia()
        override fun onSessionDestroyed() {
            controller = null
            NoirBus.mediaController = null
            NoirBus.media.value = null
        }
    }

    override fun onListenerConnected() {
        instance = this
        refresh()
        try {
            val mgr = getSystemService(MediaSessionManager::class.java)
            val cn = ComponentName(this, NoirNotificationService::class.java)
            mgr.addOnActiveSessionsChangedListener(sessionListener, cn, handler)
            bindMedia(mgr.getActiveSessions(cn))
        } catch (e: Exception) { }
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        try { getSystemService(MediaSessionManager::class.java).removeOnActiveSessionsChangedListener(sessionListener) } catch (e: Exception) { }
        controller?.unregisterCallback(controllerCallback)
        controller = null
        NoirBus.mediaController = null
        NoirBus.media.value = null
        NoirBus.notifications.value = emptyList()
    }

    private fun bindMedia(list: List<MediaController>?) {
        val best = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list?.firstOrNull()
        if (best?.sessionToken == controller?.sessionToken && best != null) { publishMedia(); return }
        controller?.unregisterCallback(controllerCallback)
        controller = best
        NoirBus.mediaController = best
        best?.registerCallback(controllerCallback, handler)
        publishMedia()
    }

    private fun publishMedia() {
        val c = controller
        if (c == null) { NoirBus.media.value = null; return }
        val md = c.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        if (title.isBlank()) { NoirBus.media.value = null; return }
        val artist = (md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)).orEmpty()
        val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
        val raw: Bitmap? = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val art = raw?.let {
            try {
                if (it.width > 320) Bitmap.createScaledBitmap(it, 320, (320f * it.height / it.width).toInt().coerceAtLeast(1), true) else it
            } catch (e: Exception) { null }
        }
        NoirBus.media.value = MediaState(title, artist, playing, c.packageName, art)
        if (playing && (title != lastTitle || !lastPlaying)) {
            NoirBus.events.tryEmit(IslandEvent("media", title, artist, c.packageName))
        }
        lastTitle = title
        lastPlaying = playing
    }

    private fun toItem(sbn: StatusBarNotification): NotifItem? {
        val n = sbn.notification ?: return null
        if (sbn.packageName == packageName) return null
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return null
        if (n.category == Notification.CATEGORY_TRANSPORT) return null
        val ex = n.extras
        val title = ex?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (ex?.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: ex?.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return null
        val app = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (e: Exception) { sbn.packageName }
        return NotifItem(
            key = sbn.key,
            pkg = sbn.packageName,
            appName = app,
            title = title,
            text = text,
            time = sbn.postTime,
            intent = n.contentIntent,
            clearable = sbn.isClearable,
        )
    }

    private fun refresh() {
        val list = try { activeNotifications?.toList() ?: emptyList() } catch (e: Exception) { emptyList() }
        NoirBus.notifications.value = list.mapNotNull { toItem(it) }.sortedByDescending { it.time }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
        val item = sbn?.let { toItem(it) } ?: return
        val label = if (item.title.isNotBlank()) item.title else item.appName
        NoirBus.events.tryEmit(IslandEvent("notif", label, item.text, item.pkg, item.intent))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    companion object {
        @Volatile private var instance: NoirNotificationService? = null
        fun dismiss(key: String) { try { instance?.cancelNotification(key) } catch (e: Exception) { } }
        fun dismissAll() { try { instance?.cancelAllNotifications() } catch (e: Exception) { } }
    }
}
