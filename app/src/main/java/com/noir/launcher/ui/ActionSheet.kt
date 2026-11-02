package com.noir.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.noir.launcher.ui.components.Hairline
import com.noir.launcher.ui.components.pressable
import com.noir.launcher.ui.theme.LocalType
import com.noir.launcher.ui.theme.Noir

class SheetItem(val label: String, val dim: Boolean = false, val action: () -> Unit)

/** Bottom sheet used for app long-press and the home-screen menu. */
@Composable
fun ActionSheet(title: String, items: List<SheetItem>, onDismiss: () -> Unit) {
    val t = LocalType.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Noir.Surface)
                .pointerInput(Unit) { detectTapGestures { } }
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            Text(title.uppercase(), style = t.micro)
            Hairline(Modifier.padding(vertical = 14.dp))
            items.forEach { item ->
                Text(
                    item.label,
                    style = t.drawer.copy(color = if (item.dim) Noir.Grey else Noir.Ink),
                    modifier = Modifier.fillMaxWidth().pressable(item.action).padding(vertical = 12.dp),
                )
            }
            Text(
                "Cancel",
                style = t.drawer.copy(color = Noir.Grey),
                modifier = Modifier.fillMaxWidth().pressable(onDismiss).padding(vertical = 12.dp),
            )
        }
    }
}
