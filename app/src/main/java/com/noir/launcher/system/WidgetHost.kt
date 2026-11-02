package com.noir.launcher.system

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context

/** Hosts real Android widgets (clock, calendar, Spotify, ...) inside the Widgets page. */
class WidgetHostManager(context: Context) {
    private val app = context.applicationContext
    val host = AppWidgetHost(app, HOST_ID)
    val manager: AppWidgetManager = AppWidgetManager.getInstance(app)

    fun start() { try { host.startListening() } catch (e: Exception) { } }
    fun stop() { try { host.stopListening() } catch (e: Exception) { } }

    fun providers(): List<AppWidgetProviderInfo> = try {
        manager.installedProviders.filter { (it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) != 0 }
    } catch (e: Exception) { emptyList() }

    fun allocate(): Int = host.allocateAppWidgetId()
    fun delete(id: Int) { try { host.deleteAppWidgetId(id) } catch (e: Exception) { } }
    fun tryBind(id: Int, provider: ComponentName): Boolean = try {
        manager.bindAppWidgetIdIfAllowed(id, provider)
    } catch (e: Exception) { false }

    fun info(id: Int): AppWidgetProviderInfo? = try { manager.getAppWidgetInfo(id) } catch (e: Exception) { null }

    fun createView(ctx: Context, id: Int): AppWidgetHostView? {
        val i = info(id) ?: return null
        return try { host.createView(ctx, id, i) } catch (e: Exception) { null }
    }

    companion object { const val HOST_ID = 4242 }
}
