package com.pulseflow.wallpaper

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

class FlowPreviewView @JvmOverloads constructor(private val c: Context, a: AttributeSet? = null) : View(c, a) {
    private val renderer = FlowRenderer()
    init { reload() }

    fun reload() {
        renderer.colors = PaletteStore.load(c)
        renderer.speedMin = FlowSettings.loadSpeedMin(c)
        renderer.speedMax = FlowSettings.loadSpeedMax(c)
        renderer.scale = FlowSettings.loadScale(c)
        renderer.brightness = FlowSettings.loadBrightness(c)
        renderer.blur = FlowSettings.loadBlur(c)
        renderer.graphicsMode = FlowSettings.loadGraphicsMode(c)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas)
        postInvalidateDelayed(if (FlowSettings.loadPerformanceMode(c)) 33 else 16)
    }
}
