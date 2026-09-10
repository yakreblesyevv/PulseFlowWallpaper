package com.pulseflow.wallpaper

import android.graphics.*
import kotlin.math.cos
import kotlin.math.sin

class FlowRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlay = Paint(Paint.ANTI_ALIAS_FLAG)
    private val start = System.currentTimeMillis()

    var speed = 1.0f
    var scale = 1.0f
    var brightness = 1.0f
    var blur = 0.72f
    var colors = intArrayOf(Color.rgb(80,40,190), Color.rgb(15,125,210), Color.rgb(220,45,130))

    fun draw(canvas: Canvas) {
        val w = canvas.width.toFloat(); val h = canvas.height.toFloat()
        canvas.drawColor(Color.rgb(3,4,9))
        val t = ((System.currentTimeMillis() - start) / 1000f) * speed
        val base = maxOf(w,h) * scale

        // Multiple oversized gradients move at different frequencies. Their edges stay
        // outside the screen much of the time, producing broad liquid fields instead
        // of visible orbiting circles.
        for (i in 0 until 9) {
            val raw = colors[i % colors.size]
            val r = (Color.red(raw) * brightness).toInt().coerceIn(0,255)
            val g = (Color.green(raw) * brightness).toInt().coerceIn(0,255)
            val b = (Color.blue(raw) * brightness).toInt().coerceIn(0,255)
            val a = if (i < 3) 180 else 105

            val p = t * (0.055f + i * 0.0037f) + i * 0.91f
            val q = t * (0.041f + i * 0.0029f) + i * 1.37f
            val x = w * (0.50f + 0.48f*sin(p.toDouble()).toFloat() + 0.12f*sin((q*1.71f).toDouble()).toFloat())
            val y = h * (0.50f + 0.44f*cos(q.toDouble()).toFloat() + 0.11f*cos((p*1.43f).toDouble()).toFloat())
            val breathe = 0.5f + 0.5f*sin((p*0.73f + q*0.31f).toDouble()).toFloat()
            val radius = base * (0.62f + 0.22f*breathe)
            val c0 = Color.argb(a,r,g,b)
            val c1 = Color.argb((a*(0.62f + blur*0.22f)).toInt(),r,g,b)
            val edge = (0.34f + blur*0.36f).coerceIn(0.34f,0.70f)
            paint.shader = RadialGradient(x,y,radius,intArrayOf(c0,c1,Color.TRANSPARENT),floatArrayOf(0f,edge,1f),Shader.TileMode.CLAMP)
            canvas.drawCircle(x,y,radius,paint)
        }
        paint.shader = null

        // A subtle dark veil keeps highlights smooth and avoids neon clipping.
        overlay.color = Color.argb(22,0,0,0)
        canvas.drawRect(0f,0f,w,h,overlay)
    }
}
