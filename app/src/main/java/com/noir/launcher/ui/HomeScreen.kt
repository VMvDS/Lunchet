package com.noir.launcher.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noir.launcher.R
import com.noir.launcher.model.AppInfo
import com.noir.launcher.model.Backdrop
import com.noir.launcher.model.DoubleTapAction
import com.noir.launcher.model.HomeLayout
import com.noir.launcher.model.SwipeDownMode
import com.noir.launcher.system.LockAccessibilityService
import com.noir.launcher.system.SystemActions
import com.noir.launcher.ui.components.AppIcon
import com.noir.launcher.ui.components.pressable
import com.noir.launcher.ui.components.rotateVertically
import com.noir.launcher.ui.theme.LocalAccent
import com.noir.launcher.ui.theme.LocalType
import com.noir.launcher.ui.theme.Noir
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.format.TextStyle as JTextStyle

@Composable
fun HomeScreen(
    vm: LauncherViewModel,
    onOpenDrawer: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenControl: () -> Unit,
    onHomeMenu: () -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
) {
    val context = LocalContext.current
    val t = LocalType.current
    val accent = LocalAccent.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val openDrawer by rememberUpdatedState(onOpenDrawer)
    val openNotifs by rememberUpdatedState(onOpenNotifications)
    val openControl by rememberUpdatedState(onOpenControl)
    val homeMenu by rememberUpdatedState(onHomeMenu)

    val locale = Locale.getDefault()
    val timeFmt = remember(settings.use24h, locale) {
        DateTimeFormatter.ofPattern(if (settings.use24h) "HH:mm" else "h:mm a", locale)
    }
    val hourFmt = remember(settings.use24h, locale) {
        DateTimeFormatter.ofPattern(if (settings.use24h) "HH" else "hh", locale)
    }
    val minFmt = remember(locale) { DateTimeFormatter.ofPattern("mm", locale) }
    val dateFmt = remember(locale) { DateTimeFormatter.ofPattern("EEE, MMM d", locale) }
    val time = status.time
    val hour = time.hour
    val greeting = when {
        hour < 5 -> "Good night"
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
    val who = settings.userName.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""
    val greetingLine = (greeting + who).uppercase(locale)
    val day = time.dayOfWeek.getDisplayName(JTextStyle.FULL, locale)
    val split = (day.length + 1) / 2
    val dayText = buildAnnotatedString {
        withStyle(SpanStyle(color = accent)) { append(day.substring(0, split)) }
        withStyle(SpanStyle(color = accent, drawStyle = Stroke(width = 2f))) { append(day.substring(split)) }
    }
    val wx = weather
    val weatherLine = wx?.let {
        val temp = if (settings.fahrenheit) it.tempC * 9 / 5 + 32 else it.tempC
        "${it.description} · $temp° · hum ${it.humidity}% · wind ${it.windKmh} km/h".uppercase(locale)
    }
    val batteryLine = "BATTERY ${status.batteryPct}% · ${if (status.charging) "CHARGING" else "DISCHARGING"}"
    val dateLine = time.format(dateFmt).uppercase(locale)

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        when (settings.doubleTap) {
                            DoubleTapAction.LOCK -> if (!LockAccessibilityService.lock()) {
                                Toast.makeText(context, R.string.lock_hint, Toast.LENGTH_SHORT).show()
                            }
                            DoubleTapAction.CONTROL -> openControl()
                            DoubleTapAction.NONE -> Unit
                        }
                    },
                    onLongPress = { homeMenu() },
                )
            }
            .pointerInput(Unit) {
                val threshold = 72.dp.toPx()
                var total = 0f
                var startX = 0f
                detectVerticalDragGestures(
                    onDragStart = { startX = it.x; total = 0f },
                    onDragEnd = {
                        if (total < -threshold) openDrawer()
                        else if (total > threshold) {
                            if (settings.swipeDown == SwipeDownMode.SYSTEM) SystemActions.expandNotifications(context)
                            else if (startX > size.width / 2f) openControl()
                            else openNotifs()
                        }
                    },
                    onDragCancel = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                )
            }
    ) {
        if (settings.backdrop == Backdrop.COAST) CoastBackdrop(Modifier.fillMaxSize())

        when (settings.layout) {
            HomeLayout.EDITORIAL -> Row(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(start = 28.dp, end = 18.dp, top = 24.dp, bottom = 28.dp)
            ) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(greetingLine, style = t.micro)
                        Text(time.format(timeFmt), style = t.clock.copy(color = accent, shadow = Shadow(accent.copy(alpha = 0.35f), blurRadius = 28f)))
                        Text(dateLine, style = t.meta)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                        favorites.forEachIndexed { i, app ->
                            Row(
                                Modifier
                                    .padding(start = (i * 5).dp)
                                    .pressable(onClick = { vm.launch(app) }, onLongClick = { onAppLongPress(app) }),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIcon(vm, app, settings.iconStyle, settings.iconSize.dp)
                                Spacer(Modifier.width(18.dp))
                                Text(app.label, style = t.row, maxLines = 1)
                            }
                        }
                        Text("SWIPE UP · ALL APPS", style = t.micro, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                Row(Modifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(dayText, style = t.day, maxLines = 1, modifier = Modifier.rotateVertically())
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.rotateVertically()) {
                        Text(dateLine, style = t.meta, maxLines = 1)
                        if (weatherLine != null) Text(weatherLine, style = t.meta, maxLines = 1)
                        if (settings.showBatteryMeta) Text(batteryLine, style = t.meta, maxLines = 1)
                    }
                }
            }

            HomeLayout.STACK -> Box(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                Column(Modifier.align(Alignment.TopCenter).padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(greetingLine, style = t.micro)
                    Text(time.format(hourFmt), style = t.bigClock)
                    Text(
                        buildAnnotatedString { withStyle(SpanStyle(drawStyle = Stroke(width = 3f))) { append(time.format(minFmt)) } },
                        style = t.bigClock.copy(color = accent, shadow = Shadow(accent.copy(alpha = 0.35f), blurRadius = 36f)),
                    )
                    Text(dateLine, style = t.meta, modifier = Modifier.padding(top = 4.dp))
                    if (weatherLine != null) Text(weatherLine, style = t.meta, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                }
                Column(
                    Modifier.align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    favorites.forEach { app ->
                        Row(
                            Modifier.pressable(onClick = { vm.launch(app) }, onLongClick = { onAppLongPress(app) }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(vm, app, settings.iconStyle, settings.iconSize.dp)
                            Spacer(Modifier.width(16.dp))
                            Text(app.label, style = t.row, maxLines = 1)
                        }
                    }
                    Text("SWIPE UP · ALL APPS", style = t.micro, modifier = Modifier.padding(top = 4.dp))
                }
            }

            HomeLayout.MINIMAL -> Box(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Column(Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 16.dp)) {
                    Text(greetingLine, style = t.micro)
                    Text(time.format(timeFmt), style = t.clock.copy(color = accent, shadow = Shadow(accent.copy(alpha = 0.35f), blurRadius = 28f)))
                    Text(dateLine, style = t.meta)
                    if (weatherLine != null) Text(weatherLine, style = t.meta, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
                }
                Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SWIPE UP · ALL APPS", style = t.micro, modifier = Modifier.padding(bottom = 14.dp))
                    val shape = RoundedCornerShape(32.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(Color(0x99060606))
                            .border(1.dp, Noir.Line, shape)
                            .padding(vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        favorites.take(5).forEach { app ->
                            Box(Modifier.pressable(onClick = { vm.launch(app) }, onLongClick = { onAppLongPress(app) })) {
                                AppIcon(vm, app, settings.iconStyle, (settings.iconSize + 8f).dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
