package com.pulseflow.wallpaper

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

class FlowPreviewView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : View(c, a) {
    private val renderer = FlowRenderer()
    init { renderer.colors = PaletteStore.load(c) }
    override fun onDraw(canvas: Canvas) { super.onDraw(canvas); renderer.draw(canvas); postInvalidateDelayed(33) }
}
