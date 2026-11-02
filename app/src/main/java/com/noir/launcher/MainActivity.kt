package com.noir.launcher

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noir.launcher.system.WidgetHostManager
import com.noir.launcher.ui.LauncherRoot
import com.noir.launcher.ui.LauncherViewModel
import com.noir.launcher.ui.theme.NoirTheme

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()
    private var homeSignal by mutableIntStateOf(0)
    private lateinit var widgetHost: WidgetHostManager
    private var pendingId = -1

    private val bindLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) afterBind(pendingId) else cancelPending()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetHost = WidgetHostManager(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            val s by vm.settings.collectAsStateWithLifecycle()
            NoirTheme(s) {
                LauncherRoot(vm, widgetHost, homeSignal) { addWidget(it) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHost.start()
    }

    override fun onStop() {
        widgetHost.stop()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) homeSignal++
    }

    private fun addWidget(p: AppWidgetProviderInfo) {
        val id = widgetHost.allocate()
        pendingId = id
        if (widgetHost.tryBind(id, p.provider)) {
            afterBind(id)
        } else {
            val i = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, p.provider)
            try { bindLauncher.launch(i) } catch (e: Exception) { cancelPending() }
        }
    }

    private fun afterBind(id: Int) {
        val info = widgetHost.info(id)
        if (info?.configure != null) {
            try {
                widgetHost.host.startAppWidgetConfigureActivityForResult(this, id, 0, REQ_CONFIGURE, null)
            } catch (e: Exception) {
                vm.addWidgetId(id)
            }
        } else {
            vm.addWidgetId(id)
        }
    }

    private fun cancelPending() {
        if (pendingId != -1) widgetHost.delete(pendingId)
        pendingId = -1
    }

    @Deprecated("Needed for the widget configure activity result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CONFIGURE) {
            if (resultCode == RESULT_OK) vm.addWidgetId(pendingId) else cancelPending()
            pendingId = -1
        }
    }

    companion object { private const val REQ_CONFIGURE = 77 }
}
