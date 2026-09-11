package com.pulseflow.wallpaper

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class PulseWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = FlowEngine()

    inner class FlowEngine : Engine() {
        private val renderer = FlowRenderer()
        private val audioConsumer="wallpaper-${hashCode()}"
        private val handler = android.os.Handler(mainLooper)
        private var visible = false
        private var frameDelay = 16L
        private val tick = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) handler.postDelayed(this, frameDelay)
            }
        }
        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { reload() }
        }

        private fun reload() {
            val ctx = this@PulseWallpaperService
            renderer.colors = PaletteStore.load(ctx)
            renderer.artwork = AlbumArtStore.active(ctx)
            renderer.liveBeats = FlowSettings.loadLiveBeats(ctx)
            renderer.debugView = FlowSettings.loadDebugView(ctx)
            if (Build.VERSION.SDK_INT >= 27) notifyColorsChanged()
            renderer.speedMin = FlowSettings.loadSpeedMin(ctx)
            renderer.speedMax = FlowSettings.loadSpeedMax(ctx)
            renderer.scale = FlowSettings.loadScale(ctx)
            renderer.brightness = FlowSettings.loadBrightness(ctx)
            renderer.blur = FlowSettings.loadBlur(ctx)
            renderer.graphicsMode = FlowSettings.loadGraphicsMode(ctx)
            renderer.beatStrength = FlowSettings.loadBeatStrength(ctx)
            frameDelay = if (FlowSettings.loadPerformanceMode(ctx)) 33L else 16L

            val hasAudio = Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            BeatAnalyzer.setConsumer(ctx,audioConsumer,visible && FlowSettings.loadLiveBeats(ctx) && hasAudio)
        }

        override fun onCreate(holder: SurfaceHolder?) {
            super.onCreate(holder)
            reload()
            val f = IntentFilter().apply {
                addAction(PaletteStore.ACTION_PALETTE)
                addAction(FlowSettings.ACTION_SETTINGS_CHANGED)
            }
            registerReceiver(receiver, f, "$packageName.INTERNAL", null, RECEIVER_NOT_EXPORTED)
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            handler.removeCallbacks(tick)
            if (!v) BeatAnalyzer.setConsumer(this@PulseWallpaperService,audioConsumer,false)
            if (v) {
                reload()
                handler.post(tick)
            }
        }

        private fun drawFrame() {
            var c: android.graphics.Canvas? = null
            try {
                c = if (Build.VERSION.SDK_INT >= 26) surfaceHolder.lockHardwareCanvas() else surfaceHolder.lockCanvas()
                if (c != null) renderer.draw(c)
            } catch (_: Throwable) {
                if (c != null) {
                    try { surfaceHolder.unlockCanvasAndPost(c) } catch (_: Throwable) {}
                    c = null
                }
                try {
                    c = surfaceHolder.lockCanvas()
                    if (c != null) renderer.draw(c)
                } catch (_: Throwable) {}
            } finally {
                if (c != null) try { surfaceHolder.unlockCanvasAndPost(c) } catch (_: Throwable) {}
            }
        }

        override fun onComputeColors(): android.app.WallpaperColors? {
            if (Build.VERSION.SDK_INT < 27 || !FlowSettings.loadAdaptiveColors(this@PulseWallpaperService)) return null
            val colors=PaletteStore.load(this@PulseWallpaperService)
            return android.app.WallpaperColors(android.graphics.Color.valueOf(colors[0]),android.graphics.Color.valueOf(colors[1]),android.graphics.Color.valueOf(colors[2]))
        }

        override fun onDestroy() {
            visible = false
            handler.removeCallbacks(tick)
            try { unregisterReceiver(receiver) } catch (_: Throwable) {}
            BeatAnalyzer.setConsumer(this@PulseWallpaperService,audioConsumer,false)
            super.onDestroy()
        }
    }
}
