package com.example.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import com.example.launcher.model.DeviceTelemetry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemMonitor(private val context: Context) {

    private val indonesianLocale = Locale("id", "ID")
    private val timeFormat = SimpleDateFormat("HH:mm", indonesianLocale)
    private val dateFormat = SimpleDateFormat("EEEE, d MMM", indonesianLocale)
    private val monthFormat = SimpleDateFormat("MMMM yyyy", indonesianLocale)
    private val dayNumFormat = SimpleDateFormat("d", indonesianLocale)

    private val localControlState = MutableStateFlow(
        LocalControlOverrides(
            brightness = readInitialBrightness(),
            volume = readCurrentVolume(),
            flashlight = false,
            dnd = false,
            autoRotate = readAutoRotate(),
            bluetoothVisual = true
        )
    )

    private data class LocalControlOverrides(
        val brightness: Float,
        val volume: Float,
        val flashlight: Boolean,
        val dnd: Boolean,
        val autoRotate: Boolean,
        val bluetoothVisual: Boolean
    )

    fun observeTelemetry(): Flow<DeviceTelemetry> {
        val ticker = flow {
            while (true) {
                emit(readSystemSnapshot())
                delay(4_000L)
            }
        }
        return combine(ticker, localControlState) { snapshot, controls ->
            snapshot.copy(
                brightnessFraction = controls.brightness,
                volumeFraction = controls.volume,
                isFlashlightOn = controls.flashlight,
                isDndEnabled = controls.dnd,
                isAutoRotateEnabled = controls.autoRotate,
                isBluetoothVisualOn = controls.bluetoothVisual
            )
        }
    }

    private fun readSystemSnapshot(): DeviceTelemetry {
        val now = Date()
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 88) ?: 88
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.takeIf { it > 0 } ?: 100
        val batteryPct = ((level * 100f) / scale).toInt().coerceIn(0, 100)
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 315) ?: 315

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        var wifiConnected = false
        var cellularConnected = false
        if (cm != null) {
            val activeNetwork = cm.activeNetwork
            val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
            if (caps != null) {
                wifiConnected = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                cellularConnected = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            }
        }

        val currentVol = readCurrentVolume()
        if (kotlin.math.abs(currentVol - localControlState.value.volume) > 0.08f) {
            localControlState.value = localControlState.value.copy(volume = currentVol)
        }

        return DeviceTelemetry(
            timeText = timeFormat.format(now),
            dateText = dateFormat.format(now).replaceFirstChar { it.uppercase(indonesianLocale) },
            monthText = monthFormat.format(now).replaceFirstChar { it.uppercase(indonesianLocale) },
            dayNumber = dayNumFormat.format(now),
            batteryPercent = batteryPct,
            isCharging = isCharging,
            batteryTempC = tempTenths / 10f,
            isWifiConnected = wifiConnected,
            isCellularConnected = cellularConnected,
            signalBars = if (cellularConnected || wifiConnected) 4 else 2,
            volumeFraction = currentVol,
            brightnessFraction = localControlState.value.brightness
        )
    }

    fun setMediaVolume(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        localControlState.value = localControlState.value.copy(volume = clamped)
        runCatching {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val target = (clamped * max).toInt().coerceIn(0, max)
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        }
    }

    fun setBrightnessFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0.08f, 1f)
        localControlState.value = localControlState.value.copy(brightness = clamped)
    }

    fun toggleFlashlight(): Boolean {
        val next = !localControlState.value.flashlight
        runCatching {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val flashCameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (flashCameraId != null) {
                cameraManager.setTorchMode(flashCameraId, next)
            }
        }
        localControlState.value = localControlState.value.copy(flashlight = next)
        return next
    }

    fun toggleDnd(): Boolean {
        val next = !localControlState.value.dnd
        localControlState.value = localControlState.value.copy(dnd = next)
        return next
    }

    fun toggleAutoRotate(): Boolean {
        val next = !localControlState.value.autoRotate
        localControlState.value = localControlState.value.copy(autoRotate = next)
        return next
    }

    fun toggleBluetoothVisual(): Boolean {
        val next = !localControlState.value.bluetoothVisual
        localControlState.value = localControlState.value.copy(bluetoothVisual = next)
        return next
    }

    fun openSystemSettings(action: String) {
        runCatching {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            runCatching {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }
        }
    }

    private fun readCurrentVolume(): Float {
        return runCatching {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 0.65f
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
        }.getOrDefault(0.65f)
    }

    private fun readInitialBrightness(): Float {
        return runCatching {
            val raw = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 180)
            (raw / 255f).coerceIn(0.1f, 1f)
        }.getOrDefault(0.72f)
    }

    private fun readAutoRotate(): Boolean {
        return runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1
        }.getOrDefault(true)
    }
}
