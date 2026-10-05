package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Retro sound effects synthesised on the fly with [AudioTrack] — no audio files or network needed.
 */
class SoundEffectsEngine {

    enum class Sfx { LASER, HIT, ERROR, FANFARE, CLICK, IMPACT, POWER_UP }

    @Volatile var enabled: Boolean = true

    private val executor = Executors.newSingleThreadExecutor()
    private val cache = HashMap<Sfx, ShortArray>()

    fun play(sfx: Sfx) {
        if (!enabled) return
        executor.execute {
            runCatching {
                val pcm = cache.getOrPut(sfx) { synthesize(sfx) }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
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
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.size * 2)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(pcm.size * 1000L / SAMPLE_RATE + 40)
                track.release()
            }
        }
    }

    fun laser() = play(Sfx.LASER)
    fun hit() = play(Sfx.HIT)
    fun error() = play(Sfx.ERROR)
    fun fanfare() = play(Sfx.FANFARE)
    fun click() = play(Sfx.CLICK)
    fun impact() = play(Sfx.IMPACT)
    fun powerUp() = play(Sfx.POWER_UP)

    fun release() = executor.shutdownNow()

    private fun synthesize(sfx: Sfx): ShortArray = when (sfx) {
        Sfx.LASER -> sweep(1400.0, 250.0, 0.16, square = true, volume = 0.22)
        Sfx.HIT -> mix(noise(0.22, 0.35), sweep(320.0, 60.0, 0.22, square = false, volume = 0.35))
        Sfx.ERROR -> concat(tone(180.0, 0.12, square = true, volume = 0.2), tone(140.0, 0.18, square = true, volume = 0.2))
        Sfx.FANFARE -> concat(
            tone(523.25, 0.11), tone(659.25, 0.11), tone(783.99, 0.11), tone(1046.5, 0.32)
        )
        Sfx.CLICK -> tone(880.0, 0.035, volume = 0.18)
        Sfx.IMPACT -> mix(noise(0.45, 0.45), sweep(120.0, 35.0, 0.45, square = false, volume = 0.5))
        Sfx.POWER_UP -> sweep(300.0, 1200.0, 0.3, square = true, volume = 0.18)
    }

    private fun samples(seconds: Double) = (SAMPLE_RATE * seconds).toInt()

    private fun envelope(i: Int, n: Int): Double {
        val attack = minOf(n / 20, 200)
        val a = if (i < attack) i / attack.toDouble() else 1.0
        return a * exp(-3.0 * i / n)
    }

    private fun tone(freq: Double, seconds: Double, square: Boolean = false, volume: Double = 0.3): ShortArray {
        val n = samples(seconds)
        return ShortArray(n) { i ->
            val phase = sin(2 * PI * freq * i / SAMPLE_RATE)
            val v = if (square) (if (phase >= 0) 1.0 else -1.0) else phase
            (v * envelope(i, n) * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun sweep(from: Double, to: Double, seconds: Double, square: Boolean, volume: Double): ShortArray {
        val n = samples(seconds)
        var phase = 0.0
        return ShortArray(n) { i ->
            val f = from + (to - from) * i / n
            phase += 2 * PI * f / SAMPLE_RATE
            val s = sin(phase)
            val v = if (square) (if (s >= 0) 1.0 else -1.0) else s
            (v * envelope(i, n) * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun noise(seconds: Double, volume: Double): ShortArray {
        val n = samples(seconds)
        val rnd = Random(7)
        return ShortArray(n) { i -> ((rnd.nextDouble() * 2 - 1) * envelope(i, n) * volume * Short.MAX_VALUE).toInt().toShort() }
    }

    private fun mix(a: ShortArray, b: ShortArray): ShortArray =
        ShortArray(maxOf(a.size, b.size)) { i ->
            val sum = (if (i < a.size) a[i].toInt() else 0) + (if (i < b.size) b[i].toInt() else 0)
            sum.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

    private fun concat(vararg parts: ShortArray): ShortArray {
        val out = ShortArray(parts.sumOf { it.size })
        var offset = 0
        parts.forEach { it.copyInto(out, offset); offset += it.size }
        return out
    }

    private companion object {
        const val SAMPLE_RATE = 22050
    }
}
