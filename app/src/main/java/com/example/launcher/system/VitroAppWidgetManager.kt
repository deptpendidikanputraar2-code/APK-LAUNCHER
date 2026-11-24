package com.example.launcher.system

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class AvailableAndroidWidget(
    val providerInfo: AppWidgetProviderInfo,
    val appName: String,
    val widgetLabel: String,
    val previewIcon: ImageBitmap?
)

class VitroAppWidgetManager(context: Context) {

    companion object {
        const val APPWIDGET_HOST_ID = 202607
    }

    private val appContext = context.applicationContext
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(appContext)
    val appWidgetHost: AppWidgetHost = AppWidgetHost(appContext, APPWIDGET_HOST_ID)

    fun startListening() {
        runCatching { appWidgetHost.startListening() }
    }

    fun stopListening() {
        runCatching { appWidgetHost.stopListening() }
    }

    fun allocateWidgetId(): Int = appWidgetHost.allocateAppWidgetId()

    fun deleteWidgetId(appWidgetId: Int) {
        runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
    }

    fun bindIfAllowed(appWidgetId: Int, info: AppWidgetProviderInfo): Boolean {
        return runCatching {
            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider)
        }.getOrDefault(false)
    }

    fun getProviderInfo(appWidgetId: Int): AppWidgetProviderInfo? {
        return runCatching { appWidgetManager.getAppWidgetInfo(appWidgetId) }.getOrNull()
    }

    fun createHostView(activityContext: Context, appWidgetId: Int): AppWidgetHostView? {
        val info = getProviderInfo(appWidgetId) ?: return null
        return runCatching {
            appWidgetHost.createView(activityContext, appWidgetId, info).apply {
                setAppWidget(appWidgetId, info)
            }
        }.getOrNull()
    }

    fun loadInstalledAndroidWidgets(): List<AvailableAndroidWidget> {
        val pm = appContext.packageManager
        val providers = runCatching { appWidgetManager.installedProviders }.getOrDefault(emptyList())
        return providers
            .mapNotNull { info ->
                runCatching {
                    val pkg = info.provider.packageName
                    val appName = runCatching {
                        val ai = pm.getApplicationInfo(pkg, 0)
                        pm.getApplicationLabel(ai).toString()
                    }.getOrDefault(pkg.substringAfterLast('.'))
                    val widgetLabel = info.loadLabel(pm).orEmpty().ifBlank { appName }
                    val previewDrawable = runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            info.loadIcon(appContext, appContext.resources.displayMetrics.densityDpi)
                        } else {
                            pm.getApplicationIcon(pkg)
                        }
                    }.getOrNull() ?: runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
                    AvailableAndroidWidget(
                        providerInfo = info,
                        appName = appName,
                        widgetLabel = widgetLabel,
                        previewIcon = previewDrawable?.let { drawableToBitmap(it) }
                    )
                }.getOrNull()
            }
            .sortedWith(compareBy({ it.appName.lowercase() }, { it.widgetLabel.lowercase() }))
    }

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int = 96): ImageBitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap.asImageBitmap()
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }
}
