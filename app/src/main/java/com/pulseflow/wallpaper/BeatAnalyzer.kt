package com.pulseflow.wallpaper

import kotlin.math.abs
import kotlin.math.sqrt

object BeatAnalyzer {
    @Volatile var level: Float = 0f
        private set

    private var smoothed = 0f

    @Synchronized
    fun pushPcm(samples: ShortArray, count: Int) {
        if (count <= 0) return
        var sum = 0.0
        var peak = 0f
        var i = 0
        while (i < count) {
            val n = samples[i] / 32768f
            sum += n * n
            peak = maxOf(peak, abs(n))
            i++
        }
        val rms = sqrt(sum / count).toFloat()
        val raw = (rms * 4.6f + peak * 0.28f).coerceIn(0f, 1f)
        push(raw)
    }

    @Synchronized
    fun silence() {
        smoothed *= 0.90f
        if (smoothed < 0.004f) smoothed = 0f
        level = smoothed
    }

    @Synchronized
    fun stop() {
        smoothed = 0f
        level = 0f
    }

    private fun push(raw: Float) {
        val clamped = raw.coerceIn(0f, 1f)
        smoothed = if (clamped > smoothed) {
            smoothed * 0.34f + clamped * 0.66f
        } else {
            smoothed * 0.84f + clamped * 0.16f
        }
        level = smoothed.coerceIn(0f, 1f)
    }
}
