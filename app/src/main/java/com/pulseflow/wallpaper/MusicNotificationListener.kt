package com.pulseflow.wallpaper

import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MusicNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val n = sbn?.notification ?: return
        if (n.category != android.app.Notification.CATEGORY_TRANSPORT && n.extras.getParcelable<android.media.session.MediaSession.Token>(android.app.Notification.EXTRA_MEDIA_SESSION) == null) return
        val title = n.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
        val artist = n.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
        getSharedPreferences("now_playing", MODE_PRIVATE).edit().putString("title", title).putString("artist", artist).apply()
        val icon: Icon? = n.largeIcon
        val drawable = try { icon?.loadDrawable(this) } catch (_: Exception) { null }
        val bitmap = drawable?.let {
            val w = maxOf(1, it.intrinsicWidth); val h = maxOf(1, it.intrinsicHeight)
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { b -> val c = android.graphics.Canvas(b); it.setBounds(0,0,w,h); it.draw(c) }
        }
        PaletteStore.save(this, bitmap)
    }
}
