package com.pulseflow.wallpaper

import android.content.*
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class PulseWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = FlowEngine()
    inner class FlowEngine : Engine() {
        private val renderer = FlowRenderer()
        private val handler = android.os.Handler(mainLooper)
        private var visible = false
        private val tick = object : Runnable { override fun run() { draw(); if (visible) handler.postDelayed(this, 33) } }
        private val receiver = object : BroadcastReceiver() { override fun onReceive(c: Context?, i: Intent?) { renderer.colors = PaletteStore.load(this@PulseWallpaperService) } }
        override fun onCreate(holder: SurfaceHolder?) { super.onCreate(holder); renderer.colors = PaletteStore.load(this@PulseWallpaperService); registerReceiver(receiver, IntentFilter(PaletteStore.ACTION_PALETTE), RECEIVER_NOT_EXPORTED) }
        override fun onVisibilityChanged(v: Boolean) { visible = v; handler.removeCallbacks(tick); if (v) handler.post(tick) }
        private fun draw() { var c: android.graphics.Canvas? = null; try { c = surfaceHolder.lockCanvas(); if (c != null) renderer.draw(c) } finally { if (c != null) surfaceHolder.unlockCanvasAndPost(c) } }
        override fun onDestroy() { visible=false; handler.removeCallbacks(tick); unregisterReceiver(receiver); super.onDestroy() }
    }
}
