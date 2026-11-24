package com.example.launcher.system

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import com.example.launcher.model.BuiltInNotifSound
import com.example.launcher.model.CustomNotificationSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Aesthetic studio-quality notification chime synthesizer + custom user audio URI player.
 * Synthesizes warm, glassy polyphonic tones via PCM AudioTrack (zero external assets required)
 * and plays custom user-added audio files via zero-permission Storage Access Framework URIs.
 */
object NotificationSoundEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastPlayedAt: Long = 0L

    fun playNotificationSound(
        context: Context,
        soundId: String,
        customSounds: List<CustomNotificationSound>,
        throttleMs: Long = 650L
    ) {
        val now = System.currentTimeMillis()
        if (throttleMs > 0L && now - lastPlayedAt < throttleMs) return
        lastPlayedAt = now

        if (soundId == BuiltInNotifSound.SILENT.id) return

        val custom = customSounds.firstOrNull { it.id == soundId }
        if (custom != null) {
            playCustomUri(context.applicationContext, custom.uriString)
            return
        }

        val builtIn = BuiltInNotifSound.entries.firstOrNull { it.id == soundId }
            ?: BuiltInNotifSound.CRYSTAL_DEW

        scope.launch {
            playSynthesizedChime(builtIn)
        }
    }

    private fun playCustomUri(context: Context, uriString: String) {
        scope.launch {
            runCatching {
                val uri = Uri.parse(uriString)
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, uri)
                    setOnCompletionListener { mp -> runCatching { mp.release() } }
                    setOnErrorListener { mp, _, _ ->
                        runCatching { mp.release() }
                        true
                    }
                    prepare()
                    start()
                }
                // Guard against very long music tracks used as notification sound (cap at 4.5s)
                kotlinx.coroutines.delay(4_500L)
                runCatching {
                    if (player.isPlaying) player.stop()
                    player.release()
                }
            }.onFailure {
                // Fallback gracefully to Crystal Dew if URI was revoked/removed
                playSynthesizedChime(BuiltInNotifSound.CRYSTAL_DEW)
            }
        }
    }

    private fun playSynthesizedChime(sound: BuiltInNotifSound) {
        if (sound == BuiltInNotifSound.SILENT) return
        val sampleRate = 44100
        val durationSec = when (sound) {
            BuiltInNotifSound.CRYSTAL_DEW -> 0.85
            BuiltInNotifSound.LIQUID_HARP -> 1.05
            BuiltInNotifSound.AURORA_PULSE -> 0.95
            BuiltInNotifSound.ZEN_GLASS_BELL -> 1.15
            BuiltInNotifSound.SILK_MARIMBA -> 0.72
            BuiltInNotifSound.PRISM_DROP -> 0.78
            BuiltInNotifSound.SILENT -> 0.0
        }
        val numSamples = (sampleRate * durationSec).toInt()
        if (numSamples <= 0) return

        val pcm = ShortArray(numSamples)
        data class Note(val freq: Double, val startSec: Double, val decay: Double, val amp: Double, val harmonic: Double)

        val notes: List<Note> = when (sound) {
            BuiltInNotifSound.CRYSTAL_DEW -> listOf(
                Note(1046.50, 0.00, 6.5, 0.42, 0.22), // C6
                Note(1318.51, 0.09, 5.8, 0.38, 0.18), // E6
                Note(1567.98, 0.18, 4.2, 0.44, 0.25)  // G6
            )
            BuiltInNotifSound.LIQUID_HARP -> listOf(
                Note(523.25, 0.00, 4.5, 0.32, 0.20),  // C5
                Note(659.25, 0.07, 4.5, 0.32, 0.20),  // E5
                Note(783.99, 0.14, 4.2, 0.34, 0.22),  // G5
                Note(987.77, 0.21, 3.8, 0.34, 0.20),  // B5
                Note(1174.66, 0.29, 3.2, 0.38, 0.18)  // D6
            )
            BuiltInNotifSound.AURORA_PULSE -> listOf(
                Note(440.00, 0.00, 3.2, 0.30, 0.30),  // A4
                Note(554.37, 0.02, 3.0, 0.28, 0.26),  // C#5
                Note(659.25, 0.16, 2.8, 0.36, 0.24),  // E5
                Note(880.00, 0.20, 2.6, 0.34, 0.22)   // A5
            )
            BuiltInNotifSound.ZEN_GLASS_BELL -> listOf(
                Note(880.00, 0.00, 2.4, 0.46, 0.15),  // A5 pure bell
                Note(1760.0, 0.00, 4.8, 0.20, 0.10),  // A6 shimmer
                Note(1318.51, 0.14, 2.6, 0.34, 0.14)  // E6 tail
            )
            BuiltInNotifSound.SILK_MARIMBA -> listOf(
                Note(587.33, 0.00, 9.5, 0.48, 0.12),  // D5
                Note(880.00, 0.11, 8.5, 0.46, 0.12),  // A5
                Note(1174.66, 0.22, 7.2, 0.40, 0.10)  // D6
            )
            BuiltInNotifSound.PRISM_DROP -> listOf(
                Note(740.00, 0.00, 7.5, 0.38, 0.18),  // F#5
                Note(1479.98, 0.12, 5.2, 0.42, 0.24)  // F#6
            )
            BuiltInNotifSound.SILENT -> emptyList()
        }

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var sample = 0.0
            for (n in notes) {
                val dt = t - n.startSec
                if (dt >= 0.0) {
                    val attack = (dt / 0.012).coerceAtMost(1.0)
                    val env = attack * exp(-n.decay * dt)
                    val fundamental = sin(2.0 * PI * n.freq * dt)
                    val overtone = sin(2.0 * PI * (n.freq * 2.0) * dt) * n.harmonic
                    val sparkle = sin(2.0 * PI * (n.freq * 3.0) * dt) * (n.harmonic * 0.35)
                    sample += (fundamental + overtone + sparkle) * env * n.amp
                }
            }
            // Gentle soft clip to avoid any distortion
            val clamped = sample.coerceIn(-0.92, 0.92)
            pcm[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
        }

        runCatching {
            val bufferSize = pcm.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bufferSize)
                .build()

            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep((durationSec * 1000).toLong() + 120L)
            track.stop()
            track.release()
        }
    }
}
