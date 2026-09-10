package com.pulseflow.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color

object PaletteStore {
    const val ACTION_PALETTE = "com.pulseflow.wallpaper.PALETTE_CHANGED"
    private const val PREF = "pulse_palette"

    data class Preset(val name: String, val colors: IntArray)

    val presets = listOf(
        Preset("Monochrome", intArrayOf(Color.rgb(230,230,235), Color.rgb(105,110,125), Color.rgb(22,24,31))),
        Preset("Rainbow", intArrayOf(Color.rgb(255,78,127), Color.rgb(55,150,255), Color.rgb(120,75,255))),
        Preset("Fire", intArrayOf(Color.rgb(255,69,32), Color.rgb(255,145,30), Color.rgb(115,20,10))),
        Preset("Beach", intArrayOf(Color.rgb(20,170,210), Color.rgb(60,220,190), Color.rgb(245,190,100))),
        Preset("Lime", intArrayOf(Color.rgb(185,255,45), Color.rgb(70,180,55), Color.rgb(20,65,40))),
        Preset("Twilight", intArrayOf(Color.rgb(106,65,205), Color.rgb(228,75,155), Color.rgb(35,70,150))),
        Preset("Blue Marble", intArrayOf(Color.rgb(30,90,210), Color.rgb(120,180,245), Color.rgb(235,240,255))),
        Preset("Night Club", intArrayOf(Color.rgb(92,35,205), Color.rgb(10,125,210), Color.rgb(225,35,135)))
    )

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
        val out = (0..2).map { Color.HSVToColor(floatArrayOf((hues.getOrNull(it) ?: fallback[it]).toFloat(), .72f, .88f)) }.toIntArray()
        saveColors(context, out, "Album Art")
    }

    fun savePreset(context: Context, index: Int) {
        val preset = presets[index.coerceIn(0, presets.lastIndex)]
        saveColors(context, preset.colors, preset.name)
    }

    fun selectedName(context: Context): String = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("name", "Night Club") ?: "Night Club"

    fun load(context: Context): IntArray {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return intArrayOf(
            p.getInt("c0", Color.rgb(80,40,190)),
            p.getInt("c1", Color.rgb(15,125,210)),
            p.getInt("c2", Color.rgb(220,45,130))
        )
    }

    private fun saveColors(context: Context, colors: IntArray, name: String) {
        val c0 = colors.getOrElse(0) { Color.rgb(80,40,190) }
        val c1 = colors.getOrElse(1) { c0 }
        val c2 = colors.getOrElse(2) { c1 }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putInt("c0", c0).putInt("c1", c1).putInt("c2", c2).putString("name", name).apply()
        context.sendBroadcast(android.content.Intent(ACTION_PALETTE).setPackage(context.packageName))
    }
}
