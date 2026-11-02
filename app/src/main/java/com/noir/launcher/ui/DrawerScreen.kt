package com.noir.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noir.launcher.model.AppInfo
import com.noir.launcher.model.DrawerStyle
import com.noir.launcher.ui.components.AppIcon
import com.noir.launcher.ui.components.Hairline
import com.noir.launcher.ui.components.pressable
import com.noir.launcher.ui.theme.LocalType
import com.noir.launcher.ui.theme.Noir
import com.noir.launcher.util.sectionLetter
import kotlinx.coroutines.launch

@Composable
fun DrawerScreen(vm: LauncherViewModel, onLaunch: (AppInfo) -> Unit, onLongPress: (AppInfo) -> Unit) {
    val t = LocalType.current
    val all by vm.apps.collectAsStateWithLifecycle()
    val visible by vm.visibleApps.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(all, visible, query) {
        if (query.isBlank()) visible else all.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val grid = settings.drawerStyle == DrawerStyle.GRID

    val firstIndex = remember(filtered) {
        buildMap<Char, Int> {
            filtered.forEachIndexed { i, a -> val c = sectionLetter(a.label); if (!containsKey(c)) put(c, i) }
        }
    }
    val letters = remember(firstIndex) { firstIndex.keys.toList() }

    Box(Modifier.fillMaxSize().background(Noir.Black).systemBarsPadding().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 20.dp)) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = t.drawer,
                    cursorBrush = SolidColor(Noir.Ink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        filtered.firstOrNull()?.let { focus.clearFocus(); onLaunch(it) }
                    }),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Noir.Chip).padding(horizontal = 22.dp, vertical = 14.dp),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) Text("Search", style = t.drawer.copy(color = Noir.Grey))
                            inner()
                        }
                    },
                )
                Text("${filtered.size} APPS", style = t.micro, modifier = Modifier.padding(top = 14.dp, start = 6.dp))
            }
            if (grid) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    contentPadding = PaddingValues(start = 20.dp, end = 40.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(filtered, key = { it.key }) { app ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .pressable(onClick = { onLaunch(app) }, onLongClick = { onLongPress(app) }),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AppIcon(vm, app, settings.iconStyle, (settings.iconSize + 12f).dp)
                            Spacer(Modifier.padding(top = 8.dp))
                            Text(
                                app.label, style = t.micro.copy(letterSpacing = 0.4.sp, color = Noir.Ink),
                                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 28.dp, end = 48.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(filtered, key = { it.key }) { app ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .pressable(onClick = { onLaunch(app) }, onLongClick = { onLongPress(app) })
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(vm, app, settings.iconStyle, settings.iconSize.dp)
                            Spacer(Modifier.width(18.dp))
                            Text(app.label, style = t.drawer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        if (query.isBlank() && letters.size > 1) {
            var railH by remember { mutableIntStateOf(1) }
            fun jump(y: Float) {
                val idx = (y / railH * letters.size).toInt().coerceIn(0, letters.lastIndex)
                firstIndex[letters[idx]]?.let { target ->
                    scope.launch { if (grid) gridState.scrollToItem(target) else listState.scrollToItem(target) }
                }
            }
            Column(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .fillMaxHeight(0.72f)
                    .onSizeChanged { railH = it.height.coerceAtLeast(1) }
                    .pointerInput(letters) { detectTapGestures { jump(it.y) } }
                    .pointerInput(letters) { detectVerticalDragGestures { change, _ -> jump(change.position.y) } },
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                letters.forEach { Text(it.toString(), style = t.micro.copy(letterSpacing = 0.sp)) }
            }
        }
    }
}
