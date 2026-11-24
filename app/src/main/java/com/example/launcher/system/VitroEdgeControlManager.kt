package com.example.launcher.system

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.example.launcher.QuickPanelActivity
import com.example.launcher.VitroLockScreenActivity
import com.example.launcher.model.EdgePanelTriggerZone
import com.example.launcher.model.LiveNotification
import kotlin.math.abs

/**
 * Replaces the phone's native Quick Settings pull-down, system-wide heads-up Dynamic Island
 * notifications, and native wake Lock Screen overlay across ALL apps via SYSTEM_ALERT_WINDOW.
 */
object VitroEdgeControlManager {

    private var edgeView: View? = null
    private var currentZone: EdgePanelTriggerZone? = null
    private var notifIslandView: View? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var dismissNotifRunnable: Runnable? = null

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
                setBackgroundColor(Color.TRANSPARENT)
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

    /**
     * Shows an expanding iPhone 18 Pro Max Dynamic Island notification pill over ANY app on the phone,
     * replacing the native Android heads-up notification banner.
     */
    fun showSystemWideNotifIsland(context: Context, notif: LiveNotification) {
        val appContext = context.applicationContext
        if (!Settings.canDrawOverlays(appContext)) return
        mainHandler.post {
            dismissSystemWideNotifIsland(appContext)
            runCatching {
                val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return@post
                val density = appContext.resources.displayMetrics.density
                val maxW = (appContext.resources.displayMetrics.widthPixels * 0.90f).toInt()
                    .coerceAtMost((360 * density).toInt())

                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    maxW,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = (14 * density).toInt()
                }

                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 28f * density
                    setColor(Color.argb(242, 6, 10, 22))
                    setStroke((1.4f * density).toInt(), Color.argb(150, 56, 189, 248))
                }

                val container = LinearLayout(appContext).apply {
                    orientation = LinearLayout.VERTICAL
                    background = bg
                    val padH = (18 * density).toInt()
                    val padV = (12 * density).toInt()
                    setPadding(padH, padV, padH, padV)
                    alpha = 0f
                    scaleX = 0.72f
                    scaleY = 0.72f

                    val appTag = TextView(appContext).apply {
                        text = "● ${notif.appName} • Baru Saja"
                        setTextColor(Color.parseColor("#38BDF8"))
                        textSize = 11f
                        setTypeface(null, Typeface.BOLD)
                    }
                    val titleTv = TextView(appContext).apply {
                        text = notif.title
                        setTextColor(Color.WHITE)
                        textSize = 14f
                        setTypeface(null, Typeface.BOLD)
                        maxLines = 1
                        ellipsize = TextUtils.TruncateAt.END
                    }
                    val bodyTv = TextView(appContext).apply {
                        text = notif.text
                        setTextColor(Color.parseColor("#CBD5E1"))
                        textSize = 12.5f
                        maxLines = 2
                        ellipsize = TextUtils.TruncateAt.END
                    }

                    addView(appTag)
                    addView(titleTv)
                    addView(bodyTv)

                    setOnClickListener {
                        dismissSystemWideNotifIsland(appContext)
                        NotificationHub.openNotification(appContext, notif)
                    }
                }

                wm.addView(container, params)
                notifIslandView = container

                container.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(260L)
                    .start()

                val hideTask = Runnable { dismissSystemWideNotifIsland(appContext) }
                dismissNotifRunnable = hideTask
                mainHandler.postDelayed(hideTask, 4_800L)
            }
        }
    }

    fun dismissSystemWideNotifIsland(context: Context) {
        dismissNotifRunnable?.let { mainHandler.removeCallbacks(it) }
        dismissNotifRunnable = null
        val view = notifIslandView ?: return
        runCatching {
            val wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            wm?.removeViewImmediate(view)
        }
        notifIslandView = null
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

    fun launchVitroLockScreen(context: Context) {
        runCatching {
            val intent = Intent(context, VitroLockScreenActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            }
            context.startActivity(intent)
        }
    }
}
