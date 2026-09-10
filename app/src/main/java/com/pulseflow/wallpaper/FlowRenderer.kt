package com.pulseflow.wallpaper

import android.graphics.*
import kotlin.math.cos
import kotlin.math.sin

class FlowRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var start = System.currentTimeMillis()
    var colors = intArrayOf(Color.rgb(80, 40, 190), Color.rgb(15, 125, 210), Color.rgb(220, 45, 130))

    fun draw(canvas: Canvas) {
        val w = canvas.width.toFloat(); val h = canvas.height.toFloat()
        canvas.drawColor(Color.rgb(5, 6, 12))
        val t = (System.currentTimeMillis() - start) / 1000f
        for (i in 0..2) {
            val phase = t * (0.22f + i * 0.035f) + i * 2.1f
            val x = w * (0.5f + 0.32f * sin(phase.toDouble()).toFloat())
            val y = h * (0.5f + 0.28f * cos((phase * 0.83f).toDouble()).toFloat())
            val radius = maxOf(w, h) * (0.55f + 0.06f * sin((phase * 0.6f).toDouble()).toFloat())
            paint.shader = RadialGradient(x, y, radius, colors[i], Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawCircle(x, y, radius, paint)
        }
        paint.shader = null
    }
}
