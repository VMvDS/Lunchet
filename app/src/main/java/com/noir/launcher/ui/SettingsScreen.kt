package com.noir.launcher.ui

import android.Manifest
import android.app.WallpaperManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.app.NotificationManagerCompat
import com.noir.launcher.data.Keys
import com.noir.launcher.model.AccentKind
import com.noir.launcher.model.Backdrop
import com.noir.launcher.model.DoubleTapAction
import com.noir.launcher.model.DrawerStyle
import com.noir.launcher.model.FontKind
import com.noir.launcher.model.HomeLayout
import com.noir.launcher.model.IconStyle
import com.noir.launcher.model.NoirBus
import com.noir.launcher.model.Place
import com.noir.launcher.model.SwipeDownMode
import com.noir.launcher.system.LockAccessibilityService
import com.noir.launcher.system.SystemActions
import com.noir.launcher.system.SystemControls
import com.noir.launcher.ui.components.Hairline
import com.noir.launcher.ui.components.NoirChips
import com.noir.launcher.ui.components.NoirSlider
import com.noir.launcher.ui.components.NoirToggle
import com.noir.launcher.ui.components.pressable
import com.noir.launcher.ui.theme.LocalAccent
import com.noir.launcher.ui.theme.LocalType
import com.noir.launcher.ui.theme.Noir
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val TABS = listOf("LOOK", "HOME", "ISLAND", "PANELS", "GESTURES", "SYSTEM")

private fun pretty(s: String) = s.replace('_', ' ')

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    val t = LocalType.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = t.micro)
        content()
        Hairline(Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun ToggleRow(title: String, sub: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    val t = LocalType.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = t.small)
            if (sub != null) Text(sub, style = t.micro, modifier = Modifier.padding(top = 3.dp))
        }
        Spacer(Modifier.width(12.dp))
        NoirToggle(checked, onChange)
    }
}

@Composable
private fun ActionRow(title: String, sub: String? = null, trailing: String? = null, onClick: () -> Unit) {
    val t = LocalType.current
    val accent = LocalAccent.current
    Row(Modifier.fillMaxWidth().pressable(onClick).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = t.small)
            if (sub != null) Text(sub, style = t.micro, modifier = Modifier.padding(top = 3.dp))
        }
        if (trailing != null) Text(trailing, style = t.meta.copy(color = accent))
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    min: Float,
    max: Float,
    fmt: (Float) -> String,
    onDone: (Float) -> Unit,
) {
    val t = LocalType.current
    var v by remember(value) { mutableFloatStateOf(value) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = t.small)
            Text(fmt(v), style = t.meta)
        }
        NoirSlider(
            fraction = (v - min) / (max - min),
            onChange = { f -> v = min + f * (max - min) },
            onDone = { onDone(v) },
        )
    }
}

@Composable
fun SettingsScreen(vm: LauncherViewModel, onClose: () -> Unit) {
    val t = LocalType.current
    val s by vm.settings.collectAsStateWithLifecycle()
    val hidden by vm.hiddenApps.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val canSetWallpaper = remember { WallpaperManager.getInstance(ctx).isSetWallpaperAllowed }

    DisposableEffect(tab) {
        NoirBus.islandPreview.value = tab == 2
        onDispose { NoirBus.islandPreview.value = false }
    }

    val serviceOn = remember(tick) { LockAccessibilityService.isRunning }
    val notifOn = remember(tick) { NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName) }
    val writeOn = remember(tick) { SystemControls.canWriteSettings(ctx) }
    val dndOn = remember(tick) { SystemControls.dndGranted(ctx) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Noir.Black)
            .systemBarsPadding()
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 20.dp, top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Settings", style = t.title)
                    Text("NOIR LAUNCHER 2.0", style = t.micro)
                }
                Text("CLOSE", style = t.meta.copy(color = Noir.Ink), modifier = Modifier.pressable(onClose).padding(8.dp))
            }
            Box(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                NoirChips(TABS, tab) { tab = it }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 8.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                when (tab) {
                    0 -> {
                        item {
                            Group("HOME LAYOUT") {
                                NoirChips(HomeLayout.entries.map { pretty(it.name) }, s.layout.ordinal) { vm.setStr(Keys.LAYOUT, HomeLayout.entries[it].name) }
                            }
                        }
                        item {
                            Group("ACCENT COLOUR") {
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    AccentKind.entries.forEach { a ->
                                        val on = a == s.accent
                                        Box(
                                            Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(a.argb))
                                                .border(if (on) 3.dp else 1.dp, if (on) Noir.Ink else Noir.Line, CircleShape)
                                                .pressable({ vm.setStr(Keys.ACCENT, a.name) })
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Group("FONT") {
                                NoirChips(FontKind.entries.map { pretty(it.name) }, s.font.ordinal) { vm.setStr(Keys.FONT, FontKind.entries[it].name) }
                            }
                        }
                        item {
                            Group("ICONS") {
                                NoirChips(IconStyle.entries.map { it.name }, s.iconStyle.ordinal) { vm.setStr(Keys.ICON, IconStyle.entries[it].name) }
                                SliderRow("Icon size", s.iconSize, 24f, 48f, { "${it.roundToInt()} dp" }) { vm.setFloat(Keys.ICON_SIZE, it) }
                            }
                        }
                        item {
                            Group("TEXT") {
                                SliderRow("Text size", s.textScale, 0.8f, 1.35f, { "${(it * 100).roundToInt()}%" }) { vm.setFloat(Keys.TEXT_SCALE, it) }
                            }
                        }
                        item {
                            Group("BACKGROUND") {
                                NoirChips(listOf("COAST", "WALLPAPER", "PURE BLACK", "AURORA"), s.backdrop.ordinal) { vm.setStr(Keys.BACKDROP, Backdrop.entries[it].name) }
                                if (s.backdrop == Backdrop.WALLPAPER) {
                                    SliderRow("Wallpaper dimming", s.dim, 0f, 0.8f, { "${(it * 100).roundToInt()}%" }) { vm.setFloat(Keys.DIM, it) }
                                    if (canSetWallpaper) ActionRow("Change wallpaper", trailing = "OPEN") { SystemActions.chooseWallpaper(ctx) }
                                }
                            }
                        }
                    }

                    1 -> {
                        item { Group("NAME") { NameField(s.userName) { vm.setStr(Keys.NAME, it.trim()) } } }
                        item {
                            Group("CLOCK") {
                                NoirChips(listOf("24H", "12H"), if (s.use24h) 0 else 1) { vm.setBool(Keys.H24, it == 0) }
                                ToggleRow("Battery line", "Shown in the vertical text", s.showBatteryMeta) { vm.setBool(Keys.BATTERY_META, it) }
                            }
                        }
                        item { WeatherGroup(vm, s.weatherEnabled, s.weatherCity, s.fahrenheit) }
                        item {
                            Group("APP DRAWER") {
                                NoirChips(DrawerStyle.entries.map { it.name }, s.drawerStyle.ordinal) { vm.setStr(Keys.DRAWER, DrawerStyle.entries[it].name) }
                                Text("LONG-PRESS ANY APP TO PIN, HIDE OR UNINSTALL", style = t.micro)
                            }
                        }
                        item {
                            Group("FEEL") {
                                ToggleRow("Haptics", "Soft tick on taps", s.haptics) { vm.setBool(Keys.HAPTICS, it) }
                            }
                        }
                        item {
                            Group("HIDDEN APPS") {
                                if (hidden.isEmpty()) Text("NONE", style = t.micro)
                                hidden.forEach { app ->
                                    ActionRow(app.label, trailing = "UNHIDE") { vm.toggleHidden(app) }
                                }
                            }
                        }
                    }

                    2 -> {
                        item {
                            Group("DYNAMIC ISLAND") {
                                ToggleRow("Enable island", "A pill around your camera hole", s.islandEnabled) { vm.setBool(Keys.ISLAND, it) }
                                ServiceStatus(serviceOn, "NEEDS THE NOIR SYSTEM SERVICE") { SystemActions.openAccessibilitySettings(ctx) }
                                Text("A PREVIEW PILL IS SHOWN WHILE THIS TAB IS OPEN", style = t.micro)
                            }
                        }
                        item {
                            Group("SHOW WHEN") {
                                ToggleRow("Music plays", null, s.islandMedia) { vm.setBool(Keys.ISLAND_MEDIA, it) }
                                ToggleRow("Charger connected", null, s.islandCharging) { vm.setBool(Keys.ISLAND_CHARGING, it) }
                                ToggleRow("Notification arrives", null, s.islandNotifs) { vm.setBool(Keys.ISLAND_NOTIFS, it) }
                                ToggleRow("Always show hole pill", "Black pill even when idle", s.islandAlways) { vm.setBool(Keys.ISLAND_ALWAYS, it) }
                            }
                        }
                        item {
                            Group("FIT TO YOUR CAMERA") {
                                SliderRow("Distance from top", s.islandTop, 0f, 40f, { "${it.roundToInt()} dp" }) { vm.setFloat(Keys.ISLAND_TOP, it) }
                                SliderRow("Hole pill size", s.islandSize, 20f, 52f, { "${it.roundToInt()} dp" }) { vm.setFloat(Keys.ISLAND_SIZE, it) }
                                SliderRow("Island width", s.islandScale, 0.7f, 1.4f, { "${(it * 100).roundToInt()}%" }) { vm.setFloat(Keys.ISLAND_SCALE, it) }
                            }
                        }
                    }

                    3 -> {
                        item {
                            Group("NOTIFICATION CENTER") {
                                StatusLine(notifOn)
                                if (!notifOn) ActionRow("Allow notification access", "Needed to list notifications and music", "OPEN") { SystemActions.openNotificationAccess(ctx) }
                            }
                        }
                        item {
                            Group("CONTROL CENTER") {
                                ActionRow(
                                    "Brightness and rotation",
                                    if (writeOn) "ALLOWED" else "NOT ALLOWED YET",
                                    if (writeOn) null else "ALLOW",
                                ) { SystemActions.openWriteSettings(ctx) }
                                ActionRow(
                                    "Focus (Do not disturb)",
                                    if (dndOn) "ALLOWED" else "NOT ALLOWED YET",
                                    if (dndOn) null else "ALLOW",
                                ) { SystemActions.openPolicyAccess(ctx) }
                                Text("WI-FI, BLUETOOTH AND AIRPLANE OPEN THE SYSTEM PANEL: ANDROID DOES NOT LET APPS FLIP THEM DIRECTLY", style = t.micro)
                            }
                        }
                        item {
                            Group("SWIPE DOWN") {
                                NoirChips(listOf("NOIR PANELS", "SYSTEM SHADE"), s.swipeDown.ordinal) { vm.setStr(Keys.SWIPE_DOWN, SwipeDownMode.entries[it].name) }
                                Text("NOIR PANELS: LEFT HALF = NOTIFICATIONS · RIGHT HALF = CONTROL CENTER", style = t.micro)
                            }
                        }
                    }

                    4 -> {
                        item {
                            Group("DOUBLE TAP") {
                                NoirChips(listOf("LOCK SCREEN", "CONTROL CENTER", "NOTHING"), s.doubleTap.ordinal) { vm.setStr(Keys.DOUBLE_TAP, DoubleTapAction.entries[it].name) }
                                if (s.doubleTap == DoubleTapAction.LOCK) ServiceStatus(serviceOn, "LOCK NEEDS THE NOIR SYSTEM SERVICE") { SystemActions.openAccessibilitySettings(ctx) }
                            }
                        }
                        item {
                            Group("ALL GESTURES") {
                                Text("SWIPE UP · APPS", style = t.micro)
                                Text("SWIPE DOWN · PANELS", style = t.micro)
                                Text("SWIPE RIGHT · WIDGETS", style = t.micro)
                                Text("LONG-PRESS EMPTY SPACE · HOME MENU", style = t.micro)
                                Text("LONG-PRESS APP · PIN / HIDE / INFO / UNINSTALL", style = t.micro)
                            }
                        }
                    }

                    else -> {
                        item {
                            Group("NOIR SYSTEM SERVICE") {
                                ServiceStatus(serviceOn, "ENABLES ISLAND, LOCK, SCREENSHOT, POWER MENU") { SystemActions.openAccessibilitySettings(ctx) }
                                Text("IT NEVER READS YOUR SCREEN. IN SETTINGS > ACCESSIBILITY CHOOSE NOIR DOUBLE-TAP LOCK.", style = t.micro)
                            }
                        }
                        item {
                            Group("HOME APP") {
                                ActionRow("Set as default home app", trailing = "OPEN") { SystemActions.openDefaultHomeSettings(ctx) }
                            }
                        }
                        item {
                            Group("HONOR / MAGICOS TIP") {
                                Text("SETTINGS > BATTERY > APP LAUNCH > NOIR > MANAGE MANUALLY > ALLOW AUTO-LAUNCH, SECONDARY LAUNCH AND RUN IN BACKGROUND. THIS KEEPS THE ISLAND AND NOTIFICATIONS ALIVE.", style = t.micro)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(on: Boolean) {
    val t = LocalType.current
    Text(if (on) "ENABLED" else "DISABLED", style = t.meta.copy(color = if (on) Noir.Ink else Noir.Grey))
}

@Composable
private fun ServiceStatus(on: Boolean, hint: String, onOpen: () -> Unit) {
    val t = LocalType.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StatusLine(on)
        if (!on) {
            Text(hint, style = t.micro)
            ActionRow("Enable Noir system service", trailing = "OPEN", onClick = onOpen)
        }
    }
}

@Composable
private fun NameField(value: String, onCommit: (String) -> Unit) {
    val t = LocalType.current
    var text by remember(value) { mutableStateOf(value) }
    val focus = LocalFocusManager.current
    BasicTextField(
        value = text,
        onValueChange = { text = it },
        singleLine = true,
        textStyle = t.row,
        cursorBrush = SolidColor(Noir.Ink),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onCommit(text); focus.clearFocus() }),
        modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused && text != value) onCommit(text) },
        decorationBox = { inner ->
            Box {
                if (text.isEmpty()) Text("Shown in the greeting", style = t.row.copy(color = Noir.Grey))
                inner()
            }
        },
    )
}

@Composable
private fun WeatherGroup(vm: LauncherViewModel, enabled: Boolean, city: String, fahrenheit: Boolean) {
    val t = LocalType.current
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<Place>()) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.useDeviceLocation { ok ->
            if (!ok) Toast.makeText(ctx, "Turn on location, then try again", Toast.LENGTH_SHORT).show()
        }
        else Toast.makeText(ctx, "Location permission denied: search a city instead", Toast.LENGTH_SHORT).show()
    }
    LaunchedEffect(q) {
        delay(450)
        results = if (q.trim().length >= 2) vm.searchPlaces(q) else emptyList()
    }
    Group("WEATHER (OPEN-METEO)") {
        ToggleRow("Show weather", if (enabled && city.isNotBlank()) city.uppercase() else "Off by default, no account needed", enabled) { vm.setBool(Keys.WX, it) }
        if (enabled) {
            NoirChips(listOf("°C", "°F"), if (fahrenheit) 1 else 0) { vm.setBool(Keys.FAHRENHEIT, it == 1) }
            ActionRow("Use my location", "One tap, approximate position only", "GO") { perm.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
            BasicTextField(
                value = q,
                onValueChange = { q = it },
                singleLine = true,
                textStyle = t.row,
                cursorBrush = SolidColor(Noir.Ink),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box {
                        if (q.isEmpty()) Text("…or search a city", style = t.row.copy(color = Noir.Grey))
                        inner()
                    }
                },
            )
            results.forEach { p ->
                ActionRow(p.name, p.region.uppercase()) { vm.pickPlace(p); q = ""; results = emptyList() }
            }
        }
    }
}
