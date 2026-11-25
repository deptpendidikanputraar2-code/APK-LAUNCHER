package com.example.launcher.system

import android.app.Activity
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.launcher.data.LauncherPreferencesRepository
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.LiveNotification
import com.example.launcher.model.NowPlayingInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

object NotificationHub {
    private val _notifications = MutableStateFlow<List<LiveNotification>>(emptyList())
    val notifications: StateFlow<List<LiveNotification>> = _notifications.asStateFlow()

    private val _incomingEvents = MutableSharedFlow<LiveNotification>(extraBufferCapacity = 16)
    val incomingEvents: SharedFlow<LiveNotification> = _incomingEvents.asSharedFlow()

    private val _nowPlaying = MutableStateFlow(NowPlayingInfo())
    val nowPlaying: StateFlow<NowPlayingInfo> = _nowPlaying.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var serviceRef: WeakReference<VitroNotificationListenerService>? = null

    internal fun attachService(service: VitroNotificationListenerService) {
        serviceRef = WeakReference(service)
        _isConnected.value = true
    }

    internal fun detachService() {
        serviceRef = null
        _isConnected.value = false
    }

    internal fun updateNotifications(list: List<LiveNotification>, nowPlaying: NowPlayingInfo) {
        _notifications.value = list
        _nowPlaying.value = nowPlaying
    }

    internal fun emitIncoming(notification: LiveNotification) {
        _incomingEvents.tryEmit(notification)
    }

    fun dismissNotification(key: String) {
        runCatching { serviceRef?.get()?.cancelNotification(key) }
        _notifications.value = _notifications.value.filterNot { it.key == key }
    }

    fun clearAllDismissible() {
        val current = _notifications.value
        current.filterNot { it.isOngoing }.forEach { notif ->
            runCatching { serviceRef?.get()?.cancelNotification(notif.key) }
        }
        _notifications.value = current.filter { it.isOngoing }
    }

    fun openNotification(context: Context, notification: LiveNotification): Boolean {
        // Dismiss Android system keyguard cleanly if opening from a Lock Screen Activity
        if (context is Activity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                if (km?.isKeyguardLocked == true) {
                    km.requestDismissKeyguard(context, null)
                }
            }
        }

        // Prefer an Activity context or active NotificationListenerService context for Android 14-16 BAL reliability
        val privilegedContext: Context = if (context is Activity) context else (serviceRef?.get() ?: context)

        // Always look up the freshest PendingIntent in case the notification updated since rendering
        val freshestPending = _notifications.value
            .firstOrNull { it.key == notification.key }
            ?.contentIntent
            ?: notification.contentIntent

        if (freshestPending != null) {
            // Android 14+ (API 34-36) strictly requires explicit BAL ActivityOptions on PendingIntent.send()
            val sentWithOptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                runCatching {
                    val opts = ActivityOptions.makeBasic().apply {
                        if (Build.VERSION.SDK_INT >= 36) {
                            setPendingIntentBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            setPendingIntentBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                            )
                        }
                    }.toBundle()
                    freshestPending.send(privilegedContext, 0, null, null, null, null, opts)
                    true
                }.getOrDefault(false)
            } else {
                false
            }
            if (sentWithOptions) return true

            val sentLegacy = runCatching {
                freshestPending.send()
                true
            }.getOrDefault(false)
            if (sentLegacy) return true
        }

        // Fallback 1: Launch target app directly by packageName
        val launchedApp = runCatching {
            val launch = privilegedContext.packageManager.getLaunchIntentForPackage(notification.packageName)
            if (launch != null) {
                launch.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                )
                privilegedContext.startActivity(launch)
                true
            } else false
        }.getOrDefault(false)
        if (launchedApp) return true

        // Fallback 2: For Android System / SystemUI notifications that have no launcher icon
        if (notification.packageName == "android" || notification.packageName.contains("systemui")) {
            return runCatching {
                privilegedContext.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                true
            }.getOrDefault(false)
        }
        return false
    }

    fun requestRefresh() {
        serviceRef?.get()?.syncAllActiveNotifications()
    }
}

class VitroNotificationListenerService : NotificationListenerService() {

    private val iconCache = mutableMapOf<String, ImageBitmap?>()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Volatile
    private var latestConfig: LauncherConfig = LauncherConfig()
    private var screenReceiverRegistered = false

    private val screenPowerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    VitroEdgeControlManager.onScreenPowerChanged(applicationContext, interactive = true)
                    if (latestConfig.replaceSystemLockScreen) {
                        VitroEdgeControlManager.launchVitroLockScreen(applicationContext)
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    VitroEdgeControlManager.onScreenPowerChanged(applicationContext, interactive = false)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val repo = LauncherPreferencesRepository(applicationContext)
        serviceScope.launch {
            repo.configFlow.collect { cfg ->
                latestConfig = cfg
                VitroEdgeControlManager.syncSystemWideUi(applicationContext, cfg)
            }
        }
        runCatching {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(screenPowerReceiver, filter)
            screenReceiverRegistered = true
        }
    }

    override fun onDestroy() {
        if (screenReceiverRegistered) {
            runCatching { unregisterReceiver(screenPowerReceiver) }
            screenReceiverRegistered = false
        }
        VitroEdgeControlManager.onScreenPowerChanged(applicationContext, interactive = false)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationHub.attachService(this)
        syncAllActiveNotifications()
    }

    override fun onListenerDisconnected() {
        NotificationHub.detachService()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        syncAllActiveNotifications()
        if (sbn.packageName == packageName) return
        val converted = convertSbn(sbn) ?: return
        if (!converted.isOngoing && !converted.isMedia) {
            NotificationHub.emitIncoming(converted)
            val cfg = latestConfig
            if (cfg.replaceSystemNotifications) {
                NotificationSoundEngine.playNotificationSound(
                    context = applicationContext,
                    soundId = cfg.activeNotifSoundId,
                    customSounds = cfg.customNotifSounds
                )
                // Show system-wide Dynamic Island heads-up over OTHER apps (when Vitro isn't already visible)
                if (cfg.showSystemWideDynamicIsland && !VitroEdgeControlManager.isVitroInForeground()) {
                    VitroEdgeControlManager.showSystemWideNotifIsland(applicationContext, converted)
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        syncAllActiveNotifications()
    }

    fun syncAllActiveNotifications() {
        val active = runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())
        val converted = active
            .filter { it.packageName != packageName }
            .mapNotNull { convertSbn(it) }
            .sortedByDescending { it.timestampMillis }

        val mediaItem = converted.firstOrNull { it.isMedia }
        val nowPlaying = if (mediaItem != null) {
            NowPlayingInfo(
                title = mediaItem.title.ifBlank { mediaItem.appName },
                artist = mediaItem.text.ifBlank { mediaItem.appName },
                appName = mediaItem.appName,
                isPlaying = true,
                hasActiveSession = true
            )
        } else {
            NowPlayingInfo()
        }

        val visibleNotifications = converted.filterNot { it.isMedia && it.isOngoing }
        NotificationHub.updateNotifications(visibleNotifications, nowPlaying)
        VitroEdgeControlManager.refreshSystemIslandContent(applicationContext)
    }

    private fun convertSbn(sbn: StatusBarNotification): LiveNotification? {
        val notif = sbn.notification ?: return null
        val extras = notif.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = (extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras?.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras?.getCharSequence(Notification.EXTRA_SUB_TEXT))
            ?.toString()?.trim().orEmpty()

        if (title.isBlank() && text.isBlank()) return null

        val isGroupSummary = (notif.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isGroupSummary) return null

        val isMedia = notif.category == Notification.CATEGORY_TRANSPORT ||
            extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true

        val appName = runCatching {
            val ai = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(ai).toString()
        }.getOrDefault(sbn.packageName.substringAfterLast('.'))

        val appIcon = iconCache.getOrPut(sbn.packageName) {
            runCatching {
                drawableToBitmap(packageManager.getApplicationIcon(sbn.packageName))
            }.getOrNull()
        }

        return LiveNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appName,
            title = title.ifBlank { appName },
            text = text.ifBlank { "Notifikasi baru dari $appName" },
            timestampMillis = sbn.postTime,
            isOngoing = sbn.isOngoing,
            isMedia = isMedia,
            appIcon = appIcon,
            contentIntent = notif.contentIntent
        )
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
