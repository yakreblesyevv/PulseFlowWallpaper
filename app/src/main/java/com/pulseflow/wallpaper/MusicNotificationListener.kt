package com.pulseflow.wallpaper

import android.app.Notification
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.media.session.MediaSession
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MusicNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val mediaToken = notification.extras.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)
        if (notification.category != Notification.CATEGORY_TRANSPORT && mediaToken == null) return

        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val artist = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        getSharedPreferences("now_playing", MODE_PRIVATE)
            .edit()
            .putString("title", title)
            .putString("artist", artist)
            .apply()

        val artwork = extractArtwork(notification)
        PaletteStore.save(this, artwork)
    }

    private fun extractArtwork(notification: Notification): Bitmap? {
        notification.extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON_BIG)?.let { return it }
        notification.extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON)?.let { return it }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notification.getLargeIcon()?.let { icon ->
                iconToBitmap(icon)?.let { return it }
            }
        }
        return null
    }

    private fun iconToBitmap(icon: Icon): Bitmap? {
        val drawable = try {
            icon.loadDrawable(this)
        } catch (_: Exception) {
            null
        } ?: return null

        return drawableToBitmap(drawable)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 512
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 512
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }
    }
}
