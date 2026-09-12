package com.pulseflow.wallpaper

import android.app.Notification
import android.content.*
import android.graphics.BitmapFactory
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.*
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.Executors

class MusicNotificationListener : NotificationListenerService() {
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val controllers = linkedMapOf<String, Pair<MediaController, MediaController.Callback>>()
    private var generation = 0
    private var lastFingerprint = ""
    private var lastTextureHash: Int? = null
    private var connected = false
    private var manager: MediaSessionManager? = null
    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { refreshControllers() }
    private val settingsChanged = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { updateCurrent() }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        if (connected) return
        connected = true
        AlbumArtStore.initialize(this)
        val filter = IntentFilter(FlowSettings.ACTION_SETTINGS_CHANGED)
        registerReceiver(settingsChanged, filter, "$packageName.INTERNAL", null, RECEIVER_NOT_EXPORTED)
        manager = getSystemService(MediaSessionManager::class.java)
        runCatching { manager?.addOnActiveSessionsChangedListener(sessionsChanged, ComponentName(this, javaClass), main) }
        refreshControllers()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) { refreshControllers() }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) { refreshControllers() }

    private fun refreshControllers() {
        if (!connected) return
        val active = runCatching { manager?.getActiveSessions(ComponentName(this, javaClass)).orEmpty() }.getOrDefault(emptyList())
        val notifications = runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())
        val fromNotifications = notifications.mapNotNull { sbn ->
            token(sbn.notification)?.let { runCatching { MediaController(this, it) }.getOrNull() }
        }
        val all = (active + fromNotifications).distinctBy { it.sessionToken }
        val keys = all.map { it.sessionToken.toString() }.toSet()
        controllers.keys.toList().filter { it !in keys }.forEach { key ->
            controllers.remove(key)?.let { (c, callback) -> runCatching { c.unregisterCallback(callback) } }
        }
        all.forEach { c ->
            val key = c.sessionToken.toString()
            if (key !in controllers) {
                val callback = object : MediaController.Callback() {
                    override fun onMetadataChanged(m: MediaMetadata?) { updateCurrent() }
                    override fun onPlaybackStateChanged(s: PlaybackState?) { updateCurrent() }
                    override fun onSessionDestroyed() { refreshControllers() }
                }
                runCatching { c.registerCallback(callback, main) }
                controllers[key] = c to callback
            }
        }
        updateCurrent()
    }

    private fun updateCurrent() {
        if (!connected) return
        val all = controllers.values.map { it.first }
        val c = all.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: all.firstOrNull { it.metadata != null }
        val notifications = runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())
        val sbn = notifications.filter { token(it.notification) != null || it.notification.category == Notification.CATEGORY_TRANSPORT }
            .sortedByDescending { it.postTime }.let { items -> if (c == null) items.firstOrNull() else
                items.firstOrNull { token(it.notification) == c.sessionToken }
                    ?: items.firstOrNull { it.packageName == c.packageName } }
        val m = c?.metadata
        val n = sbn?.notification
        val isPlaying = c?.playbackState?.state == PlaybackState.STATE_PLAYING
        val changedPlaying = AlbumArtStore.playing != isPlaying
        AlbumArtStore.playing = isPlaying
        val title = m?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: n?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val artist = m?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: n?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        getSharedPreferences("now_playing", MODE_PRIVATE).edit().putString("title", title)
            .putString("artist", artist).putBoolean("playing", isPlaying).apply()
        if (!FlowSettings.loadAlbumColors(this)) {
            generation++
            lastFingerprint = ""
            lastTextureHash = null
            return
        }
        val art = m?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: m?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: notificationArt(n)
            ?: m?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val coverUris = listOfNotNull(
            m?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI),
            m?.getString(MediaMetadata.METADATA_KEY_ART_URI),
            m?.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
        ).distinct().filter { Uri.parse(it).scheme == "content" }
        val fingerprint = "${c?.packageName ?: sbn?.packageName}|$title|$artist|${art?.generationId}|${art?.width}|${coverUris.joinToString()}"
        if (art == null && coverUris.isEmpty()) {
            // Cancel an older cover job while the next track is loading its artwork.
            generation++
            lastFingerprint = ""
        }
        if ((art != null || coverUris.isNotEmpty()) && fingerprint != lastFingerprint) {
            lastFingerprint = fingerprint
            val request = ++generation
            worker.execute {
                val loaded = if (art == null) coverUris.firstNotNullOfOrNull { loadContentCover(it) } else null
                val prepared = runCatching { (art ?: loaded)?.let { AlbumArtStore.prepare(it) } }.getOrNull()
                loaded?.recycle()
                main.post {
                    if (request == generation && prepared == null) lastFingerprint = ""
                    if (request == generation && prepared != null && connected && FlowSettings.loadAlbumColors(this)) {
                        val pixels=IntArray(4096)
                        prepared.getPixels(pixels,0,64,0,0,64,64)
                        val hash=pixels.contentHashCode()
                        if(lastTextureHash != hash) {
                            lastTextureHash=hash
                            AlbumArtStore.publish(this, prepared)
                            PaletteStore.save(this, prepared)
                        }
                    }
                }
            }
        }
        if (changedPlaying) sendBroadcast(Intent(PaletteStore.ACTION_PALETTE).setPackage(packageName))
    }

    // Players may publish a granted content URI instead of an embedded bitmap.
    // Decode a bounded image on the worker, keeping provider I/O away from the UI.
    private fun loadContentCover(value: String): Bitmap? = runCatching {
        val uri = Uri.parse(value)
        if (uri.scheme != "content") return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 512) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()

    private fun token(n: Notification): MediaSession.Token? = runCatching {
        if (Build.VERSION.SDK_INT >= 33) n.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
        else { @Suppress("DEPRECATION") n.extras.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION) }
    }.getOrNull()

    private fun notificationArt(n: Notification?): Bitmap? = runCatching {
        val drawable = n?.getLargeIcon()?.loadDrawable(this) ?: return null
        if (drawable is BitmapDrawable) return drawable.bitmap
        Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888).also {
            drawable.setBounds(0,0,128,128); drawable.draw(Canvas(it))
        }
    }.getOrNull()

    private fun disconnect() {
        if (!connected) return
        connected = false
        AlbumArtStore.playing = false
        sendBroadcast(Intent(PaletteStore.ACTION_PALETTE).setPackage(packageName))
        generation++
        runCatching { unregisterReceiver(settingsChanged) }
        runCatching { manager?.removeOnActiveSessionsChangedListener(sessionsChanged) }
        controllers.values.forEach { (c, callback) -> runCatching { c.unregisterCallback(callback) } }
        controllers.clear()
    }
    override fun onListenerDisconnected() { disconnect(); super.onListenerDisconnected() }
    override fun onDestroy() { disconnect(); worker.shutdown(); super.onDestroy() }
}
