package com.pulseflow.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.sin

class FlowRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val start = System.currentTimeMillis()

    var speed = 1.0f
    var colors = intArrayOf(
        Color.rgb(80, 40, 190),
        Color.rgb(15, 125, 210),
        Color.rgb(220, 45, 130)
    )

    fun draw(canvas: Canvas) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        canvas.drawColor(Color.rgb(5, 6, 12))

        val t = ((System.currentTimeMillis() - start) / 1000f) * speed
        val maxSize = maxOf(w, h)

        // Several large, softly overlapping blobs create a more liquid flow
        // than three simple orbiting circles.
        for (i in 0 until 6) {
            val color = colors[i % colors.size]
            val p1 = t * (0.13f + i * 0.009f) + i * 1.17f
            val p2 = t * (0.09f + i * 0.007f) + i * 0.73f
            val p3 = t * (0.06f + i * 0.005f) + i * 1.91f

            val x = w * (
                0.5f +
                    0.27f * sin(p1.toDouble()).toFloat() +
                    0.10f * sin((p2 * 1.63f).toDouble()).toFloat()
                )
            val y = h * (
                0.5f +
                    0.24f * cos(p2.toDouble()).toFloat() +
                    0.09f * sin((p3 * 1.37f).toDouble()).toFloat()
                )

            val pulse = 0.5f + 0.5f * sin((p3 * 1.25f).toDouble()).toFloat()
            val radius = maxSize * (0.46f + 0.15f * pulse)
            val alpha = if (i < 3) 205 else 145
            val softColor = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

            paint.shader = RadialGradient(
                x,
                y,
                radius,
                intArrayOf(softColor, Color.argb(alpha / 2, Color.red(color), Color.green(color), Color.blue(color)), Color.TRANSPARENT),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(x, y, radius, paint)
        }

        paint.shader = null
    }
}
