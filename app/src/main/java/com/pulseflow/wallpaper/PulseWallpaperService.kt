package com.pulseflow.wallpaper

import android.content.*
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class PulseWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = FlowEngine()
    inner class FlowEngine : Engine() {
        private val renderer=FlowRenderer(); private val handler=android.os.Handler(mainLooper); private var visible=false
        private val tick=object:Runnable{override fun run(){draw();if(visible)handler.postDelayed(this,16)}}
        private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){reload()}}
        private fun reload(){renderer.colors=PaletteStore.load(this@PulseWallpaperService);renderer.speed=FlowSettings.loadSpeed(this@PulseWallpaperService);renderer.scale=FlowSettings.loadScale(this@PulseWallpaperService);renderer.brightness=FlowSettings.loadBrightness(this@PulseWallpaperService);renderer.blur=FlowSettings.loadBlur(this@PulseWallpaperService)}
        override fun onCreate(holder:SurfaceHolder?){super.onCreate(holder);reload();val f=IntentFilter().apply{addAction(PaletteStore.ACTION_PALETTE);addAction(FlowSettings.ACTION_SETTINGS_CHANGED)};if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,RECEIVER_NOT_EXPORTED)else{@Suppress("DEPRECATION") registerReceiver(receiver,f)}}
        override fun onVisibilityChanged(v:Boolean){visible=v;handler.removeCallbacks(tick);if(v)handler.post(tick)}
        private fun draw(){var c:android.graphics.Canvas?=null;try{c=if(Build.VERSION.SDK_INT>=26)surfaceHolder.lockHardwareCanvas() else surfaceHolder.lockCanvas();if(c!=null)renderer.draw(c)}catch(_:Throwable){if(c!=null){try{surfaceHolder.unlockCanvasAndPost(c)}catch(_:Throwable){};c=null};try{c=surfaceHolder.lockCanvas();if(c!=null)renderer.draw(c)}catch(_:Throwable){}}finally{if(c!=null)try{surfaceHolder.unlockCanvasAndPost(c)}catch(_:Throwable){}}}
        override fun onDestroy(){visible=false;handler.removeCallbacks(tick);unregisterReceiver(receiver);super.onDestroy()}
    }
}
