package com.pulseflow.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class PulseWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = FlowEngine()

    inner class FlowEngine : Engine() {
        private val renderer = FlowRenderer()
        private val handler = android.os.Handler(mainLooper)
        private var visible = false

        private val tick = object : Runnable {
            override fun run() {
                draw()
                if (visible) handler.postDelayed(this, 16)
            }
        }

        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    PaletteStore.ACTION_PALETTE -> renderer.colors = PaletteStore.load(this@PulseWallpaperService)
                    FlowSettings.ACTION_SPEED_CHANGED -> renderer.speed = FlowSettings.loadSpeed(this@PulseWallpaperService)
                }
            }
        }

        override fun onCreate(holder: SurfaceHolder?) {
            super.onCreate(holder)
            renderer.colors = PaletteStore.load(this@PulseWallpaperService)
            renderer.speed = FlowSettings.loadSpeed(this@PulseWallpaperService)

            val filter = IntentFilter().apply {
                addAction(PaletteStore.ACTION_PALETTE)
                addAction(FlowSettings.ACTION_SPEED_CHANGED)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(receiver, filter)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            handler.removeCallbacks(tick)
            if (visible) handler.post(tick)
        }

        private fun draw() {
            var canvas: android.graphics.Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) renderer.draw(canvas)
            } finally {
                if (canvas != null) surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }

        override fun onDestroy() {
            visible = false
            handler.removeCallbacks(tick)
            unregisterReceiver(receiver)
            super.onDestroy()
        }
    }
}
