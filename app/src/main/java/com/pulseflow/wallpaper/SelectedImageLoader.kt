package com.pulseflow.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build

/** Reads only a user-selected document. No broad photo/storage permission. */
object SelectedImageLoader {
    fun load(context: Context, uri: Uri): Bitmap {
        val decoded = if(Build.VERSION.SDK_INT>=28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver,uri)) { decoder, info, _ ->
                decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
                val size=info.size
                val ratio=512f/maxOf(size.width,size.height).coerceAtLeast(1)
                if(ratio<1f) decoder.setTargetSize((size.width*ratio).toInt().coerceAtLeast(1),(size.height*ratio).toInt().coerceAtLeast(1))
            }
        } else {
            val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,bounds) }
            require(bounds.outWidth>0 && bounds.outHeight>0)
            var sample=1
            while(maxOf(bounds.outWidth,bounds.outHeight)/sample>512) sample*=2
            val options=BitmapFactory.Options().apply { inSampleSize=sample; inPreferredConfig=Bitmap.Config.ARGB_8888 }
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,options) }
                ?:error("Unreadable image")
        }
        return try { AlbumArtStore.prepare(decoded) } finally { decoded.recycle() }
    }
}
