package com.pulseflow.wallpaper

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

class FlowPreviewView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : View(c, a) {
    private val renderer = FlowRenderer()

    init {
        renderer.colors = PaletteStore.load(c)
        renderer.speed = FlowSettings.loadSpeed(c)
    }

    fun setFlowSpeed(speed: Float) {
        renderer.speed = speed
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas)
        postInvalidateDelayed(16)
    }
}
