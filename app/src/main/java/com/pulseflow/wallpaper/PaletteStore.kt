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
        val buckets = linkedMapOf<Int, Int>()
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            val c = bitmap.getPixel(x,y)
            val key = (Color.red(c)/16 shl 8) or (Color.green(c)/16 shl 4) or (Color.blue(c)/16)
            buckets[key] = (buckets[key] ?: 0) + 1
        }
        val ranked = buckets.entries.sortedByDescending { it.value }.map {
            Color.rgb(((it.key shr 8) and 15)*16+8, ((it.key shr 4) and 15)*16+8, (it.key and 15)*16+8)
        }
        val chosen = mutableListOf<Int>()
        for (c in ranked) {
            if (chosen.all { kotlin.math.abs(Color.red(it)-Color.red(c)) + kotlin.math.abs(Color.green(it)-Color.green(c)) + kotlin.math.abs(Color.blue(it)-Color.blue(c)) > 80 }) chosen.add(c)
            if (chosen.size == 3) break
        }
        val base = chosen.firstOrNull() ?: Color.BLACK
        val out = IntArray(3) { chosen.getOrNull(it) ?: base }
        saveColors(context, out, "Album Art")
    }

    fun savePreset(context: Context, index: Int) {
        val preset = presets[index.coerceIn(0, presets.lastIndex)]
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt("fallback", index.coerceIn(0, presets.lastIndex)).apply()
        if (!FlowSettings.loadAlbumColors(context) || AlbumArtStore.active(context) == null) saveColors(context, preset.colors, preset.name)
    }

    fun selectedName(context: Context): String {
        val p=context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return if(AlbumArtStore.active(context)!=null) "Album Art" else presets[p.getInt("fallback",7).coerceIn(0,presets.lastIndex)].name
    }

    fun load(context: Context): IntArray {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (AlbumArtStore.active(context) == null) return presets[p.getInt("fallback",7).coerceIn(0,presets.lastIndex)].colors
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
