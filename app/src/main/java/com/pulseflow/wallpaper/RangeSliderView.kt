package com.pulseflow.wallpaper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class RangeSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var minValue = 0.05f
    private var maxValue = 2.5f
    var low = 0.25f
        private set
    var high = 1.15f
        private set
    private var activeThumb = 0
    private var listener: ((Float, Float) -> Unit)? = null

    fun configure(min: Float, max: Float, low: Float, high: Float) {
        minValue = min
        maxValue = max
        this.low = low.coerceIn(min, max)
        this.high = high.coerceIn(this.low, max)
        invalidate()
    }

    fun setOnRangeChangedListener(block: (Float, Float) -> Unit) {
        listener = block
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (52 * resources.displayMetrics.density).toInt())
    }

    private fun valueToX(v: Float): Float {
        val pad = 24f * resources.displayMetrics.density
        val usable = width - pad * 2f
        return pad + ((v - minValue) / (maxValue - minValue)) * usable
    }

    private fun xToValue(x: Float): Float {
        val pad = 24f * resources.displayMetrics.density
        val usable = (width - pad * 2f).coerceAtLeast(1f)
        val t = ((x - pad) / usable).coerceIn(0f, 1f)
        return minValue + t * (maxValue - minValue)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val y = height / 2f
        val left = valueToX(minValue)
        val right = valueToX(maxValue)
        val lx = valueToX(low)
        val hx = valueToX(high)
        val d = resources.displayMetrics.density

        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 4f * d
        paint.color = Color.rgb(55, 58, 72)
        canvas.drawLine(left, y, right, y, paint)
        paint.color = Color.rgb(155, 108, 255)
        canvas.drawLine(lx, y, hx, y, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawCircle(lx, y, 10f * d, paint)
        canvas.drawCircle(hx, y, 10f * d, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * d
        paint.color = Color.rgb(155, 108, 255)
        canvas.drawCircle(lx, y, 10f * d, paint)
        canvas.drawCircle(hx, y, 10f * d, paint)
        paint.style = Paint.Style.FILL
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val xLow = valueToX(low)
                val xHigh = valueToX(high)
                activeThumb = if (abs(event.x - xLow) <= abs(event.x - xHigh)) 1 else 2
                updateFromX(event.x)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                updateFromX(event.x)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                activeThumb = 0
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateFromX(x: Float) {
        val v = xToValue(x)
        if (activeThumb == 1) low = v.coerceAtMost(high - 0.01f)
        if (activeThumb == 2) high = v.coerceAtLeast(low + 0.01f)
        invalidate()
        listener?.invoke(low, high)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
