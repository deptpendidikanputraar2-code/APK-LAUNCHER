package com.example.launcher.system

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.launcher.model.LiveNotification
import com.example.launcher.model.NowPlayingInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
        val pending = notification.contentIntent
        if (pending != null) {
            val sent = runCatching {
                pending.send()
                true
            }.getOrDefault(false)
            if (sent) return true
        }
        return runCatching {
            val launch = context.packageManager.getLaunchIntentForPackage(notification.packageName)
            if (launch != null) {
                launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                true
            } else false
        }.getOrDefault(false)
    }

    fun requestRefresh() {
        serviceRef?.get()?.syncAllActiveNotifications()
    }
}

class VitroNotificationListenerService : NotificationListenerService() {

    private val iconCache = mutableMapOf<String, ImageBitmap?>()

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
