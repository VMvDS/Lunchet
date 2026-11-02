package com.noir.launcher.model

import android.media.session.MediaController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/** Process-wide hand-off between the system services and the UI. */
object NoirBus {
    val notifications = MutableStateFlow<List<NotifItem>>(emptyList())
    val media = MutableStateFlow<MediaState?>(null)
    val torch = MutableStateFlow(false)
    val islandPreview = MutableStateFlow(false)
    val events = MutableSharedFlow<IslandEvent>(extraBufferCapacity = 8)

    @Volatile var mediaController: MediaController? = null
}
