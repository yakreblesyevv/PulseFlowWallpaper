package com.pulseflow.wallpaper

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import kotlin.concurrent.thread

class PlaybackCaptureService : Service() {
    companion object {
        const val ACTION_START = "com.pulseflow.wallpaper.START_PLAYBACK_CAPTURE"
        const val ACTION_STOP = "com.pulseflow.wallpaper.STOP_PLAYBACK_CAPTURE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "result_data"
        private const val CHANNEL_ID = "pulseflow_live_beats"
        private const val NOTIFICATION_ID = 1401
    }

    @Volatile private var running = false
    private var projection: MediaProjection? = null
    private var recorder: AudioRecord? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCapture()
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action != ACTION_START || Build.VERSION.SDK_INT < 29) return START_NOT_STICKY

        startForeground(NOTIFICATION_ID, buildNotification())
        val code = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_DATA)
        }
        if (code != Activity.RESULT_OK || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startCapture(code, data)
        return START_NOT_STICKY
    }

    @RequiresApi(29)
    private fun startCapture(resultCode: Int, data: Intent) {
        stopCapture()
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mp = manager.getMediaProjection(resultCode, data)
        projection = mp
        mp.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopCapture()
                stopSelf()
            }
        }, android.os.Handler(mainLooper))

        val config = AudioPlaybackCaptureConfiguration.Builder(mp)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        val sampleRate = 44100
        val channelMask = AudioFormat.CHANNEL_IN_STEREO
        val format = AudioFormat.ENCODING_PCM_16BIT
        val min = AudioRecord.getMinBufferSize(sampleRate, channelMask, format)
        val bufferSize = maxOf(min, sampleRate / 5 * 4)

        val record = AudioRecord.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(format)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(config)
            .build()
        recorder = record
        running = true
        record.startRecording()

        thread(name = "PulseFlowPlaybackCapture", isDaemon = true) {
            val buffer = ShortArray(4096)
            while (running) {
                val n = try { record.read(buffer, 0, buffer.size) } catch (_: Throwable) { -1 }
                if (n > 0) BeatAnalyzer.pushPcm(buffer, n) else BeatAnalyzer.silence()
            }
        }
    }

    private fun stopCapture() {
        running = false
        try { recorder?.stop() } catch (_: Throwable) {}
        try { recorder?.release() } catch (_: Throwable) {}
        recorder = null
        try { projection?.stop() } catch (_: Throwable) {}
        projection = null
        BeatAnalyzer.stop()
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "PulseFlow Live Beats", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, PlaybackCaptureService::class.java).apply { action = ACTION_STOP }
        val pending = PendingIntent.getService(
            this, 14, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("PulseFlow Live Beats aktif")
            .setContentText("Müzik ritmi duvar kağıdına aktarılıyor")
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Durdur", pending).build())
            .build()
    }
}
