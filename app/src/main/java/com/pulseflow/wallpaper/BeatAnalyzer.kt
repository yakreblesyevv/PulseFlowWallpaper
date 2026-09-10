package com.pulseflow.wallpaper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.os.Build
import android.os.SystemClock
import kotlin.math.max
import kotlin.math.sqrt

object BeatAnalyzer {
    @Volatile var level: Float = 0f
        private set
    @Volatile var bass: Float = 0f
        private set
    @Volatile var rawBass: Float = 0f
        private set
    @Volatile var callbackCount: Long = 0L
        private set
    @Volatile var lastCallbackMs: Long = 0L
        private set
    @Volatile var running: Boolean = false
        private set

    private var visualizer: Visualizer? = null
    private var envelope = 0f
    private var floor = 0.015f
    private var ceiling = 0.18f
    private var previousEnergy = 0f

    fun hasSignal(nowMs: Long = SystemClock.elapsedRealtime()): Boolean =
        running && callbackCount > 0 && nowMs - lastCallbackMs < 1400L

    fun statusText(): String = when {
        !running -> "OFF"
        hasSignal() -> "ACTIVE"
        else -> "NO SIGNAL"
    }

    @Synchronized
    fun start(context: Context): Boolean {
        if (visualizer != null) {
            running = true
            return true
        }
        if (Build.VERSION.SDK_INT >= 23 &&
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) return false

        return try {
            val v = Visualizer(0)
            val range = Visualizer.getCaptureSizeRange()
            v.captureSize = range[1]
            v.scalingMode = Visualizer.SCALING_MODE_NORMALIZED
            val rate = (Visualizer.getMaxCaptureRate() * 0.95f).toInt()
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) = Unit

                override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    if (fft == null || fft.size < 16) return
                    callbackCount++
                    lastCallbackMs = SystemClock.elapsedRealtime()
                    processFft(fft, samplingRate)
                }
            }, rate, false, true)
            v.enabled = true
            visualizer = v
            running = true
            callbackCount = 0L
            lastCallbackMs = 0L
            true
        } catch (_: Throwable) {
            stop()
            false
        }
    }

    @Synchronized
    fun stop() {
        try { visualizer?.enabled = false } catch (_: Throwable) {}
        try { visualizer?.release() } catch (_: Throwable) {}
        visualizer = null
        running = false
        envelope = 0f
        bass = 0f
        rawBass = 0f
        floor = 0.015f
        ceiling = 0.18f
        previousEnergy = 0f
        callbackCount = 0L
        lastCallbackMs = 0L
        level = 0f
    }

    @Synchronized
    private fun processFft(fft: ByteArray, samplingRateMilliHz: Int) {
        val sampleRateHz = (samplingRateMilliHz / 1000f).coerceAtLeast(8000f)
        val hzPerBin = sampleRateHz / fft.size.toFloat()
        val maxBin = fft.size / 2 - 1

        var subSum = 0.0; var subCount = 0
        var bassSum = 0.0; var bassCount = 0
        var lowMidSum = 0.0; var lowMidCount = 0
        var strongest = 0f

        for (bin in 1..maxBin) {
            val freq = bin * hzPerBin
            if (freq > 520f) break
            val reIndex = bin * 2
            val imIndex = reIndex + 1
            if (imIndex >= fft.size) break
            val re = fft[reIndex].toInt().toFloat()
            val im = fft[imIndex].toInt().toFloat()
            val mag = sqrt(re * re + im * im) / 181.0f
            strongest = max(strongest, mag)
            when (freq) {
                in 32f..75f -> { subSum += mag; subCount++ }
                in 75f..190f -> { bassSum += mag; bassCount++ }
                in 190f..520f -> { lowMidSum += mag; lowMidCount++ }
            }
        }

        val sub = if (subCount > 0) (subSum / subCount).toFloat() else 0f
        val mainBass = if (bassCount > 0) (bassSum / bassCount).toFloat() else 0f
        val lowMid = if (lowMidCount > 0) (lowMidSum / lowMidCount).toFloat() else 0f

        val energy = (sub * 1.45f + mainBass * 1.15f + lowMid * 0.12f + strongest * 0.10f)
            .coerceIn(0f, 1.4f)
        rawBass = energy.coerceIn(0f, 1f)

        // Adaptive range: quiet sections track down quickly, loud peaks decay slowly.
        floor = if (energy < floor) floor * 0.88f + energy * 0.12f else floor * 0.995f + energy * 0.005f
        ceiling = if (energy > ceiling) ceiling * 0.72f + energy * 0.28f else ceiling * 0.997f + energy * 0.003f
        floor = floor.coerceIn(0.003f, 0.24f)
        ceiling = ceiling.coerceIn(floor + 0.055f, 1.2f)

        val normalized = ((energy - floor * 0.90f) / (ceiling - floor).coerceAtLeast(0.055f))
            .coerceIn(0f, 1f)
        val transient = ((energy - previousEnergy) * 5.2f).coerceIn(0f, 1f)
        previousEnergy = previousEnergy * 0.58f + energy * 0.42f

        val punch = (normalized * 0.76f + transient * 0.62f).coerceIn(0f, 1f)
        envelope = if (punch > envelope) {
            envelope * 0.08f + punch * 0.92f
        } else {
            envelope * 0.80f + punch * 0.20f
        }

        bass = normalized
        level = envelope.coerceIn(0f, 1f)
    }
}
