package com.pulseflow.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color

object PaletteStore {
    const val ACTION_PALETTE = "com.pulseflow.wallpaper.PALETTE_CHANGED"
    private const val PREF = "pulse_palette"

    fun save(context: Context, bitmap: Bitmap?) {
        if (bitmap == null) return
        val scaled = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
        val buckets = HashMap<Int, Int>()
        for (x in 0 until scaled.width) for (y in 0 until scaled.height) {
            val c = scaled.getPixel(x, y)
            val hsv = FloatArray(3); Color.colorToHSV(c, hsv)
            if (hsv[1] < .18f || hsv[2] < .18f) continue
            val key = ((hsv[0] / 30).toInt() * 30) % 360
            buckets[key] = (buckets[key] ?: 0) + 1
        }
        val hues = buckets.entries.sortedByDescending { it.value }.take(3).map { it.key }
        val fallback = listOf(270, 205, 330)
        val out = (0..2).map { Color.HSVToColor(floatArrayOf((hues.getOrNull(it) ?: fallback[it]).toFloat(), .72f, .88f)) }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt("c0", out[0]).putInt("c1", out[1]).putInt("c2", out[2]).apply()
        context.sendBroadcast(android.content.Intent(ACTION_PALETTE).setPackage(context.packageName))
    }

    fun load(context: Context): IntArray {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return intArrayOf(p.getInt("c0", Color.rgb(80,40,190)), p.getInt("c1", Color.rgb(15,125,210)), p.getInt("c2", Color.rgb(220,45,130)))
    }
}
