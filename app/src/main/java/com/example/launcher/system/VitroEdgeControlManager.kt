package com.example.launcher.system

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.example.launcher.QuickPanelActivity
import com.example.launcher.VitroLockScreenActivity
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.EdgePanelTriggerZone
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.LedBlinkPattern
import com.example.launcher.model.LedPlacement
import com.example.launcher.model.LiveNotification
import com.example.launcher.ui.components.BlackBerryLedEvaluator
import com.example.launcher.ui.components.ResolvedLedState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

/**
 * Replaces the phone's native Quick Settings pull-down, persistent top Status Indicator,
 * interactive Dynamic Island, and Lock Screen across ALL apps via lightweight native WindowManager overlays.
 *
 * Battery-safe architecture:
 * - Zero polling loops or 60fps animations when idle.
 * - Updates strictly on Android system broadcasts (TIME_TICK, BATTERY_CHANGED, CONNECTIVITY_ACTION)
 *   and NotificationHub push events.
 * - Automatically detaches persistent overlays when Vitro Launcher is in foreground or when the screen turns OFF.
 */
object VitroEdgeControlManager {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val foregroundCounter = AtomicInteger(0)

    @Volatile
    private var isQuickPanelInForeground: Boolean = false
    @Volatile
    private var isScreenInteractive: Boolean = true
    @Volatile
    private var lastConfig: LauncherConfig = LauncherConfig()
    @Volatile
    private var torchEnabled: Boolean = false

    private var edgeView: View? = null
    private var currentZone: EdgePanelTriggerZone? = null

    private var statusIslandView: View? = null
    private var statusLeftTv: TextView? = null
    private var statusRightTv: TextView? = null
    private var statusBroadcastRegistered: Boolean = false
    private var statusReceiver: BroadcastReceiver? = null

    private var expandedIslandView: View? = null
    private var dismissExpandedRunnable: Runnable? = null

    private var systemLedView: SystemDualLedView? = null
    private var ledTickRunnable: Runnable? = null
    private var systemIndicatorCondSig: String = ""
    private var systemIndicatorCondStart: Long = System.currentTimeMillis()
    @Volatile
    private var manualTestLed1Start: Long = 0L
    @Volatile
    private var manualTestLed2Start: Long = 0L

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun isVitroInForeground(): Boolean = foregroundCounter.get() > 0

    fun onVitroActivityResumed(context: Context, isQuickPanel: Boolean = false) {
        foregroundCounter.incrementAndGet()
        if (isQuickPanel) isQuickPanelInForeground = true
        val appContext = context.applicationContext
        // Hide system-wide persistent overlays while inside FeyyL so they never double up with Compose UI
        mainHandler.post {
            detachPersistentStatusIsland(appContext)
            detachSystemLedOverlay(appContext)
            dismissExpandedIsland(appContext)
            if (isQuickPanel) removeTrigger(appContext)
        }
    }

    fun onVitroActivityPaused(context: Context, isQuickPanel: Boolean = false) {
        val remaining = foregroundCounter.decrementAndGet().coerceAtLeast(0)
        if (remaining == 0) foregroundCounter.set(0)
        if (isQuickPanel) isQuickPanelInForeground = false
        val appContext = context.applicationContext
        // Delay slightly so transitions between FeyyL activities (e.g., MainActivity -> QuickPanelActivity) don't flicker
        mainHandler.postDelayed({
            syncSystemWideUi(appContext, lastConfig)
        }, 180L)
    }

    fun onScreenPowerChanged(context: Context, interactive: Boolean) {
        isScreenInteractive = interactive
        val appContext = context.applicationContext
        mainHandler.post {
            if (!interactive) {
                detachPersistentStatusIsland(appContext)
                detachSystemLedOverlay(appContext)
                dismissExpandedIsland(appContext)
                removeTrigger(appContext)
            } else {
                syncSystemWideUi(appContext, lastConfig)
            }
        }
    }

    fun triggerLedTestPulse(ledIndex: Int) {
        val now = System.currentTimeMillis()
        if (ledIndex == 1) manualTestLed1Start = now else manualTestLed2Start = now
    }

    fun syncEdgeTrigger(
        context: Context,
        enabled: Boolean,
        zone: EdgePanelTriggerZone
    ) {
        lastConfig = lastConfig.copy(
            replaceSystemControlPanel = enabled,
            edgeTriggerZone = zone
        )
        syncSystemWideUi(context, lastConfig)
    }

    fun syncSystemWideUi(context: Context, config: LauncherConfig) {
        lastConfig = config
        val appContext = context.applicationContext
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        isScreenInteractive = pm?.isInteractive ?: true

        mainHandler.post {
            val canOverlay = Settings.canDrawOverlays(appContext)
            if (!canOverlay || !isScreenInteractive) {
                removeTrigger(appContext)
                detachPersistentStatusIsland(appContext)
                detachSystemLedOverlay(appContext)
                dismissExpandedIsland(appContext)
                return@post
            }

            // 1. Control Panel top-edge pull-down trigger (works over ALL apps except while QuickPanel itself is open)
            if (config.replaceSystemControlPanel && !isQuickPanelInForeground) {
                if (edgeView == null || currentZone != config.edgeTriggerZone) {
                    removeTrigger(appContext)
                    attachTrigger(appContext, config.edgeTriggerZone)
                }
            } else {
                removeTrigger(appContext)
            }

            // 2. Persistent System-Wide Status Indicator + Dynamic Island Pill (shown ONLY over OTHER apps)
            val shouldShowSystemPill = !isVitroInForeground() &&
                (config.showSystemWideDynamicIsland || config.dynamicIsland.enabled) &&
                expandedIslandView == null
            if (shouldShowSystemPill) {
                attachPersistentStatusIsland(appContext)
                updatePersistentStatusTexts(appContext)
            } else {
                detachPersistentStatusIsland(appContext)
            }

            // 3. System-Wide Dual BlackBerry LED Lamps (shown ONLY over OTHER apps when active)
            val ledCfg = config.ledConfig
            val shouldShowSystemLed = !isVitroInForeground() &&
                ledCfg.masterEnabled &&
                ledCfg.showOverOtherApps
            if (shouldShowSystemLed) {
                syncSystemLedOverlay(appContext)
            } else {
                detachSystemLedOverlay(appContext)
            }
        }
    }

    fun refreshSystemIslandContent(context: Context) {
        val appContext = context.applicationContext
        mainHandler.post {
            if (statusIslandView != null) {
                updatePersistentStatusTexts(appContext)
            }
            if (!isVitroInForeground() && isScreenInteractive &&
                lastConfig.ledConfig.masterEnabled && lastConfig.ledConfig.showOverOtherApps &&
                Settings.canDrawOverlays(appContext)
            ) {
                syncSystemLedOverlay(appContext)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachTrigger(context: Context, zone: EdgePanelTriggerZone) {
        runCatching {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            val dm = context.resources.displayMetrics
            val screenWidth = dm.widthPixels
            val density = dm.density

            val stripHeight = (30f * density).toInt()
            val stripWidth = when (zone) {
                EdgePanelTriggerZone.TOP_FULL -> screenWidth
                else -> (screenWidth * 0.40f).toInt().coerceAtLeast((116f * density).toInt())
            }
            val gravity = when (zone) {
                EdgePanelTriggerZone.TOP_RIGHT -> Gravity.TOP or Gravity.END
                EdgePanelTriggerZone.TOP_LEFT -> Gravity.TOP or Gravity.START
                EdgePanelTriggerZone.TOP_FULL -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }

            val params = WindowManager.LayoutParams(
                stripWidth,
                stripHeight,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                this.gravity = gravity
                x = 0
                y = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
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
                            if (!triggered && dy > 24f * density && dy > dx) {
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
     * Lightweight, battery-friendly persistent Dynamic Island + Status Indicator pill
     * displayed at the top center whenever the user is inside ANY other app.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun attachPersistentStatusIsland(context: Context) {
        if (statusIslandView != null) return
        runCatching {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            val density = context.resources.displayMetrics.density
            val cfg = lastConfig.dynamicIsland
            val pillW = (cfg.widthDp.coerceIn(148f, 230f) * density).toInt()
            val pillH = (cfg.heightDp.coerceIn(28f, 40f) * density).toInt()

            val params = WindowManager.LayoutParams(
                pillW,
                pillH,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                x = (cfg.horizontalOffsetDp * density).toInt()
                y = (cfg.topOffsetDp.coerceIn(4f, 48f) * density).toInt()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = pillH / 2f
                setColor(Color.argb(242, 4, 7, 16))
                setStroke((1.1f * density).toInt(), Color.argb(128, 56, 189, 248))
            }

            val leftTv = TextView(context).apply {
                setTextColor(Color.WHITE)
                textSize = 11.5f
                setTypeface(null, Typeface.BOLD)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
            }

            val spacer = View(context)

            val rightTv = TextView(context).apply {
                setTextColor(Color.parseColor("#38BDF8"))
                textSize = 11.5f
                setTypeface(null, Typeface.BOLD)
                maxLines = 1
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
            }

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = bg
                val padH = (12 * density).toInt()
                setPadding(padH, 0, padH, 0)

                addView(leftTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(spacer, LinearLayout.LayoutParams((18 * density).toInt(), 1))
                addView(rightTv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

                var downX = 0f
                var downY = 0f
                var downTime = 0L
                var swipeHandled = false

                setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = event.rawX
                            downY = event.rawY
                            downTime = SystemClock.uptimeMillis()
                            swipeHandled = false
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dy = event.rawY - downY
                            val dx = abs(event.rawX - downX)
                            if (!swipeHandled && dy > 26f * density && dy > dx) {
                                swipeHandled = true
                                launchQuickPanelOverlay(context)
                            }
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (!swipeHandled) {
                                val elapsed = SystemClock.uptimeMillis() - downTime
                                val moved = abs(event.rawX - downX) + abs(event.rawY - downY)
                                if (moved < 18f * density) {
                                    if (elapsed >= 480L) {
                                        launchQuickPanelOverlay(context)
                                    } else {
                                        showExpandedSystemIsland(context, incomingNotif = null)
                                    }
                                }
                            }
                            true
                        }
                        else -> false
                    }
                }
            }

            wm.addView(container, params)
            statusIslandView = container
            statusLeftTv = leftTv
            statusRightTv = rightTv
            registerStatusReceiver(context)
        }
    }

    private fun detachPersistentStatusIsland(context: Context) {
        unregisterStatusReceiver(context)
        val view = statusIslandView ?: return
        runCatching {
            val wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            wm?.removeViewImmediate(view)
        }
        statusIslandView = null
        statusLeftTv = null
        statusRightTv = null
    }

    private fun registerStatusReceiver(context: Context) {
        if (statusBroadcastRegistered) return
        val appContext = context.applicationContext
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                updatePersistentStatusTexts(appContext)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            @Suppress("DEPRECATION")
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        runCatching {
            appContext.registerReceiver(receiver, filter)
            statusReceiver = receiver
            statusBroadcastRegistered = true
        }
    }

    private fun unregisterStatusReceiver(context: Context) {
        if (!statusBroadcastRegistered) return
        statusReceiver?.let { r ->
            runCatching { context.applicationContext.unregisterReceiver(r) }
        }
        statusReceiver = null
        statusBroadcastRegistered = false
    }

    private fun updatePersistentStatusTexts(context: Context) {
        val left = statusLeftTv ?: return
        val right = statusRightTv ?: return

        val timeText = timeFormat.format(Date())
        val nowPlaying = NotificationHub.nowPlaying.value
        val latestNotif = NotificationHub.notifications.value.firstOrNull()

        left.text = when {
            nowPlaying.hasActiveSession -> "♪ ${nowPlaying.title.take(10)}"
            latestNotif != null && lastConfig.dynamicIsland.showNotifications -> "● $timeText"
            else -> timeText
        }

        val (pct, charging) = readQuickBattery(context)
        val netGlyph = readQuickNetworkGlyph(context)
        val bolt = if (charging) "⚡" else ""
        right.text = "$netGlyph $bolt$pct%"
        right.setTextColor(
            when {
                charging -> Color.parseColor("#34D399")
                pct <= 20 -> Color.parseColor("#FB7185")
                else -> Color.parseColor("#38BDF8")
            }
        )
    }

    /**
     * Shows an expanding iPhone 18 Pro Max Dynamic Island notification over ANY app on the phone,
     * replacing native heads-up notifications. Tapping opens the target app immediately!
     */
    fun showSystemWideNotifIsland(context: Context, notif: LiveNotification) {
        val appContext = context.applicationContext
        if (!Settings.canDrawOverlays(appContext) || !isScreenInteractive) return
        mainHandler.post {
            showExpandedSystemIsland(appContext, incomingNotif = notif)
        }
    }

    /**
     * Interactive expanded Dynamic Island over ANY app:
     * - If [incomingNotif] is provided: sleek heads-up notification card with clickable open + dismiss.
     * - If [incomingNotif] is null (user tapped the system status pill): full Pro Max deck with
     *   time/battery header, music Prev/Play/Next, latest clickable notification, Flashlight, & Control Center!
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun showExpandedSystemIsland(context: Context, incomingNotif: LiveNotification?) {
        val appContext = context.applicationContext
        if (!Settings.canDrawOverlays(appContext)) return
        dismissExpandedIsland(appContext, reattachPill = false)
        detachPersistentStatusIsland(appContext)

        runCatching {
            val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            val dm = appContext.resources.displayMetrics
            val density = dm.density
            val maxW = (dm.widthPixels * 0.92f).toInt().coerceAtMost((368 * density).toInt())

            val params = WindowManager.LayoutParams(
                maxW,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (12 * density).toInt()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            val cardBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28f * density
                setColor(Color.argb(246, 5, 9, 20))
                setStroke((1.4f * density).toInt(), Color.argb(165, 56, 189, 248))
            }

            val root = LinearLayout(appContext).apply {
                orientation = LinearLayout.VERTICAL
                background = cardBg
                val padH = (16 * density).toInt()
                val padV = (12 * density).toInt()
                setPadding(padH, padV, padH, padV)
                alpha = 0f
                scaleX = 0.80f
                scaleY = 0.80f

                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_OUTSIDE) {
                        dismissExpandedIsland(appContext, reattachPill = true)
                        true
                    } else false
                }
            }

            if (incomingNotif != null) {
                // Heads-Up Notification Mode (fully clickable!)
                val topRow = LinearLayout(appContext).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val textCol = LinearLayout(appContext).apply {
                    orientation = LinearLayout.VERTICAL
                    isClickable = true
                    setOnClickListener {
                        dismissExpandedIsland(appContext, reattachPill = true)
                        NotificationHub.openNotification(appContext, incomingNotif)
                    }
                }
                textCol.addView(TextView(appContext).apply {
                    text = "● ${incomingNotif.appName} • Baru Saja"
                    setTextColor(Color.parseColor("#38BDF8"))
                    textSize = 11f
                    setTypeface(null, Typeface.BOLD)
                })
                textCol.addView(TextView(appContext).apply {
                    text = incomingNotif.title
                    setTextColor(Color.WHITE)
                    textSize = 14.5f
                    setTypeface(null, Typeface.BOLD)
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                })
                textCol.addView(TextView(appContext).apply {
                    text = incomingNotif.text
                    setTextColor(Color.parseColor("#CBD5E1"))
                    textSize = 12.5f
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                })

                val closeBtn = TextView(appContext).apply {
                    text = "✕"
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 16f
                    gravity = Gravity.CENTER
                    val sz = (36 * density).toInt()
                    layoutParams = LinearLayout.LayoutParams(sz, sz)
                    setOnClickListener {
                        dismissExpandedIsland(appContext, reattachPill = true)
                    }
                }

                topRow.addView(textCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                topRow.addView(closeBtn)
                root.addView(topRow)
                root.setOnClickListener {
                    dismissExpandedIsland(appContext, reattachPill = true)
                    NotificationHub.openNotification(appContext, incomingNotif)
                }
            } else {
                // Full Interactive Dynamic Island Deck over any app
                val (pct, charging) = readQuickBattery(appContext)
                val netGlyph = readQuickNetworkGlyph(appContext)
                val nowPlaying = NotificationHub.nowPlaying.value
                val latestNotif = NotificationHub.notifications.value.firstOrNull()

                // 1. Header Row
                val header = LinearLayout(appContext).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                header.addView(
                    TextView(appContext).apply {
                        text = "${timeFormat.format(Date())} • $netGlyph • ${if (charging) "⚡" else ""}$pct%"
                        setTextColor(Color.WHITE)
                        textSize = 12.5f
                        setTypeface(null, Typeface.BOLD)
                    },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                )
                header.addView(TextView(appContext).apply {
                    text = "✕"
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 15f
                    gravity = Gravity.CENTER
                    val sz = (32 * density).toInt()
                    layoutParams = LinearLayout.LayoutParams(sz, sz)
                    setOnClickListener { dismissExpandedIsland(appContext, reattachPill = true) }
                })
                root.addView(header)

                // 2. Music Control Row
                val musicRow = LinearLayout(appContext).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = roundedBox(density, 18f, Color.argb(42, 99, 102, 241), Color.argb(65, 255, 255, 255))
                    val p = (10 * density).toInt()
                    setPadding(p, p, p, p)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = (8 * density).toInt() }
                }
                val trackInfo = LinearLayout(appContext).apply { orientation = LinearLayout.VERTICAL }
                trackInfo.addView(TextView(appContext).apply {
                    text = nowPlaying.title.ifBlank { "Pemutar Musik HP" }
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                })
                trackInfo.addView(TextView(appContext).apply {
                    text = nowPlaying.artist.ifBlank { "Kontrol Media Langsung" }
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 11f
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                })
                musicRow.addView(trackInfo, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                musicRow.addView(mediaKeyButton(appContext, density, "⏮") { dispatchMediaKey(appContext, KeyEvent.KEYCODE_MEDIA_PREVIOUS) })
                musicRow.addView(mediaKeyButton(appContext, density, "⏯") { dispatchMediaKey(appContext, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) })
                musicRow.addView(mediaKeyButton(appContext, density, "⏭") { dispatchMediaKey(appContext, KeyEvent.KEYCODE_MEDIA_NEXT) })
                root.addView(musicRow)

                // 3. Latest Clickable Notification Row (if any)
                if (latestNotif != null) {
                    val notifRow = LinearLayout(appContext).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = roundedBox(density, 16f, Color.argb(26, 255, 255, 255), Color.argb(45, 255, 255, 255))
                        val p = (10 * density).toInt()
                        setPadding(p, p, p, p)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { topMargin = (8 * density).toInt() }
                        setOnClickListener {
                            dismissExpandedIsland(appContext, reattachPill = true)
                            NotificationHub.openNotification(appContext, latestNotif)
                        }
                    }
                    val nCol = LinearLayout(appContext).apply { orientation = LinearLayout.VERTICAL }
                    nCol.addView(TextView(appContext).apply {
                        text = "${latestNotif.appName} • ${latestNotif.title}"
                        setTextColor(Color.WHITE)
                        textSize = 12.5f
                        setTypeface(null, Typeface.BOLD)
                        maxLines = 1
                        ellipsize = TextUtils.TruncateAt.END
                    })
                    nCol.addView(TextView(appContext).apply {
                        text = latestNotif.text
                        setTextColor(Color.parseColor("#CBD5E1"))
                        textSize = 11.5f
                        maxLines = 1
                        ellipsize = TextUtils.TruncateAt.END
                    })
                    notifRow.addView(nCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    notifRow.addView(TextView(appContext).apply {
                        text = "Buka"
                        setTextColor(Color.parseColor("#38BDF8"))
                        textSize = 11.5f
                        setTypeface(null, Typeface.BOLD)
                        setPadding((8 * density).toInt(), 0, (4 * density).toInt(), 0)
                    })
                    root.addView(notifRow)
                }

                // 4. Quick Action Row (Flashlight & Control Center)
                val actionsRow = LinearLayout(appContext).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = (9 * density).toInt() }
                }
                val torchBtn = actionPillButton(
                    appContext,
                    density,
                    label = if (torchEnabled) "Senter ON" else "Senter",
                    active = torchEnabled
                ) { btn ->
                    val next = toggleTorchDirect(appContext)
                    btn.text = if (next) "Senter ON" else "Senter"
                    btn.background = roundedBox(
                        density,
                        16f,
                        if (next) Color.argb(72, 245, 158, 11) else Color.argb(28, 255, 255, 255),
                        if (next) Color.argb(170, 245, 158, 11) else Color.argb(50, 255, 255, 255)
                    )
                }
                val panelBtn = actionPillButton(appContext, density, label = "Pusat Kontrol", active = true) {
                    dismissExpandedIsland(appContext, reattachPill = false)
                    launchQuickPanelOverlay(appContext)
                }
                actionsRow.addView(
                    torchBtn,
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = (6 * density).toInt()
                    }
                )
                actionsRow.addView(
                    panelBtn,
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = (6 * density).toInt()
                    }
                )
                root.addView(actionsRow)
            }

            wm.addView(root, params)
            expandedIslandView = root

            root.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(220L)
                .start()

            val autoHideMs = if (incomingNotif != null) 4_800L else 9_000L
            val hideTask = Runnable { dismissExpandedIsland(appContext, reattachPill = true) }
            dismissExpandedRunnable = hideTask
            mainHandler.postDelayed(hideTask, autoHideMs)
        }
    }

    fun dismissSystemWideNotifIsland(context: Context) {
        dismissExpandedIsland(context, reattachPill = true)
    }

    private fun dismissExpandedIsland(context: Context, reattachPill: Boolean = true) {
        dismissExpandedRunnable?.let { mainHandler.removeCallbacks(it) }
        dismissExpandedRunnable = null
        val view = expandedIslandView
        if (view != null) {
            runCatching {
                val wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                wm?.removeViewImmediate(view)
            }
            expandedIslandView = null
        }
        if (reattachPill) {
            val appContext = context.applicationContext
            if (!isVitroInForeground() && isScreenInteractive && Settings.canDrawOverlays(appContext) &&
                (lastConfig.showSystemWideDynamicIsland || lastConfig.dynamicIsland.enabled)
            ) {
                attachPersistentStatusIsland(appContext)
                updatePersistentStatusTexts(appContext)
            }
        }
    }

    private fun mediaKeyButton(context: Context, density: Float, glyph: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            text = glyph
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            val sz = (34 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(sz, sz).apply { marginStart = (4 * density).toInt() }
            background = roundedBox(density, 17f, Color.argb(38, 255, 255, 255), Color.argb(55, 255, 255, 255))
            setOnClickListener { onClick() }
        }
    }

    private fun actionPillButton(
        context: Context,
        density: Float,
        label: String,
        active: Boolean,
        onClick: (TextView) -> Unit
    ): TextView {
        return TextView(context).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            val padV = (9 * density).toInt()
            setPadding(0, padV, 0, padV)
            background = roundedBox(
                density,
                16f,
                if (active) Color.argb(60, 56, 189, 248) else Color.argb(28, 255, 255, 255),
                if (active) Color.argb(150, 56, 189, 248) else Color.argb(50, 255, 255, 255)
            )
            setOnClickListener { onClick(this) }
        }
    }

    private fun roundedBox(density: Float, radiusDp: Float, fillColor: Int, strokeColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp * density
            setColor(fillColor)
            setStroke((1f * density).toInt(), strokeColor)
        }
    }

    private fun dispatchMediaKey(context: Context, keyCode: Int) {
        runCatching {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val t = SystemClock.uptimeMillis()
            audio.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, keyCode, 0))
            audio.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, keyCode, 0))
        }
    }

    private fun toggleTorchDirect(context: Context): Boolean {
        val next = !torchEnabled
        runCatching {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return torchEnabled
            val camId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (camId != null) {
                cm.setTorchMode(camId, next)
                torchEnabled = next
            }
        }
        return torchEnabled
    }

    private fun readQuickBattery(context: Context): Pair<Int, Boolean> {
        val intent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 88) ?: 88
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.takeIf { it > 0 } ?: 100
        val pct = ((level * 100f) / scale).toInt().coerceIn(0, 100)
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return pct to charging
    }

    private fun readQuickNetworkGlyph(context: Context): String {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "5G"
                else -> "Off"
            }
        }.getOrDefault("Wi-Fi")
    }

    private fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
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

    // =========================================================================
    // SYSTEM-WIDE DUAL BLACKBERRY LED OVERLAY (ACTIVE OVER ALL OTHER APPS)
    // =========================================================================

    private fun readQuickTelemetry(context: Context): DeviceTelemetry {
        val (pct, charging) = readQuickBattery(context)
        val netGlyph = readQuickNetworkGlyph(context)
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val ringer = runCatching { audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL }
            .getOrDefault(AudioManager.RINGER_MODE_NORMAL)
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return DeviceTelemetry(
            batteryPercent = pct,
            isCharging = charging,
            isWifiConnected = netGlyph == "Wi-Fi",
            isCellularConnected = netGlyph == "5G",
            networkLabel = netGlyph,
            ringerMode = ringer,
            isDndEnabled = ringer == AudioManager.RINGER_MODE_SILENT,
            isPowerSaveMode = pm?.isPowerSaveMode ?: false
        )
    }

    private fun syncSystemLedOverlay(context: Context) {
        val appContext = context.applicationContext
        val ledCfg = lastConfig.ledConfig
        val now = System.currentTimeMillis()
        val telemetry = readQuickTelemetry(appContext)

        val sig = "${telemetry.isCharging}|${telemetry.batteryPercent <= 20}|${telemetry.batteryPercent >= 98}|" +
            "${telemetry.ringerMode}|${telemetry.networkLabel}|${ledCfg.indicatorTrigger}"
        if (sig != systemIndicatorCondSig) {
            systemIndicatorCondSig = sig
            systemIndicatorCondStart = now
        }

        val led1 = BlackBerryLedEvaluator.evaluateAppLed(
            config = ledCfg,
            notifications = NotificationHub.notifications.value,
            nowMillis = now,
            manualTestStartMillis = manualTestLed1Start
        )
        val led2 = BlackBerryLedEvaluator.evaluateIndicatorLed(
            config = ledCfg,
            telemetry = telemetry,
            nowMillis = now,
            conditionStartMillis = systemIndicatorCondStart,
            manualTestStartMillis = manualTestLed2Start
        )

        if (!led1.isActive && !led2.isActive) {
            detachSystemLedOverlay(appContext)
            return
        }

        val density = appContext.resources.displayMetrics.density
        val ledView = systemLedView ?: attachSystemLedOverlay(appContext, density) ?: return
        ledView.updateStates(led1, led2, ledCfg.placement, ledCfg.spacingDp, now)
        updateSystemLedWindowPosition(appContext, ledView, density)

        // Schedule lightweight blink tick ONLY when at least one active LED uses a blink pattern (or duration expiry)
        ledTickRunnable?.let { mainHandler.removeCallbacks(it) }
        val needsFastTick = (led1.isActive && led1.pattern != LedBlinkPattern.SOLID) ||
            (led2.isActive && led2.pattern != LedBlinkPattern.SOLID)
        val needsExpiryTick = (led1.isActive && ledCfg.appLed.durationSeconds > 0) ||
            (led2.isActive && ledCfg.indicatorLed.durationSeconds > 0)

        if (needsFastTick || needsExpiryTick) {
            val tickMs = if (needsFastTick) {
                if (telemetry.isPowerSaveMode) 95L else 45L
            } else {
                1000L
            }
            val nextTick = Runnable {
                if (!isVitroInForeground() && isScreenInteractive && systemLedView != null) {
                    syncSystemLedOverlay(appContext)
                }
            }
            ledTickRunnable = nextTick
            mainHandler.postDelayed(nextTick, tickMs)
        }
    }

    private fun attachSystemLedOverlay(context: Context, density: Float): SystemDualLedView? {
        return runCatching {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return null
            val view = SystemDualLedView(context).apply {
                setOnClickListener {
                    val notif = latestAppNotification
                    if (notif != null) {
                        NotificationHub.openNotification(context.applicationContext, notif)
                    }
                }
            }
            val params = buildSystemLedLayoutParams(context, density)
            wm.addView(view, params)
            systemLedView = view
            view
        }.getOrNull()
    }

    private fun updateSystemLedWindowPosition(context: Context, view: SystemDualLedView, density: Float) {
        runCatching {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            wm.updateViewLayout(view, buildSystemLedLayoutParams(context, density))
        }
    }

    private fun buildSystemLedLayoutParams(context: Context, density: Float): WindowManager.LayoutParams {
        val ledCfg = lastConfig.ledConfig
        val islandCfg = lastConfig.dynamicIsland
        val maxSizeDp = maxOf(ledCfg.appLed.sizeDp, ledCfg.indicatorLed.sizeDp).coerceIn(6f, 18f)
        val haloBoxDp = (maxSizeDp * 2.6f).coerceAtLeast(24f)

        val widthPx: Int
        val heightPx = (haloBoxDp * density).toInt()
        val gravity: Int
        val xPos: Int
        val yPos: Int

        if (ledCfg.placement == LedPlacement.BESIDE_DYNAMIC_ISLAND) {
            val totalWidthDp = islandCfg.widthDp.coerceIn(148f, 230f) + (ledCfg.spacingDp * 2f) + (haloBoxDp * 2f)
            widthPx = (totalWidthDp * density).toInt()
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            xPos = (islandCfg.horizontalOffsetDp * density).toInt()
            yPos = ((islandCfg.topOffsetDp + (islandCfg.heightDp / 2f) - (haloBoxDp / 2f)).coerceAtLeast(2f) * density).toInt()
        } else {
            val totalWidthDp = (haloBoxDp * 2f) + ledCfg.spacingDp
            widthPx = (totalWidthDp * density).toInt()
            gravity = when (ledCfg.placement) {
                LedPlacement.TOP_LEFT_PAIR -> Gravity.TOP or Gravity.START
                LedPlacement.TOP_CENTER_PAIR -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                else -> Gravity.TOP or Gravity.END
            }
            xPos = if (ledCfg.placement == LedPlacement.TOP_CENTER_PAIR) 0 else (ledCfg.horizontalOffsetDp * density).toInt()
            yPos = (ledCfg.topOffsetDp.coerceIn(2f, 80f) * density).toInt()
        }

        return WindowManager.LayoutParams(
            widthPx,
            heightPx,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            this.x = xPos
            this.y = yPos
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun detachSystemLedOverlay(context: Context) {
        ledTickRunnable?.let { mainHandler.removeCallbacks(it) }
        ledTickRunnable = null
        val view = systemLedView ?: return
        runCatching {
            val wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            wm?.removeViewImmediate(view)
        }
        systemLedView = null
    }

    /**
     * Ultra-lightweight native Android View rendering 1 or 2 glowing BlackBerry LED diodes
     * with radial bloom + crisp specular lens pinpoint over any third-party app.
     */
    private class SystemDualLedView(context: Context) : View(context) {
        private val density = context.resources.displayMetrics.density
        private val housingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val specPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

        private var led1: ResolvedLedState? = null
        private var led2: ResolvedLedState? = null
        private var placement: LedPlacement = LedPlacement.TOP_RIGHT_PAIR
        private var spacingDp: Float = 10f
        private var nowMs: Long = 0L
        var latestAppNotification: LiveNotification? = null
            private set

        fun updateStates(
            s1: ResolvedLedState,
            s2: ResolvedLedState,
            place: LedPlacement,
            spacing: Float,
            now: Long
        ) {
            led1 = s1
            led2 = s2
            placement = place
            spacingDp = spacing
            nowMs = now
            latestAppNotification = s1.triggerNotification
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val s1 = led1
            val s2 = led2
            val cy = height / 2f

            if (placement == LedPlacement.BESIDE_DYNAMIC_ISLAND) {
                if (s1?.isActive == true) {
                    val cx = (s1.sizeDp * 1.3f * density).coerceAtLeast(12f * density)
                    drawDiode(canvas, cx, cy, s1)
                }
                if (s2?.isActive == true) {
                    val cx = width - (s2.sizeDp * 1.3f * density).coerceAtLeast(12f * density)
                    drawDiode(canvas, cx, cy, s2)
                }
            } else {
                val gapPx = spacingDp * density
                val cx1 = width * 0.5f - (gapPx * 0.5f) - (7f * density)
                val cx2 = width * 0.5f + (gapPx * 0.5f) + (7f * density)
                if (s1?.isActive == true && s2?.isActive == true) {
                    drawDiode(canvas, cx1, cy, s1)
                    drawDiode(canvas, cx2, cy, s2)
                } else if (s1?.isActive == true) {
                    drawDiode(canvas, width / 2f, cy, s1)
                } else if (s2?.isActive == true) {
                    drawDiode(canvas, width / 2f, cy, s2)
                }
            }
        }

        private fun drawDiode(canvas: Canvas, cx: Float, cy: Float, state: ResolvedLedState) {
            val intensity = BlackBerryLedEvaluator.computeIntensity(state.pattern, state.blinkIntervalMs, nowMs)
            if (intensity <= 0.015f) return

            val argb = state.colorHex.toInt()
            val r = Color.red(argb)
            val g = Color.green(argb)
            val b = Color.blue(argb)
            val coreRadius = (state.sizeDp * density / 2f).coerceAtLeast(3f)
            val glow = state.glowIntensity.coerceIn(0.2f, 1f)

            // Recessed bezel ring
            housingPaint.color = Color.argb((170 * intensity).toInt(), 8, 12, 22)
            canvas.drawCircle(cx, cy, coreRadius * 1.24f, housingPaint)

            // Outer radial halo bloom
            val outerRadius = coreRadius * (2.05f + 0.55f * glow)
            bloomPaint.shader = RadialGradient(
                cx,
                cy,
                outerRadius,
                intArrayOf(
                    Color.argb((160 * glow * intensity).toInt().coerceIn(0, 255), r, g, b),
                    Color.argb((55 * glow * intensity).toInt().coerceIn(0, 255), r, g, b),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, outerRadius, bloomPaint)

            // Vivid luminous diode core
            corePaint.color = Color.argb((250 * intensity).toInt().coerceIn(0, 255), r, g, b)
            canvas.drawCircle(cx, cy, coreRadius, corePaint)

            // Specular white highlight dot
            specPaint.alpha = (235 * intensity).toInt().coerceIn(0, 255)
            canvas.drawCircle(
                cx - coreRadius * 0.28f,
                cy - coreRadius * 0.28f,
                (coreRadius * 0.28f).coerceAtLeast(1.2f),
                specPaint
            )
        }
    }
}
