package com.example.launcher.system

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.example.launcher.QuickPanelActivity
import com.example.launcher.model.EdgePanelTriggerZone
import kotlin.math.abs

/**
 * Replaces the phone's native Quick Settings / Control Center pull-down across ALL apps
 * by placing a clean, invisible top-edge swipe-down trigger via SYSTEM_ALERT_WINDOW.
 */
object VitroEdgeControlManager {

    private var edgeView: View? = null
    private var currentZone: EdgePanelTriggerZone? = null

    fun syncEdgeTrigger(
        context: Context,
        enabled: Boolean,
        zone: EdgePanelTriggerZone
    ) {
        val appContext = context.applicationContext
        if (!enabled || !Settings.canDrawOverlays(appContext)) {
            removeTrigger(appContext)
            return
        }
        if (edgeView != null && currentZone == zone) return
        removeTrigger(appContext)
        attachTrigger(appContext, zone)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachTrigger(context: Context, zone: EdgePanelTriggerZone) {
        runCatching {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            val dm = context.resources.displayMetrics
            val screenWidth = dm.widthPixels
            val density = dm.density

            val stripHeight = (26f * density).toInt()
            val stripWidth = when (zone) {
                EdgePanelTriggerZone.TOP_FULL -> screenWidth
                else -> (screenWidth * 0.42f).toInt().coerceAtLeast((120f * density).toInt())
            }
            val gravity = when (zone) {
                EdgePanelTriggerZone.TOP_RIGHT -> Gravity.TOP or Gravity.END
                EdgePanelTriggerZone.TOP_LEFT -> Gravity.TOP or Gravity.START
                EdgePanelTriggerZone.TOP_FULL -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                stripWidth,
                stripHeight,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                this.gravity = gravity
                x = 0
                y = 0
            }

            val triggerView = View(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                var startX = 0f
                var startY = 0f
                var triggered = false

                setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = event.rawX
                            startY = event.rawY
                            triggered = false
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dy = event.rawY - startY
                            val dx = abs(event.rawX - startX)
                            if (!triggered && dy > 28f * density && dy > dx) {
                                triggered = true
                                launchQuickPanelOverlay(context)
                            }
                            true
                        }
                        else -> false
                    }
                }
            }

            wm.addView(triggerView, params)
            edgeView = triggerView
            currentZone = zone
        }
    }

    fun removeTrigger(context: Context) {
        val view = edgeView ?: return
        runCatching {
            val wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            wm?.removeViewImmediate(view)
        }
        edgeView = null
        currentZone = null
    }

    fun launchQuickPanelOverlay(context: Context) {
        runCatching {
            val intent = Intent(context, QuickPanelActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }
            context.startActivity(intent)
        }
    }
}
