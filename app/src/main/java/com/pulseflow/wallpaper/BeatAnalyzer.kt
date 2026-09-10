package com.pulseflow.wallpaper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import kotlin.math.max
import kotlin.math.sqrt

object BeatAnalyzer {
    @Volatile var level: Float = 0f
        private set

    private var visualizer: Visualizer? = null
    private var smoothed = 0f

    @Synchronized
    fun start(context: Context): Boolean {
        if (!FlowSettings.loadLiveBeats(context)) {
            stop()
            return false
        }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stop()
            return false
        }
        if (visualizer != null) return true

        return try {
            val v = Visualizer(0)
            val range = Visualizer.getCaptureSizeRange()
            v.captureSize = range[1].coerceAtMost(1024)
            v.scalingMode = Visualizer.SCALING_MODE_NORMALIZED
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int
                ) {
                    if (waveform == null || waveform.isEmpty()) return
                    var sum = 0.0
                    for (b in waveform) {
                        val s = ((b.toInt() and 0xFF) - 128) / 128.0
                        sum += s * s
                    }
                    val rms = sqrt(sum / waveform.size).toFloat()
                    push(rms * 2.1f)
                }

                override fun onFftDataCapture(
                    visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int
                ) {
                    if (fft == null || fft.size < 8) return
                    val bins = minOf(24, (fft.size - 2) / 2)
                    var bass = 0f
                    for (i in 1..bins) {
                        val re = fft[i * 2].toFloat()
                        val im = fft[i * 2 + 1].toFloat()
                        bass += sqrt(re * re + im * im)
                    }
                    push((bass / max(1, bins)) / 72f)
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, true)
            v.enabled = true
            visualizer = v
            true
        } catch (_: Throwable) {
            visualizer = null
            level = 0f
            false
        }
    }

    @Synchronized
    fun stop() {
        try { visualizer?.enabled = false } catch (_: Throwable) {}
        try { visualizer?.release() } catch (_: Throwable) {}
        visualizer = null
        smoothed = 0f
        level = 0f
    }

    private fun push(raw: Float) {
        val clamped = raw.coerceIn(0f, 1f)
        smoothed = if (clamped > smoothed) {
            smoothed * 0.42f + clamped * 0.58f
        } else {
            smoothed * 0.88f + clamped * 0.12f
        }
        level = smoothed.coerceIn(0f, 1f)
    }
}
