package com.noir.launcher.ui

import android.app.WallpaperManager
import android.appwidget.AppWidgetProviderInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noir.launcher.model.AppInfo
import com.noir.launcher.model.Backdrop
import com.noir.launcher.system.SystemActions
import com.noir.launcher.system.WidgetHostManager
import com.noir.launcher.ui.theme.LocalAccent
import com.noir.launcher.ui.theme.Noir

@Composable
fun LauncherRoot(
    vm: LauncherViewModel,
    widgetHost: WidgetHostManager,
    homeSignal: Int,
    onAddWidget: (AppWidgetProviderInfo) -> Unit,
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val accent = LocalAccent.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var notifOpen by remember { mutableStateOf(false) }
    var controlOpen by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }
    var sheetApp by remember { mutableStateOf<AppInfo?>(null) }
    var homeMenu by remember { mutableStateOf(false) }
    val pager = rememberPagerState(initialPage = 1) { 2 }

    val anyOverlay = drawerOpen || settingsOpen || notifOpen || controlOpen || pickerOpen || sheetApp != null || homeMenu

    // Home button pressed while we are already foreground: collapse everything.
    LaunchedEffect(homeSignal) {
        drawerOpen = false; settingsOpen = false; notifOpen = false; controlOpen = false
        pickerOpen = false; sheetApp = null; homeMenu = false
        if (homeSignal > 0) pager.animateScrollToPage(1)
    }

    // Wallpaper parallax while paging.
    LaunchedEffect(settings.backdrop) {
        snapshotFlow { pager.currentPage + pager.currentPageOffsetFraction }.collect { pos ->
            try {
                val wm = WallpaperManager.getInstance(ctx)
                wm.setWallpaperOffsets(view.windowToken, pos.coerceIn(0f, 1f), 0.5f)
            } catch (e: Exception) { }
        }
    }

    BackHandler(enabled = anyOverlay || pager.currentPage != 1) {
        when {
            sheetApp != null -> sheetApp = null
            homeMenu -> homeMenu = false
            pickerOpen -> pickerOpen = false
            settingsOpen -> settingsOpen = false
            controlOpen -> controlOpen = false
            notifOpen -> notifOpen = false
            drawerOpen -> drawerOpen = false
            else -> {}
        }
    }

    val bg = when (settings.backdrop) {
        Backdrop.WALLPAPER -> Modifier.background(
            Brush.verticalGradient(listOf(Color.Black.copy(alpha = settings.dim * 0.7f), Color.Black.copy(alpha = settings.dim + 0.35f)))
        )
        else -> Modifier.background(Noir.Black)
    }

    Box(Modifier.fillMaxSize().then(bg)) {
        if (settings.backdrop == Backdrop.AURORA) AuroraBackdrop(accent, Modifier.fillMaxSize())
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), userScrollEnabled = !anyOverlay) { page ->
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    val off = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                    alpha = 1f - 0.7f * off
                    scaleX = 1f - 0.08f * off
                    scaleY = 1f - 0.08f * off
                }
            ) {
            if (page == 0) {
                WidgetsScreen(
                    vm = vm,
                    host = widgetHost,
                    onAdd = { pickerOpen = true },
                    onOpenSettings = { settingsOpen = true },
                )
            } else {
                HomeScreen(
                    vm = vm,
                    onOpenDrawer = { drawerOpen = true },
                    onOpenNotifications = { notifOpen = true },
                    onOpenControl = { controlOpen = true },
                    onHomeMenu = { homeMenu = true },
                    onAppLongPress = { sheetApp = it },
                )
            }
            }
        }

        if (!anyOverlay) {
            Row(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (i in 0..1) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (pager.currentPage == i) accent else Noir.Line))
                }
            }
        }

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically(tween(280)) { it / 6 } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(220)) { it / 6 } + fadeOut(tween(180)),
        ) {
            DrawerScreen(
                vm = vm,
                onLaunch = { drawerOpen = false; vm.launch(it) },
                onLongPress = { sheetApp = it },
            )
        }
        AnimatedVisibility(
            visible = notifOpen,
            enter = slideInVertically(tween(300)) { -it / 3 } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(220)) { -it / 3 } + fadeOut(tween(160)),
        ) { NotificationCenter(vm) { notifOpen = false } }
        AnimatedVisibility(
            visible = controlOpen,
            enter = slideInVertically(tween(300)) { -it / 3 } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(220)) { -it / 3 } + fadeOut(tween(160)),
        ) { ControlCenter(onClose = { controlOpen = false }, onOpenSettings = { controlOpen = false; settingsOpen = true }) }
        AnimatedVisibility(settingsOpen, enter = fadeIn(tween(200)), exit = fadeOut(tween(160))) {
            SettingsScreen(vm) { settingsOpen = false }
        }
        AnimatedVisibility(pickerOpen, enter = fadeIn(tween(200)), exit = fadeOut(tween(160))) {
            WidgetPicker(widgetHost, onPick = { pickerOpen = false; onAddWidget(it) }, onClose = { pickerOpen = false })
        }

        if (homeMenu) {
            ActionSheet(
                title = "Noir",
                items = listOf(
                    SheetItem("Customize") { homeMenu = false; settingsOpen = true },
                    SheetItem("Add widget") { homeMenu = false; pickerOpen = true },
                    SheetItem("Control Center") { homeMenu = false; controlOpen = true },
                    SheetItem("Change wallpaper") { homeMenu = false; SystemActions.chooseWallpaper(ctx) },
                ),
                onDismiss = { homeMenu = false },
            )
        }

        sheetApp?.let { app ->
            val pinned = vm.isPinned(app)
            val items = buildList {
                if (pinned || vm.canPinMore()) add(SheetItem(if (pinned) "Unpin from home" else "Pin to home") { vm.togglePin(app); sheetApp = null })
                add(SheetItem(if (vm.isHidden(app)) "Unhide" else "Hide app") { vm.toggleHidden(app); sheetApp = null })
                add(SheetItem("App info") { vm.showInfo(app); sheetApp = null })
                add(SheetItem("Uninstall") { SystemActions.uninstall(ctx, app.component.packageName); sheetApp = null })
            }
            ActionSheet(title = app.label, items = items, onDismiss = { sheetApp = null })
        }
    }
}
