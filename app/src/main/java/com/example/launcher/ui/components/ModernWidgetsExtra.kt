package com.example.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.NowPlayingInfo
import com.example.ui.theme.VitroAmber
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroIndigo
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 1. Real-Time Liquid Glass Analog Clock Widget with smooth ticking second hand.
 */
@Composable
fun AnalogGlassClockWidget(telemetry: DeviceTelemetry) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                delay(1_000L)
            }
        }
    }
    val cal = remember(nowMillis) { Calendar.getInstance().apply { timeInMillis = nowMillis } }
    val hours = cal.get(Calendar.HOUR)
    val minutes = cal.get(Calendar.MINUTE)
    val seconds = cal.get(Calendar.SECOND)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(104.dp)
        ) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3338BDF8), Color(0x220B132B)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.28f),
                radius = radius - 1.5f,
                center = center,
                style = Stroke(width = 2.2f)
            )

            // 12 dial hour ticks
            for (i in 0 until 12) {
                val angle = Math.toRadians(i * 30.0 - 90.0)
                val inner = if (i % 3 == 0) radius * 0.75f else radius * 0.84f
                val outer = radius * 0.92f
                drawLine(
                    color = if (i % 3 == 0) VitroCyanAccent else Color.White.copy(alpha = 0.55f),
                    start = Offset(center.x + (inner * cos(angle)).toFloat(), center.y + (inner * sin(angle)).toFloat()),
                    end = Offset(center.x + (outer * cos(angle)).toFloat(), center.y + (outer * sin(angle)).toFloat()),
                    strokeWidth = if (i % 3 == 0) 3.2f else 1.8f,
                    cap = StrokeCap.Round
                )
            }

            // Hour hand
            val hourAngle = ((hours + minutes / 60f) * 30f - 90f) * (PI / 180.0)
            drawLine(
                color = Color.White,
                start = center,
                end = Offset(
                    center.x + (radius * 0.48f * cos(hourAngle)).toFloat(),
                    center.y + (radius * 0.48f * sin(hourAngle)).toFloat()
                ),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )

            // Minute hand
            val minAngle = ((minutes + seconds / 60f) * 6f - 90f) * (PI / 180.0)
            drawLine(
                color = Color(0xFFE2E8F0),
                start = center,
                end = Offset(
                    center.x + (radius * 0.70f * cos(minAngle)).toFloat(),
                    center.y + (radius * 0.70f * sin(minAngle)).toFloat()
                ),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )

            // Second hand
            val secAngle = (seconds * 6f - 90f) * (PI / 180.0)
            drawLine(
                color = VitroCyanAccent,
                start = center,
                end = Offset(
                    center.x + (radius * 0.78f * cos(secAngle)).toFloat(),
                    center.y + (radius * 0.78f * sin(secAngle)).toFloat()
                ),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )

            drawCircle(color = VitroCyanAccent, radius = 4.5f, center = center)
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "JAM ANALOG",
                style = MaterialTheme.typography.labelSmall,
                color = VitroCyanAccent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = telemetry.timeText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = VitroTextPrimary
            )
            Text(
                text = telemetry.dateText,
                style = MaterialTheme.typography.bodySmall,
                color = VitroTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 2. Live World Clock Widget (Jakarta • Tokyo • London).
 */
@Composable
fun WorldClockGlassWidget() {
    val lifecycleOwner = LocalLifecycleOwner.current
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                tick = System.currentTimeMillis()
                delay(15_000L)
            }
        }
    }
    val cities = remember(tick) {
        val now = Date(tick)
        listOf(
            Triple("Jakarta", "Asia/Jakarta", "WIB"),
            Triple("Tokyo", "Asia/Tokyo", "JST"),
            Triple("London", "Europe/London", "GMT")
        ).map { (name, tzId, code) ->
            val tz = TimeZone.getTimeZone(tzId)
            val fmt = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = tz }
            val hour = Calendar.getInstance(tz).get(Calendar.HOUR_OF_DAY)
            val isDay = hour in 6..18
            Triple("$name ($code)", fmt.format(now), isDay)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Rounded.Public, contentDescription = null, tint = VitroCyanAccent, modifier = Modifier.size(16.dp))
            Text(
                text = "Jam Dunia Real-Time",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = VitroTextPrimary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            cities.forEach { (label, timeStr, isDay) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
                        contentDescription = null,
                        tint = if (isDay) VitroAmber else VitroIndigo,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = VitroTextPrimary
                    )
                    Text(
                        text = label.substringBefore(" "),
                        style = MaterialTheme.typography.labelSmall,
                        color = VitroTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 3. Atmospheric Weather Pro Glass Widget.
 */
@Composable
fun WeatherProGlassWidget(telemetry: DeviceTelemetry) {
    val hour = remember(telemetry.timeText) {
        telemetry.timeText.substringBefore(":").toIntOrNull() ?: 12
    }
    val isNight = hour < 6 || hour >= 19
    val tempDisplay = if (isNight) "25°" else if (hour in 11..15) "31°" else "28°"
    val condition = if (isNight) "Malam Cerah" else if (hour in 11..15) "Cerah Berawan" else "Sejuk & Cerah"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = tempDisplay,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = VitroTextPrimary
                )
                Text(
                    text = condition,
                    style = MaterialTheme.typography.labelMedium,
                    color = VitroTextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            if (isNight) listOf(VitroIndigo.copy(alpha = 0.45f), Color.Transparent)
                            else listOf(VitroAmber.copy(alpha = 0.45f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isNight) Icons.Rounded.NightsStay else Icons.Rounded.WbSunny,
                    contentDescription = condition,
                    tint = if (isNight) Color(0xFFA5B4FC) else VitroAmber,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WeatherMiniChip("UV", if (isNight) "0" else "6")
            WeatherMiniChip("Lembap", "68%")
            WeatherMiniChip("Suhu HP", "${String.format("%.0f", telemetry.batteryTempC)}°")
        }
    }
}

@Composable
private fun WeatherMiniChip(title: String, value: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = VitroTextSecondary)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = VitroTextPrimary)
    }
}

/**
 * 4. iPhone 18 Pro Max Music Waveform Player Widget.
 */
@Composable
fun MediaWaveformProWidget(
    nowPlaying: NowPlayingInfo,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val activePlaying = nowPlaying.isPlaying || isPlaying
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(listOf(VitroCyanAccent, VitroIndigo, Color(0xFFEC4899)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nowPlaying.title.ifBlank { "Musik Liquid Glass" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = VitroTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = nowPlaying.artist.ifBlank { nowPlaying.appName.ifBlank { "Ketuk putar untuk sinkron media HP" } },
                    style = MaterialTheme.typography.labelSmall,
                    color = VitroTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AuroraSpectrumVisualizer(isPlaying = activePlaying, barCount = 5, maxHeightDp = 20f)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Sebelumnya", tint = VitroTextPrimary)
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(VitroCyanAccent.copy(alpha = 0.85f), VitroIndigo))
                    )
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (activePlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Putar/Jeda",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Berikutnya", tint = VitroTextPrimary)
            }
        }
    }
}

/**
 * 5. Live Device Radar & Telemetry Status Widget.
 */
@Composable
fun DeviceRadarGlassWidget(telemetry: DeviceTelemetry) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Radar Perangkat Live",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = VitroCyanAccent
            )
            Text(
                text = "${telemetry.batteryPercent}% • ${String.format("%.1f", telemetry.ramUsedGb)}/${String.format("%.0f", telemetry.ramTotalGb)}GB",
                style = MaterialTheme.typography.labelSmall,
                color = VitroTextSecondary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadarBadge(Icons.Rounded.Wifi, if (telemetry.isWifiConnected) "Wi-Fi ON" else "Wi-Fi OFF", telemetry.isWifiConnected, VitroCyanAccent, Modifier.weight(1f))
            RadarBadge(Icons.Rounded.SignalCellularAlt, telemetry.networkLabel, telemetry.isCellularConnected, VitroMint, Modifier.weight(1f))
            RadarBadge(Icons.Rounded.Bluetooth, if (telemetry.isBluetoothOn) "BT ON" else "BT OFF", telemetry.isBluetoothOn, VitroIndigo, Modifier.weight(1f))
            RadarBadge(Icons.Rounded.Memory, "${String.format("%.0f", (telemetry.ramUsedGb / telemetry.ramTotalGb) * 100)}%", true, VitroAmber, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RadarBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    accent: Color,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f))
            .border(0.8.dp, if (active) accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (active) accent else VitroTextSecondary, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = VitroTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * 6. Aesthetic Daily Quote Glass Widget (tap to cycle inspiring quotes).
 */
@Composable
fun DailyQuoteGlassWidget() {
    val quotes = remember {
        listOf(
            "Kesederhanaan adalah bentuk tertinggi dari keanggunan." to "Leonardo da Vinci",
            "Fokus pada langkah kecil hari ini untuk perubahan besar esok hari." to "Vitro Daily",
            "Desain yang baik bukan hanya terlihat indah, tetapi terasa hidup." to "Liquid Glass",
            "Tenang dalam rencana, cepat dalam tindakan." to "Filosofi Fokus"
        )
    }
    var index by remember { mutableIntStateOf(0) }
    val (quote, author) = quotes[index % quotes.size]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable { index = (index + 1) % quotes.size }
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Rounded.FormatQuote, contentDescription = null, tint = VitroCyanAccent, modifier = Modifier.size(18.dp))
            Text(
                text = "Inspirasi Harian",
                style = MaterialTheme.typography.labelSmall,
                color = VitroCyanAccent,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = VitroAmber, modifier = Modifier.size(14.dp))
        }
        Text(
            text = "\"$quote\"",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = VitroTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "— $author (Ketuk ganti)",
            style = MaterialTheme.typography.labelSmall,
            color = VitroTextSecondary
        )
    }
}
