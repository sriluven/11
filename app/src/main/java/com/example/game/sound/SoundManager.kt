package com.example.game.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Handles audio playback via programmatic PCM synthesis (zero asset loading issues)
 * and haptic feedback.
 */
class SoundManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sampleRate = 22050

    var isSoundEnabled: Boolean = true
    var isHapticsEnabled: Boolean = true

    // Pre-synthesized PCM buffers for instantaneous playback without GC stutter
    private val flapBuffer: ShortArray by lazy { generateSweep(350f, 750f, 0.07f, true) }
    private val pointBuffer: ShortArray by lazy { generatePointSound() }
    private val hitBuffer: ShortArray by lazy { generateHitSound() }
    private val dieBuffer: ShortArray by lazy { generateSweep(650f, 180f, 0.22f, false) }
    private val swooshBuffer: ShortArray by lazy { generateSwooshSound() }
    private val medalBuffer: ShortArray by lazy { generateMedalArpeggio() }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun playFlap() {
        if (isSoundEnabled) {
            playRawPcm(flapBuffer)
        }
        if (isHapticsEnabled) {
            vibrate(30, 80)
        }
    }

    fun playPoint() {
        if (isSoundEnabled) {
            playRawPcm(pointBuffer)
        }
        if (isHapticsEnabled) {
            vibrate(45, 140)
        }
    }

    fun playHit() {
        if (isSoundEnabled) {
            playRawPcm(hitBuffer)
        }
        if (isHapticsEnabled) {
            vibrate(100, 255)
        }
    }

    fun playDie() {
        if (isSoundEnabled) {
            playRawPcm(dieBuffer)
        }
    }

    fun playSwoosh() {
        if (isSoundEnabled) {
            playRawPcm(swooshBuffer)
        }
    }

    fun playMedal() {
        if (isSoundEnabled) {
            playRawPcm(medalBuffer)
        }
    }

    private fun playRawPcm(buffer: ShortArray) {
        scope.launch {
            try {
                val minSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(minSize, buffer.size * 2)

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
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
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()

                // Allow audio to finish then release
                val durationMs = (buffer.size.toFloat() / sampleRate * 1000).toLong() + 50
                kotlinx.coroutines.delay(durationMs)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // Ignore audio device access exceptions gracefully
            }
        }
    }

    private fun vibrate(durationMs: Long, amplitude: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val clampedAmp = amplitude.coerceIn(1, 255)
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, clampedAmp))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {
            // Ignore if vibration fails or device lacks hardware
        }
    }

    // Synthesizers
    private fun generateSweep(startFreq: Float, endFreq: Float, durationSec: Float, upward: Boolean): ShortArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val progress = i.toFloat() / totalSamples
            val currentFreq = startFreq + (endFreq - startFreq) * progress
            val phaseInc = 2.0 * PI * currentFreq / sampleRate
            phase += phaseInc

            // Envelope (attack and gentle decay)
            val envelope = if (upward) {
                (1f - progress) * (if (progress < 0.1f) progress / 0.1f else 1f)
            } else {
                (1f - progress * 0.8f)
            }

            val sample = sin(phase) * envelope * 0.65
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generatePointSound(): ShortArray {
        // Two pure retro high bell tones: 587Hz (D5) -> 880Hz (A5)
        val d1 = 0.06f
        val d2 = 0.14f
        val totalSamples = (sampleRate * (d1 + d2)).toInt()
        val buffer = ShortArray(totalSamples)
        val samples1 = (sampleRate * d1).toInt()

        var phase = 0.0
        for (i in 0 until samples1) {
            phase += 2.0 * PI * 587.33 / sampleRate
            val env = 1f - (i.toFloat() / samples1) * 0.3f
            buffer[i] = (sin(phase) * env * 0.6 * Short.MAX_VALUE).toInt().toShort()
        }

        phase = 0.0
        for (i in samples1 until totalSamples) {
            val rel = i - samples1
            phase += 2.0 * PI * 880.0 / sampleRate
            val progress = rel.toFloat() / (totalSamples - samples1)
            val env = (1f - progress)
            buffer[i] = (sin(phase) * env * 0.7 * Short.MAX_VALUE).toInt().toShort()
        }

        return buffer
    }

    private fun generateHitSound(): ShortArray {
        // Crunchy retro arcade hit: mix of square wave, low rumble and noise
        val durationSec = 0.12f
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val progress = i.toFloat() / totalSamples
            val freq = 160f * (1f - progress * 0.5f)
            phase += 2.0 * PI * freq / sampleRate
            val sine = sin(phase)
            val square = if (sine > 0) 1f else -1f
            val noise = (Math.random() * 2.0 - 1.0).toFloat()

            val mixed = (square * 0.4f + noise * 0.6f) * (1f - progress)
            buffer[i] = (mixed * 0.8f * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateSwooshSound(): ShortArray {
        val durationSec = 0.14f
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val progress = i.toFloat() / totalSamples
            val noise = (Math.random() * 2.0 - 1.0).toFloat()
            val envelope = sin(progress * PI.toFloat())
            buffer[i] = (noise * envelope * 0.45f * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateMedalArpeggio(): ShortArray {
        val noteDur = 0.09f
        val totalSamples = (sampleRate * (noteDur * 3)).toInt()
        val buffer = ShortArray(totalSamples)
        val freqs = floatArrayOf(523.25f, 659.25f, 783.99f) // C5, E5, G5

        var idx = 0
        for (f in freqs) {
            val noteSamples = (sampleRate * noteDur).toInt()
            var phase = 0.0
            for (i in 0 until noteSamples) {
                if (idx >= totalSamples) break
                phase += 2.0 * PI * f / sampleRate
                val progress = i.toFloat() / noteSamples
                val env = 1f - progress * 0.5f
                buffer[idx++] = (sin(phase) * env * 0.65 * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return buffer
    }
}
