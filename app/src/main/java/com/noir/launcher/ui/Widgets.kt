package com.noir.launcher.ui

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noir.launcher.data.Keys
import com.noir.launcher.model.NoirBus
import com.noir.launcher.system.WidgetHostManager
import com.noir.launcher.ui.components.NoirCard
import com.noir.launcher.ui.components.pressable
import com.noir.launcher.ui.components.MediaCard
import com.noir.launcher.ui.theme.LocalAccent
import com.noir.launcher.ui.theme.LocalType
import com.noir.launcher.ui.theme.Noir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

@Composable
fun WidgetsScreen(
    vm: LauncherViewModel,
    host: WidgetHostManager,
    onAdd: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val t = LocalType.current
    val accent = LocalAccent.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val media by NoirBus.media.collectAsStateWithLifecycle()
    var edit by remember { mutableStateOf(false) }
    val locale = Locale.getDefault()
    val time = status.time

    LazyColumn(
        Modifier.fillMaxSize().systemBarsPadding(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Widgets", style = t.title)
                    Text("SWIPE LEFT FOR HOME", style = t.micro)
                }
                Row {
                    if (settings.widgetIds.isNotEmpty()) {
                        Text(
                            if (edit) "DONE" else "EDIT",
                            style = t.meta.copy(color = Noir.Ink),
                            modifier = Modifier.pressable({ edit = !edit }).padding(8.dp),
                        )
                    }
                    Text("+ ADD", style = t.meta.copy(color = accent), modifier = Modifier.pressable(onAdd).padding(8.dp))
                }
            }
        }

        item {
            NoirCard {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(time.dayOfMonth.toString(), style = t.numeral.copy(fontSize = 76.sp, color = accent))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.padding(bottom = 10.dp)) {
                        Text(time.month.getDisplayName(JTextStyle.FULL, locale).uppercase(locale), style = t.meta.copy(color = Noir.Ink))
                        Text(time.dayOfWeek.getDisplayName(JTextStyle.FULL, locale), style = t.small)
                    }
                }
            }
        }

        item {
            val w = weather
            if (settings.weatherEnabled && w != null) {
                NoirCard(Modifier.pressable({ onOpenSettings() })) {
                    val temp = if (settings.fahrenheit) w.tempC * 9 / 5 + 32 else w.tempC
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$temp°", style = t.numeral.copy(fontSize = 56.sp))
                        Spacer(Modifier.width(18.dp))
                        Column {
                            Text(w.description, style = t.small)
                            Text("HUM ${w.humidity}% · WIND ${w.windKmh} KM/H", style = t.micro, modifier = Modifier.padding(top = 4.dp))
                            if (settings.weatherCity.isNotBlank()) Text(settings.weatherCity.uppercase(locale), style = t.micro, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            } else {
                NoirCard(Modifier.pressable({ onOpenSettings() })) {
                    Text(if (settings.weatherEnabled) "Loading weather" else "Weather", style = t.small)
                    Text(if (settings.weatherEnabled) "ONE MOMENT" else "TAP TO TURN ON IN SETTINGS · NO TYPING NEEDED", style = t.micro, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        item {
            NoirCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            val sw = 6.dp.toPx()
                            val tl = Offset(sw / 2f, sw / 2f)
                            val sz = Size(size.width - sw, size.height - sw)
                            drawArc(Noir.Line, 0f, 360f, false, tl, sz, style = Stroke(sw))
                            drawArc(accent, -90f, 360f * status.batteryPct / 100f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                        }
                        Text("${status.batteryPct}", style = t.small)
                    }
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text("Battery", style = t.small)
                        Text(if (status.charging) "CHARGING" else "ON BATTERY", style = t.micro, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        item {
            NoirCard {
                Text("PROGRESS", style = t.micro)
                Spacer(Modifier.height(12.dp))
                val dayFrac = (time.hour * 60 + time.minute) / 1440f
                val weekFrac = ((time.dayOfWeek.value - 1) + dayFrac) / 7f
                val monthFrac = ((time.dayOfMonth - 1) + dayFrac) / time.toLocalDate().lengthOfMonth()
                val yearFrac = ((time.dayOfYear - 1) + dayFrac) / time.toLocalDate().lengthOfYear()
                listOf("DAY" to dayFrac, "WEEK" to weekFrac, "MONTH" to monthFrac, "YEAR" to yearFrac).forEach { (label, f) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = t.micro, modifier = Modifier.width(56.dp))
                        Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(50)).background(Noir.Line)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(f.coerceIn(0.01f, 1f)).background(accent))
                        }
                        Text("${(f * 100).toInt()}%", style = t.micro, modifier = Modifier.width(48.dp).padding(start = 10.dp))
                    }
                }
            }
        }

        media?.let { m -> item { MediaCard(m) } }

        items(settings.widgetIds, key = { it }) { id ->
            HostedWidget(host, id, edit) {
                host.delete(id)
                vm.removeWidgetId(id)
            }
        }

        item { NotesCard(vm, settings.notes) }
    }
}

@Composable
private fun NotesCard(vm: LauncherViewModel, saved: String) {
    val t = LocalType.current
    var text by remember(saved) { mutableStateOf(saved) }
    val latest by rememberUpdatedState(text)
    val latestSaved by rememberUpdatedState(saved)
    DisposableEffect(Unit) {
        onDispose { if (latest != latestSaved) vm.setStr(Keys.NOTES, latest) }
    }
    NoirCard {
        Text("QUICK NOTE", style = t.micro)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = t.small,
            cursorBrush = SolidColor(Noir.Ink),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .onFocusChanged { if (!it.isFocused && text != saved) vm.setStr(Keys.NOTES, text) },
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) Text("Write something…", style = t.small.copy(color = Noir.Grey))
                    inner()
                }
            },
        )
    }
}

@Composable
private fun HostedWidget(host: WidgetHostManager, id: Int, edit: Boolean, onRemove: () -> Unit) {
    val density = LocalDensity.current
    val screenW = LocalConfiguration.current.screenWidthDp
    val info = remember(id) { host.info(id) }
    if (info == null) {
        androidx.compose.runtime.LaunchedEffect(id) { onRemove() }
    } else {
        val h = with(density) { info.minHeight.toDp() }.coerceIn(72.dp, 420.dp)
        val shape = RoundedCornerShape(22.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(h)
                .clip(shape)
                .background(Noir.Card)
                .border(1.dp, Noir.Hairline, shape)
        ) {
            AndroidView(
                factory = { c -> host.createView(c, id) ?: View(c) },
                modifier = Modifier.fillMaxSize(),
                update = { v ->
                    if (v is AppWidgetHostView) {
                        val w = screenW - 48
                        val hh = h.value.toInt()
                        v.updateAppWidgetSize(null, w, hh, w, hh)
                    }
                },
            )
            if (edit) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xAA000000))
                        .pointerInput(Unit) { detectTapGestures { onRemove() } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("TAP TO REMOVE", style = LocalType.current.meta.copy(color = Noir.Ink))
                }
            }
        }
    }
}

@Composable
fun WidgetPicker(host: WidgetHostManager, onPick: (AppWidgetProviderInfo) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val t = LocalType.current
    val providers = remember {
        host.providers().sortedBy { it.loadLabel(ctx.packageManager).lowercase() }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Noir.Black)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        LazyColumn(
            Modifier.fillMaxSize().systemBarsPadding(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Add widget", style = t.title)
                        Text("${providers.size} AVAILABLE", style = t.micro)
                    }
                    Text("CLOSE", style = t.meta.copy(color = Noir.Ink), modifier = Modifier.pressable(onClose).padding(8.dp))
                }
            }
            items(providers, key = { it.provider.flattenToString() }) { p ->
                val preview by produceState<ImageBitmap?>(null, p) {
                    value = withContext(Dispatchers.Default) {
                        try {
                            val d = p.loadPreviewImage(ctx, 0) ?: p.loadIcon(ctx, 0)
                            if (d == null) null else {
                                val iw = if (d.intrinsicWidth > 0) d.intrinsicWidth else 400
                                val ih = if (d.intrinsicHeight > 0) d.intrinsicHeight else 200
                                val sc = minOf(1f, 480f / maxOf(iw, ih))
                                d.toBitmap((iw * sc).toInt().coerceAtLeast(1), (ih * sc).toInt().coerceAtLeast(1)).asImageBitmap()
                            }
                        } catch (e: Throwable) {
                            null
                        }
                    }
                }
                NoirCard(Modifier.pressable({ onPick(p) })) {
                    preview?.let {
                        Image(it, null, Modifier.fillMaxWidth().heightIn(max = 150.dp), contentScale = ContentScale.Fit)
                        Spacer(Modifier.height(12.dp))
                    }
                    Text(p.loadLabel(ctx.packageManager), style = t.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(p.provider.packageName.uppercase(), style = t.micro, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
