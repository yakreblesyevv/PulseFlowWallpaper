package com.pulseflow.wallpaper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.os.Build
import kotlin.math.sqrt

object BeatAnalyzer {
    @Volatile var level: Float = 0f
        private set
    @Volatile var bass: Float = 0f
        private set

    private var visualizer: Visualizer? = null
    private var envelope = 0f
    private var noiseFloor = 0.035f

    @Synchronized
    fun start(context: Context): Boolean {
        if (visualizer != null) return true
        if (Build.VERSION.SDK_INT >= 23 &&
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) return false

        return try {
            val v = Visualizer(0)
            val range = Visualizer.getCaptureSizeRange()
            v.captureSize = range[1]
            val rate = Visualizer.getMaxCaptureRate()
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) = Unit

                override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    if (fft == null || fft.size < 8) return
                    processFft(fft, samplingRate)
                }
            }, rate, false, true)
            v.enabled = true
            visualizer = v
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
        envelope = 0f
        bass = 0f
        level = 0f
        noiseFloor = 0.035f
    }

    @Synchronized
    private fun processFft(fft: ByteArray, samplingRateMilliHz: Int) {
        val sampleRateHz = (samplingRateMilliHz / 1000f).coerceAtLeast(8000f)
        val captureSize = fft.size
        val hzPerBin = sampleRateHz / captureSize.toFloat()

        var lowSum = 0.0
        var lowCount = 0
        var bodySum = 0.0
        var bodyCount = 0

        val maxBin = captureSize / 2 - 1
        for (bin in 1..maxBin) {
            val freq = bin * hzPerBin
            if (freq > 420f) break
            val reIndex = bin * 2
            val imIndex = reIndex + 1
            if (imIndex >= fft.size) break
            val re = fft[reIndex].toInt().toFloat()
            val im = fft[imIndex].toInt().toFloat()
            val mag = sqrt(re * re + im * im) / 181f

            if (freq in 45f..180f) {
                lowSum += mag
                lowCount++
            }
            if (freq in 180f..420f) {
                bodySum += mag
                bodyCount++
            }
        }

        val low = if (lowCount > 0) (lowSum / lowCount).toFloat() else 0f
        val body = if (bodyCount > 0) (bodySum / bodyCount).toFloat() else 0f
        val rawBass = (low * 1.35f + body * 0.20f).coerceIn(0f, 1f)

        // Slowly follow the room/device floor so quiet sections remain calm.
        noiseFloor = (noiseFloor * 0.985f + rawBass * 0.015f).coerceIn(0.015f, 0.32f)
        val normalized = ((rawBass - noiseFloor * 0.72f) / (0.34f - noiseFloor * 0.35f))
            .coerceIn(0f, 1f)

        // Fast attack + slower decay makes kick/bass hits readable as pulses.
        envelope = if (normalized > envelope) {
            envelope * 0.18f + normalized * 0.82f
        } else {
            envelope * 0.84f + normalized * 0.16f
        }

        bass = normalized
        level = envelope.coerceIn(0f, 1f)
    }
}
