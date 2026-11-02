package com.noir.launcher.ui

import android.media.AudioManager
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noir.launcher.R
import com.noir.launcher.model.NoirBus
import com.noir.launcher.system.LockAccessibilityService
import com.noir.launcher.system.NoirNotificationService
import com.noir.launcher.system.SystemActions
import com.noir.launcher.system.SystemControls
import com.noir.launcher.ui.components.G
import com.noir.launcher.ui.components.Glyph
import com.noir.launcher.ui.components.MediaCard
import com.noir.launcher.ui.components.NoirCard
import com.noir.launcher.ui.components.NoirSlider
import com.noir.launcher.ui.components.PackageIcon
import com.noir.launcher.ui.components.pressable
import com.noir.launcher.ui.theme.LocalAccent
import com.noir.launcher.ui.theme.LocalType
import com.noir.launcher.ui.theme.Noir
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun Tile(label: String, glyph: G, active: Boolean, onClick: () -> Unit) {
    val t = LocalType.current
    val accent = LocalAccent.current
    Column(Modifier.width(76.dp).pressable(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (active) accent else Noir.Chip)
                .border(1.dp, if (active) accent else Noir.Line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Glyph(glyph, color = if (active) Noir.Black else Noir.Ink, box = 26.dp)
        }
        Text(label, style = t.micro.copy(letterSpacing = 1.sp), modifier = Modifier.padding(top = 8.dp), maxLines = 1)
    }
}

@Composable
private fun SliderLine(glyph: G, fraction: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Glyph(glyph, color = Noir.Grey, box = 20.dp)
        Spacer(Modifier.width(14.dp))
        NoirSlider(fraction, onChange, Modifier.weight(1f))
    }
}

@Composable
fun ControlCenter(onClose: () -> Unit, onOpenSettings: () -> Unit) {
    val ctx = LocalContext.current
    val t = LocalType.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(700); tick++ } }

    val wifi = remember(tick) { SystemControls.wifiOn(ctx) }
    val bt = remember(tick) { SystemControls.bluetoothOn(ctx) }
    val dnd = remember(tick) { SystemControls.dndOn(ctx) }
    val rotate = remember(tick) { SystemControls.autoRotateOn(ctx) }
    val canWrite = remember(tick) { SystemControls.canWriteSettings(ctx) }
    val torch by NoirBus.torch.collectAsStateWithLifecycle()
    val media by NoirBus.media.collectAsStateWithLifecycle()

    var brightness by remember { mutableFloatStateOf(SystemControls.brightness(ctx)) }
    var mediaVol by remember { mutableFloatStateOf(SystemControls.volume(ctx, AudioManager.STREAM_MUSIC)) }
    var ringVol by remember { mutableFloatStateOf(SystemControls.volume(ctx, AudioManager.STREAM_RING)) }

    fun needService() {
        Toast.makeText(ctx, R.string.lock_hint, Toast.LENGTH_SHORT).show()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF4000000))
            .pointerInput(Unit) { detectTapGestures { onClose() } }
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -60.dp.toPx()) onClose() },
                    onDragCancel = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                )
            }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("CONTROL CENTER", style = t.micro)
                Text("DONE", style = t.meta.copy(color = Noir.Ink), modifier = Modifier.pressable(onClose).padding(8.dp))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Tile("WI-FI", G.WIFI, wifi) { SystemControls.openWifi(ctx); onClose() }
                Tile("BLUETOOTH", G.BLUETOOTH, bt) { SystemControls.openBluetooth(ctx); onClose() }
                Tile("AIRPLANE", G.PLANE, false) { SystemControls.openAirplane(ctx); onClose() }
                Tile("TORCH", G.TORCH, torch) { SystemControls.toggleTorch(ctx) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Tile("FOCUS", G.MOON, dnd) {
                    if (SystemControls.dndGranted(ctx)) SystemControls.setDnd(ctx, !dnd)
                    else { SystemActions.openPolicyAccess(ctx); onClose() }
                }
                Tile("ROTATE", G.ROTATE, rotate) {
                    if (canWrite) SystemControls.setAutoRotate(ctx, !rotate)
                    else { SystemActions.openWriteSettings(ctx); onClose() }
                }
                Tile("SCREENSHOT", G.SHOT, false) {
                    if (!LockAccessibilityService.isRunning) needService()
                    else {
                        onClose()
                        scope.launch { delay(550); LockAccessibilityService.screenshot() }
                    }
                }
                Tile("LOCK", G.LOCK, false) {
                    if (!LockAccessibilityService.lock()) needService() else onClose()
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Tile("CAMERA", G.CAMERA, false) { SystemControls.openCamera(ctx); onClose() }
                Tile("CALCULATOR", G.CALC, false) { SystemControls.openCalculator(ctx); onClose() }
                Tile("TIMER", G.TIMER, false) { SystemControls.openTimer(ctx); onClose() }
                Tile("POWER", G.POWER, false) {
                    if (!LockAccessibilityService.action(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)) needService()
                    else onClose()
                }
            }

            NoirCard {
                if (canWrite) {
                    SliderLine(G.SUN, brightness) { brightness = it; SystemControls.setBrightness(ctx, it) }
                } else {
                    Row(
                        Modifier.fillMaxWidth().pressable({ SystemActions.openWriteSettings(ctx); onClose() }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Glyph(G.SUN, color = Noir.Grey, box = 20.dp)
                        Spacer(Modifier.width(14.dp))
                        Text("ALLOW BRIGHTNESS + ROTATE CONTROL", style = t.meta.copy(color = Noir.Ink))
                    }
                }
                Spacer(Modifier.height(16.dp))
                SliderLine(G.VOLUME, mediaVol) { mediaVol = it; SystemControls.setVolume(ctx, AudioManager.STREAM_MUSIC, it) }
                Spacer(Modifier.height(16.dp))
                SliderLine(G.BELL, ringVol) { ringVol = it; SystemControls.setVolume(ctx, AudioManager.STREAM_RING, it) }
            }

            media?.let { MediaCard(it) }

            Text(
                "NOIR SETTINGS",
                style = t.meta.copy(color = Noir.Ink),
                modifier = Modifier.pressable({ onOpenSettings() }).padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
fun NotificationCenter(vm: LauncherViewModel, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val t = LocalType.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val notifs by NoirBus.notifications.collectAsStateWithLifecycle()
    val media by NoirBus.media.collectAsStateWithLifecycle()
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); tick++ } }
    val enabled = remember(tick) { NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName) }
    val now = remember(tick, notifs) { System.currentTimeMillis() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF4000000))
            .pointerInput(Unit) { detectTapGestures { onClose() } }
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -60.dp.toPx()) onClose() },
                    onDragCancel = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                )
            }
    ) {
        LazyColumn(
            Modifier.fillMaxSize().systemBarsPadding().pointerInput(Unit) { detectTapGestures { } },
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Notifications", style = t.title)
                        Text("${notifs.size} NEW", style = t.micro)
                    }
                    Row {
                        if (notifs.isNotEmpty()) {
                            Text("CLEAR ALL", style = t.meta.copy(color = Noir.Ink), modifier = Modifier.pressable({ NoirNotificationService.dismissAll() }).padding(8.dp))
                        }
                        Text("DONE", style = t.meta.copy(color = Noir.Ink), modifier = Modifier.pressable(onClose).padding(8.dp))
                    }
                }
            }
            if (!enabled) {
                item {
                    NoirCard(Modifier.pressable({ SystemActions.openNotificationAccess(ctx); onClose() })) {
                        Text("Show notifications here", style = t.small)
                        Spacer(Modifier.height(4.dp))
                        Text("TAP TO ALLOW NOTIFICATION ACCESS FOR NOIR", style = t.micro)
                    }
                }
            }
            media?.let { m -> item { MediaCard(m) } }
            if (enabled && notifs.isEmpty()) {
                item { Text("NOTHING NEW", style = t.micro, modifier = Modifier.padding(top = 20.dp)) }
            }
            items(notifs, key = { it.key }) { n ->
                val shape = RoundedCornerShape(22.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(Noir.Card)
                        .border(1.dp, Noir.Hairline, shape)
                        .pressable({
                            try { n.intent?.send() } catch (e: Exception) { }
                            if (n.clearable) NoirNotificationService.dismiss(n.key)
                            onClose()
                        })
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    PackageIcon(vm, n.pkg, settings.iconStyle, 28.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        val ago = DateUtils.getRelativeTimeSpanString(n.time, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
                        Text("${n.appName.uppercase()} · ${ago.uppercase()}", style = t.micro, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (n.title.isNotBlank()) Text(n.title, style = t.small, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        if (n.text.isNotBlank()) Text(n.text, style = t.meta.copy(letterSpacing = 0.3.sp), maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    }
                    if (n.clearable) {
                        Glyph(
                            G.CLOSE,
                            Modifier.pressable({ NoirNotificationService.dismiss(n.key) }).padding(6.dp),
                            color = Noir.Grey,
                            box = 16.dp,
                        )
                    }
                }
            }
        }
    }
}
