package com.pulseflow.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import android.app.Activity

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(32,48,32,32); setBackgroundColor(Color.rgb(7,8,14)); gravity=Gravity.CENTER_HORIZONTAL }
        root.addView(TextView(this).apply { text="PULSEFLOW"; textSize=28f; setTextColor(Color.WHITE); gravity=Gravity.CENTER }, LinearLayout.LayoutParams(-1,-2))
        root.addView(TextView(this).apply { text="Music-driven live wallpaper"; textSize=15f; setTextColor(Color.LTGRAY); gravity=Gravity.CENTER; setPadding(0,8,0,24) })
        root.addView(FlowPreviewView(this), LinearLayout.LayoutParams(-1,0,1f))
        val prefs=getSharedPreferences("now_playing",MODE_PRIVATE)
        root.addView(TextView(this).apply { text="Now playing: ${prefs.getString("title","Waiting for music")}  ${prefs.getString("artist","")}"; setTextColor(Color.WHITE); setPadding(0,22,0,16) })
        root.addView(Button(this).apply { text="1. Enable Music Access"; setOnClickListener { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) } }, LinearLayout.LayoutParams(-1,-2))
        root.addView(Button(this).apply { text="2. Set Live Wallpaper"; setOnClickListener { startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this@MainActivity, PulseWallpaperService::class.java))) } }, LinearLayout.LayoutParams(-1,-2))
        setContentView(root)
    }
}
