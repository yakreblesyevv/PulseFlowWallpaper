package com.pulseflow.wallpaper

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.provider.Settings
import android.util.AtomicFile
import java.io.File
import java.util.concurrent.Executors

/** Small immutable texture shared by previews and wallpaper. Never recycle shared bitmaps. */
object AlbumArtStore {
    @Volatile var texture: Bitmap? = null
        private set
    @Volatile var revision = 0L
        private set
    @Volatile var playing = false
    private var loaded = false
    private val disk = Executors.newSingleThreadExecutor()
    private fun file(c: Context) = AtomicFile(File(c.filesDir, "album-texture.png"))

    @Synchronized fun initialize(c: Context) {
        if (loaded) return
        loaded = true
        val p = c.getSharedPreferences("album_cache", Context.MODE_PRIVATE)
        val boot = Settings.Global.getInt(c.contentResolver, Settings.Global.BOOT_COUNT, -1)
        if (FlowSettings.loadPreserveReboot(c) || p.getInt("boot", -2) == boot) {
            texture = runCatching { file(c).openRead().use { BitmapFactory.decodeStream(it) } }.getOrNull()
            revision++
        }
    }

    fun active(c: Context): Bitmap? {
        initialize(c)
        return if (FlowSettings.loadAlbumColors(c) && (playing || FlowSettings.loadPreservePause(c))) texture else null
    }

    fun publish(c: Context, bitmap: Bitmap) {
        texture = bitmap
        revision++
        val app = c.applicationContext
        val boot = Settings.Global.getInt(c.contentResolver, Settings.Global.BOOT_COUNT, -1)
        disk.execute {
            val f = file(app)
            var out: java.io.FileOutputStream? = null
            try {
                out = f.startWrite()
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
                f.finishWrite(out)
                app.getSharedPreferences("album_cache", Context.MODE_PRIVATE).edit().putInt("boot", boot).apply()
            } catch (_: Exception) { out?.let { f.failWrite(it) } }
        }
    }

    /** Downsample the whole cover; retain neutral and dark pixels, without a preset palette. */
    fun prepare(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val software = if (source.config == Bitmap.Config.HARDWARE) source.copy(Bitmap.Config.ARGB_8888, false) else source
        Canvas(result).drawBitmap(software, null, Rect(0, 0, 64, 64), Paint(Paint.FILTER_BITMAP_FLAG))
        if (software !== source) software?.recycle()
        // Two small box passes remove readable cover details but preserve spatial color distribution.
        var pixels = IntArray(4096)
        result.getPixels(pixels, 0, 64, 0, 0, 64, 64)
        repeat(2) {
            val output = IntArray(4096)
            for (y in 0..63) for (x in 0..63) {
                var r=0; var g=0; var b=0
                for (dy in -3..3) for (dx in -3..3) {
                    val color = pixels[(y+dy).coerceIn(0,63)*64+(x+dx).coerceIn(0,63)]
                    r+=Color.red(color); g+=Color.green(color); b+=Color.blue(color)
                }
                output[y*64+x]=Color.rgb(r/49,g/49,b/49)
            }
            pixels=output
        }
        result.setPixels(pixels,0,64,0,0,64,64)
        return result
    }
}
