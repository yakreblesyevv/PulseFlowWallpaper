package com.pulseflow.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.min

object PaletteStore {
    const val ACTION_PALETTE = "com.pulseflow.wallpaper.PALETTE_CHANGED"
    private const val PREF = "pulse_palette"

    data class Preset(val name: String, val colors: IntArray)
    private data class Sample(val h: Float, val s: Float, val v: Float, val weight: Float)

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
        val scaled = Bitmap.createScaledBitmap(bitmap, 36, 36, true)
        val samples = ArrayList<Sample>(scaled.width * scaled.height)
        val hsv = FloatArray(3)

        for (x in 0 until scaled.width) for (y in 0 until scaled.height) {
            val c = scaled.getPixel(x, y)
            Color.colorToHSV(c, hsv)
            val s = hsv[1]
            val v = hsv[2]
            if (v < 0.09f || v > 0.98f && s < 0.10f) continue
            val weight = (0.25f + s * 0.85f) * (0.35f + v * 0.65f)
            samples.add(Sample(hsv[0], s, v, weight))
        }

        if (samples.isEmpty()) return

        val hueBuckets = Array(36) { mutableListOf<Sample>() }
        samples.forEach { hueBuckets[(it.h / 10f).toInt().coerceIn(0, 35)].add(it) }

        val ranked = hueBuckets.mapIndexedNotNull { index, bucket ->
            if (bucket.isEmpty()) null else {
                val w = bucket.sumOf { it.weight.toDouble() }.toFloat()
                val h = bucket.sumOf { (it.h * it.weight).toDouble() }.toFloat() / w
                val s = bucket.sumOf { (it.s * it.weight).toDouble() }.toFloat() / w
                val v = bucket.sumOf { (it.v * it.weight).toDouble() }.toFloat() / w
                Triple(index, Sample(h, s, v, w), w)
            }
        }.sortedByDescending { it.third }

        val chosen = mutableListOf<Sample>()
        for ((_, sample, _) in ranked) {
            val distinct = chosen.all { hueDistance(it.h, sample.h) >= 32f }
            if (distinct || chosen.isEmpty()) chosen.add(sample)
            if (chosen.size == 3) break
        }
        for ((_, sample, _) in ranked) {
            if (chosen.size == 3) break
            if (chosen.none { abs(it.h - sample.h) < 1f }) chosen.add(sample)
        }

        val fallbackHues = floatArrayOf(270f, 205f, 330f)
        val out = IntArray(3) { i ->
            val sample = chosen.getOrNull(i)
            val h = sample?.h ?: fallbackHues[i]
            val s = (sample?.s ?: 0.70f).coerceIn(0.38f, 0.88f)
            // Preserve album mood but keep wallpaper from becoming muddy or blown-out.
            val v = (sample?.v ?: 0.82f).coerceIn(0.48f, 0.92f)
            Color.HSVToColor(floatArrayOf(h, s, v))
        }
        saveColors(context, out, "Album Art")
    }

    private fun hueDistance(a: Float, b: Float): Float {
        val d = abs(a - b) % 360f
        return min(d, 360f - d)
    }

    fun savePreset(context: Context, index: Int) {
        val preset = presets[index.coerceIn(0, presets.lastIndex)]
        saveColors(context, preset.colors, preset.name)
    }

    fun selectedName(context: Context): String =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("name", "Night Club") ?: "Night Club"

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
