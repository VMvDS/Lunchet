package com.noir.launcher.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.noir.launcher.model.AppInfo
import com.noir.launcher.model.IconStyle
import com.noir.launcher.model.MediaState
import com.noir.launcher.model.NoirBus
import com.noir.launcher.ui.LauncherViewModel
import com.noir.launcher.ui.theme.LocalAccent
import com.noir.launcher.ui.theme.LocalHaptics
import com.noir.launcher.ui.theme.LocalType
import com.noir.launcher.ui.theme.Noir

/** Lays a composable out rotated 90 degrees counter-clockwise (reads bottom-to-top). */
fun Modifier.rotateVertically(): Modifier = layout { measurable, _ ->
    val p = measurable.measure(Constraints())
    layout(p.height, p.width) {
        p.placeWithLayer(x = (p.height - p.width) / 2, y = (p.width - p.height) / 2) { rotationZ = -90f }
    }
}

/** Tap / long-press with a quiet pressed-state fade instead of a ripple, plus optional haptics. */
@Composable
fun Modifier.pressable(onClick: () -> Unit, onLongClick: (() -> Unit)? = null): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val click by rememberUpdatedState(onClick)
    val long by rememberUpdatedState(onLongClick)
    val hasLong = onLongClick != null
    val haptic = LocalHapticFeedback.current
    val hapticsOn = LocalHaptics.current
    val sc by animateFloatAsState(if (pressed) 0.94f else 1f, label = "press")
    return this
        .graphicsLayer { scaleX = sc; scaleY = sc }
        .alpha(if (pressed) 0.6f else 1f)
        .pointerInput(hasLong, hapticsOn) {
            detectTapGestures(
                onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                onTap = {
                    if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    click()
                },
                onLongPress = if (hasLong) ({
                    if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    long?.invoke()
                    Unit
                }) else null,
            )
        }
}

@Composable
fun AppIcon(vm: LauncherViewModel, app: AppInfo, style: IconStyle, size: Dp) {
    val bmp by produceState<ImageBitmap?>(null, app.key, style) { value = vm.loadIcon(app, style) }
    Box(Modifier.size(size)) {
        bmp?.let {
            Image(bitmap = it, contentDescription = null, modifier = Modifier.size(size), filterQuality = FilterQuality.High)
        }
    }
}

@Composable
fun PackageIcon(vm: LauncherViewModel, pkg: String, style: IconStyle, size: Dp) {
    val bmp by produceState<ImageBitmap?>(null, pkg, style) { value = vm.loadPackageIcon(pkg, style) }
    Box(Modifier.size(size)) {
        bmp?.let {
            Image(bitmap = it, contentDescription = null, modifier = Modifier.size(size), filterQuality = FilterQuality.High)
        }
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Noir.Hairline))
}

@Composable
fun NoirCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Noir.Card)
            .border(1.dp, Noir.Hairline, shape)
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun NoirChips(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val t = LocalType.current
    val accent = LocalAccent.current
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val shape = RoundedCornerShape(50)
            Box(
                Modifier
                    .clip(shape)
                    .background(if (on) accent else Noir.Chip)
                    .border(1.dp, if (on) accent else Noir.Line, shape)
                    .pressable({ onSelect(i) })
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(label, style = t.meta.copy(color = if (on) Noir.Black else Noir.Ink), maxLines = 1)
            }
        }
    }
}

@Composable
fun NoirToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val accent = LocalAccent.current
    val x by animateDpAsState(if (checked) 20.dp else 2.dp, label = "toggle")
    Box(
        Modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(RoundedCornerShape(50))
            .background(if (checked) accent else Noir.Line)
            .pressable({ onChange(!checked) })
    ) {
        Box(
            Modifier
                .offset(x = x, y = 2.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) Noir.Black else Noir.Ink)
        )
    }
}

/** Thin pill slider. onChange fires live while dragging, onDone once on release / tap. */
@Composable
fun NoirSlider(
    fraction: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
    height: Dp = 30.dp,
) {
    val accent = LocalAccent.current
    var w by remember { mutableIntStateOf(1) }
    val cur by rememberUpdatedState(onChange)
    val done by rememberUpdatedState(onDone)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Noir.Chip)
            .onSizeChanged { w = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { cur((it.x / w).coerceIn(0f, 1f)); done() })
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { cur((it.x / w).coerceIn(0f, 1f)) },
                    onDragEnd = { done() },
                    onDragCancel = { done() },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        cur((change.position.x / w).coerceIn(0f, 1f))
                    },
                )
            }
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0.02f, 1f)).clip(RoundedCornerShape(50)).background(accent))
    }
}

@Composable
fun MediaCard(media: MediaState) {
    val t = LocalType.current
    NoirCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val art = remember(media.art) { media.art?.asImageBitmap() }
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Noir.Chip)) {
                if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(media.title, style = t.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(media.artist.uppercase(), style = t.micro, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Glyph(G.PREV, Modifier.pressable({ NoirBus.mediaController?.transportControls?.skipToPrevious() }).padding(12.dp), box = 24.dp)
            Glyph(
                if (media.playing) G.PAUSE else G.PLAY,
                Modifier.pressable({
                    val tc = NoirBus.mediaController?.transportControls
                    if (media.playing) tc?.pause() else tc?.play()
                }).padding(12.dp),
                box = 28.dp,
            )
            Glyph(G.NEXT, Modifier.pressable({ NoirBus.mediaController?.transportControls?.skipToNext() }).padding(12.dp), box = 24.dp)
        }
    }
}
