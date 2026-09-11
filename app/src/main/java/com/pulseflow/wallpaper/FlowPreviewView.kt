package com.pulseflow.wallpaper

import android.content.*
import android.graphics.Canvas
import android.os.Build
import android.util.AttributeSet
import android.view.View

class FlowPreviewView @JvmOverloads constructor(private val c: Context, a: AttributeSet? = null) : View(c, a) {
    private val renderer = FlowRenderer()
    private var frameDelay = 16L
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { reload() }
    }
    init { reload() }

    fun reload() {
        renderer.colors = PaletteStore.load(c)
        renderer.artwork = AlbumArtStore.active(c)
        renderer.speedMin = FlowSettings.loadSpeedMin(c)
        renderer.speedMax = FlowSettings.loadSpeedMax(c)
        renderer.scale = FlowSettings.loadScale(c)
        renderer.brightness = FlowSettings.loadBrightness(c)
        renderer.blur = FlowSettings.loadBlur(c)
        renderer.graphicsMode = FlowSettings.loadGraphicsMode(c)
        renderer.beatStrength = FlowSettings.loadBeatStrength(c)
        renderer.liveBeats = FlowSettings.loadLiveBeats(c)
        renderer.debugView = FlowSettings.loadDebugView(c)
        frameDelay = if (FlowSettings.loadPerformanceMode(c)) 33L else 16L
        invalidate()
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val f = IntentFilter(PaletteStore.ACTION_PALETTE).apply { addAction(FlowSettings.ACTION_SETTINGS_CHANGED) }
        if (Build.VERSION.SDK_INT >= 33) c.registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED)
        else { @Suppress("DEPRECATION") c.registerReceiver(receiver,f) }
        reload()
    }
    override fun onDetachedFromWindow() {
        runCatching { c.unregisterReceiver(receiver) }
        super.onDetachedFromWindow()
    }
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas)
        if (windowVisibility == VISIBLE && isShown) postInvalidateDelayed(frameDelay)
    }
}
