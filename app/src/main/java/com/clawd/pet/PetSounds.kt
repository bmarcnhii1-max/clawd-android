package com.clawd.pet

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.sin
import kotlin.random.Random

/**
 * Звуки питомца без единого аудиофайла: всё синтезируется в PCM на лету.
 * Полный офлайн, ноль ассетов. Короткие и тихие; в беззвучном режиме молчим.
 */
object PetSounds {
    private const val RATE = 22050

    private fun allowed(ctx: Context): Boolean {
        if (!Prefs(ctx).sounds) return false
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.ringerMode != AudioManager.RINGER_MODE_SILENT
    }

    private fun play(ctx: Context, samples: ShortArray) {
        if (!allowed(ctx)) return
        Thread {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(samples, 0, samples.size)
                track.play()
                Thread.sleep((samples.size * 1000L / RATE) + 50L)
                track.stop()
                track.release()
            } catch (_: Exception) {
            }
        }.start()
    }

    private fun sine(phase: Double): Short =
        (sin(phase) * Short.MAX_VALUE * 0.3).toInt().toShort()

    /** Мурчание ~1 с: низкий рокот 48 Гц с модуляцией 23 Гц. */
    fun purr(ctx: Context) {
        val n = RATE
        val s = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            val mod = 0.6 + 0.4 * sin(2.0 * Math.PI * 23.0 * t)
            s[i] = (sin(2.0 * Math.PI * 48.0 * t) * mod * Short.MAX_VALUE * 0.3).toInt().toShort()
        }
        play(ctx, s)
    }

    /** Победа: две восходящие ноты. */
    fun chirp(ctx: Context) {
        val notes = listOf(700.0 to 1050.0, 900.0 to 1400.0)
        val per = RATE * 18 / 100
        val s = ShortArray(per * 2)
        for (k in 0..1) {
            val (f0, f1) = notes[k]
            for (i in 0 until per) {
                val f = f0 + (f1 - f0) * i / per
                s[k * per + i] = sine(2.0 * Math.PI * f * i / RATE)
            }
        }
        play(ctx, s)
    }

    /** Еда: три коротких хруста шумом с затуханием. */
    fun munch(ctx: Context) {
        val rnd = Random.Default
        val per = RATE * 9 / 100
        val gap = RATE * 5 / 100
        val s = ShortArray((per + gap) * 3)
        for (k in 0..2) {
            val base = k * (per + gap)
            for (i in 0 until per) {
                val decay = 1.0 - i.toDouble() / per
                s[base + i] = (rnd.nextFloat() * 2f - 1f).let { (it * Short.MAX_VALUE * 0.25 * decay).toInt().toShort() }
            }
        }
        play(ctx, s)
    }
}
