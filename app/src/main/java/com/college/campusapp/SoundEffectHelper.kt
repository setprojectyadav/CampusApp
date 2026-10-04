package com.college.campusapp

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.college.campusapp.security.EncryptionManager
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Professional synthetic audio feedback helper for CampusApp task executions.
 * Synthesizes harmonic micro-chimes on-the-fly with zero file dependencies.
 */
object SoundEffectHelper {

    private val audioExecutor = Executors.newSingleThreadExecutor()
    private const val SAMPLE_RATE = 44100

    enum class SoundType {
        SUCCESS,          // Upward double-chime for orders, payments, deposits
        PAYMENT_SUCCESS,  // 3-tone melodic confirmation for UPI/Wallet transactions
        CLICK,            // Subtle soft micro-pop
        ACTION_COMPLETE,  // Bright chime for pool joined, transfer created, meal joined
        REFUND_OR_UNLOCK, // Ascending triad chime for escrow unlocks and refunds
        ALERT             // Gentle soft notification
    }

    fun playSound(context: Context, type: SoundType) {
        try {
            // Check if sound effects are enabled in user settings (defaults to true)
            val isSoundEnabled = EncryptionManager.getBoolean(context, "sound_effects_enabled", true)
            if (!isSoundEnabled) return

            audioExecutor.execute {
                try {
                    val pcmData = generateSoundPcm(type)
                    playPcm(pcmData)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSuccess(context: Context) = playSound(context, SoundType.SUCCESS)
    fun playPaymentSuccess(context: Context) = playSound(context, SoundType.PAYMENT_SUCCESS)
    fun playClick(context: Context) = playSound(context, SoundType.CLICK)
    fun playActionComplete(context: Context) = playSound(context, SoundType.ACTION_COMPLETE)
    fun playRefundOrUnlock(context: Context) = playSound(context, SoundType.REFUND_OR_UNLOCK)
    fun playAlert(context: Context) = playSound(context, SoundType.ALERT)

    private fun generateSoundPcm(type: SoundType): ShortArray {
        return when (type) {
            SoundType.SUCCESS -> {
                // 160ms harmonic upward chime (659Hz [E5] -> 1046Hz [C6])
                val durationSec = 0.16
                val numSamples = (SAMPLE_RATE * durationSec).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val freq = if (t < 0.07) 659.25 else 1046.50
                    val envelope = Math.exp(-12.0 * (if (t < 0.07) t else t - 0.07))
                    val wave = sin(2.0 * PI * freq * t) + 0.3 * sin(4.0 * PI * freq * t)
                    buffer[i] = (wave * envelope * 16000.0).toInt().coerceIn(-32768, 32767).toShort()
                }
                buffer
            }
            SoundType.PAYMENT_SUCCESS -> {
                // 240ms 3-tone melodic confirmation chime (587Hz [D5] -> 880Hz [A5] -> 1174Hz [D6])
                val durationSec = 0.24
                val numSamples = (SAMPLE_RATE * durationSec).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val (freq, noteT) = when {
                        t < 0.07 -> Pair(587.33, t)
                        t < 0.14 -> Pair(880.00, t - 0.07)
                        else -> Pair(1174.66, t - 0.14)
                    }
                    val envelope = Math.exp(-10.0 * noteT)
                    val wave = sin(2.0 * PI * freq * t) + 0.25 * sin(4.0 * PI * freq * t)
                    buffer[i] = (wave * envelope * 18000.0).toInt().coerceIn(-32768, 32767).toShort()
                }
                buffer
            }
            SoundType.CLICK -> {
                // 20ms subtle soft pop (1200Hz)
                val durationSec = 0.02
                val numSamples = (SAMPLE_RATE * durationSec).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val envelope = Math.exp(-100.0 * t)
                    val wave = sin(2.0 * PI * 1200.0 * t)
                    buffer[i] = (wave * envelope * 12000.0).toInt().coerceIn(-32768, 32767).toShort()
                }
                buffer
            }
            SoundType.ACTION_COMPLETE -> {
                // 140ms pleasant notification ding (784Hz [G5] -> 1046Hz [C6])
                val durationSec = 0.14
                val numSamples = (SAMPLE_RATE * durationSec).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val freq = if (t < 0.05) 784.0 else 1046.50
                    val envelope = Math.exp(-14.0 * (if (t < 0.05) t else t - 0.05))
                    val wave = sin(2.0 * PI * freq * t) + 0.2 * sin(3.0 * PI * freq * t)
                    buffer[i] = (wave * envelope * 16000.0).toInt().coerceIn(-32768, 32767).toShort()
                }
                buffer
            }
            SoundType.REFUND_OR_UNLOCK -> {
                // 220ms ascending triad (523Hz [C5] -> 659Hz [E5] -> 784Hz [G5])
                val durationSec = 0.22
                val numSamples = (SAMPLE_RATE * durationSec).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val (freq, noteT) = when {
                        t < 0.07 -> Pair(523.25, t)
                        t < 0.14 -> Pair(659.25, t - 0.07)
                        else -> Pair(783.99, t - 0.14)
                    }
                    val envelope = Math.exp(-11.0 * noteT)
                    val wave = sin(2.0 * PI * freq * t) + 0.25 * sin(2.0 * 2.0 * PI * freq * t)
                    buffer[i] = (wave * envelope * 17000.0).toInt().coerceIn(-32768, 32767).toShort()
                }
                buffer
            }
            SoundType.ALERT -> {
                // 150ms gentle dual-tone
                val durationSec = 0.15
                val numSamples = (SAMPLE_RATE * durationSec).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val freq = if (t < 0.07) 880.0 else 740.0
                    val envelope = Math.exp(-12.0 * (if (t < 0.07) t else t - 0.07))
                    val wave = sin(2.0 * PI * freq * t)
                    buffer[i] = (wave * envelope * 14000.0).toInt().coerceIn(-32768, 32767).toShort()
                }
                buffer
            }
        }
    }

    private fun playPcm(pcmData: ShortArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = Math.max(minBufferSize, pcmData.size * 2)

        val audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STATIC
            )
        }

        try {
            audioTrack.write(pcmData, 0, pcmData.size)
            audioTrack.play()
            val durationMs = (pcmData.size * 1000L / SAMPLE_RATE) + 80L
            Thread.sleep(durationMs)
        } finally {
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (ignored: Exception) {}
        }
    }
}
