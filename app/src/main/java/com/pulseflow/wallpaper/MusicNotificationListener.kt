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

    override fun onListenerConnected() {
        super.onListenerConnected()
        if (!FlowSettings.loadAlbumColors(this)) return
        activeNotifications
            ?.sortedByDescending { it.postTime }
            ?.firstOrNull { isMediaNotification(it.notification) }
            ?.let { process(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (!FlowSettings.loadAlbumColors(this)) return
        sbn?.let { process(it) }
    }

    private fun process(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        if (!isMediaNotification(notification)) return

        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val artist = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        getSharedPreferences("now_playing", MODE_PRIVATE)
            .edit()
            .putString("title", title)
            .putString("artist", artist)
            .apply()

        extractArtwork(notification)?.let { PaletteStore.save(this, it) }
    }

    private fun isMediaNotification(notification: Notification): Boolean {
        val mediaToken = if (Build.VERSION.SDK_INT >= 33) {
            notification.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
        } else {
            @Suppress("DEPRECATION") notification.extras.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)
        }
        return notification.category == Notification.CATEGORY_TRANSPORT || mediaToken != null
    }

    private fun extractArtwork(notification: Notification): Bitmap? {
        if (Build.VERSION.SDK_INT >= 33) {
            notification.extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG, Bitmap::class.java)?.let { return it }
            notification.extras.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap::class.java)?.let { return it }
        } else {
            @Suppress("DEPRECATION") notification.extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON_BIG)?.let { return it }
            @Suppress("DEPRECATION") notification.extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON)?.let { return it }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notification.getLargeIcon()?.let { icon ->
                iconToBitmap(icon)?.let { return it }
            }
        }
        return null
    }

    private fun iconToBitmap(icon: Icon): Bitmap? {
        val drawable = try { icon.loadDrawable(this) } catch (_: Exception) { null } ?: return null
        return drawableToBitmap(drawable)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 512
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 512
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }
    }
}
