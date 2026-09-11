package com.pulseflow.wallpaper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.os.Build
import android.os.SystemClock
import kotlin.math.ln
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

    private val consumers=mutableSetOf<String>()
    @Synchronized fun setConsumer(context: Context, key: String, active: Boolean): Boolean {
        if(active) { consumers.add(key); return start(context) }
        consumers.remove(key)
        if(consumers.isEmpty()) stop()
        return false
    }
    private var visualizer: Visualizer? = null
    @Volatile private var lastEnergyMs=0L
    private var envelope = 0f
    private var baseline = 0.025f
    private var peakTracker = 0.18f
    private var previousEnergy = 0f
    private var previousNormalized = 0f

    fun hasSignal(nowMs: Long = SystemClock.elapsedRealtime()): Boolean =
        running && callbackCount > 0 && nowMs - lastCallbackMs < 1400L && nowMs-lastEnergyMs<1000L

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
            visualizer = v
            val range = Visualizer.getCaptureSizeRange()
            v.captureSize = range[1]
            v.scalingMode = Visualizer.SCALING_MODE_NORMALIZED
            val rate = (Visualizer.getMaxCaptureRate() * 0.90f).toInt()
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
        baseline = 0.025f
        peakTracker = 0.18f
        previousEnergy = 0f
        previousNormalized = 0f
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
            if (freq > 420f) break
            val reIndex = bin * 2
            val imIndex = reIndex + 1
            if (imIndex >= fft.size) break
            val re = fft[reIndex].toInt().toFloat()
            val im = fft[imIndex].toInt().toFloat()
            val mag = sqrt(re * re + im * im) / 181.0f
            strongest = max(strongest, mag)
            when (freq) {
                in 35f..72f -> { subSum += mag; subCount++ }
                in 72f..170f -> { bassSum += mag; bassCount++ }
                in 170f..420f -> { lowMidSum += mag; lowMidCount++ }
            }
        }

        val sub = if (subCount > 0) (subSum / subCount).toFloat() else 0f
        val mainBass = if (bassCount > 0) (bassSum / bassCount).toFloat() else 0f
        val lowMid = if (lowMidCount > 0) (lowMidSum / lowMidCount).toFloat() else 0f

        // Weighted bass energy. Log compression prevents loud tracks from pinning at 1.00.
        val linear = sub * 1.20f + mainBass * 0.95f + lowMid * 0.08f + strongest * 0.035f
        if(linear>0.002f) lastEnergyMs=SystemClock.elapsedRealtime()
        val energy = (ln(1.0 + linear * 5.0) / ln(6.0)).toFloat().coerceIn(0f, 1.15f)
        rawBass = energy.coerceIn(0f, 1f)

        // Baseline follows sustained loudness; peak tracker holds short-lived peaks.
        val baselineRate = if (energy < baseline) 0.10f else 0.018f
        baseline += (energy - baseline) * baselineRate
        baseline = baseline.coerceIn(0.004f, 0.72f)

        if (energy > peakTracker) {
            peakTracker += (energy - peakTracker) * 0.42f
        } else {
            peakTracker *= 0.992f
        }
        peakTracker = peakTracker.coerceIn(baseline + 0.10f, 1.15f)

        val range = (peakTracker - baseline).coerceAtLeast(0.10f)
        var normalized = ((energy - baseline) / range).coerceIn(0f, 1f)
        // Gentle curve: keep normal music in the middle instead of saturating.
        normalized = (normalized * 0.82f).coerceIn(0f, 0.92f)

        val energyRise = (energy - previousEnergy).coerceAtLeast(0f)
        val normalizedRise = (normalized - previousNormalized).coerceAtLeast(0f)
        val transient = (energyRise * 3.6f + normalizedRise * 1.8f).coerceIn(0f, 1f)

        previousEnergy += (energy - previousEnergy) * 0.34f
        previousNormalized += (normalized - previousNormalized) * 0.42f

        // Beat pulse favors transients; sustained bass remains visible but not pegged.
        val punch = (normalized * 0.48f + transient * 0.78f).coerceIn(0f, 1f)
        envelope = if (punch > envelope) {
            envelope * 0.12f + punch * 0.88f
        } else {
            envelope * 0.88f + punch * 0.12f
        }

        bass = normalized
        level = envelope.coerceIn(0f, 1f)
    }
}
