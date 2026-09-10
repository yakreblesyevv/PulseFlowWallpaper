package com.pulseflow.wallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(Color.rgb(7, 8, 14))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        root.addView(TextView(this).apply {
            text = "PULSEFLOW"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2))

        root.addView(TextView(this).apply {
            text = "Fluid live wallpaper"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 20)
        })

        val preview = FlowPreviewView(this)
        root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f))

        val speedLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 4)
        }

        val currentSpeed = FlowSettings.loadSpeed(this)
        speedLabel.text = "Akış hızı: %.1fx".format(currentSpeed)
        root.addView(speedLabel, LinearLayout.LayoutParams(-1, -2))

        val speedBar = SeekBar(this).apply {
            max = 100
            progress = (((currentSpeed - 0.15f) / (2.5f - 0.15f)) * 100f).toInt().coerceIn(0, 100)
            setPadding(12, 0, 12, 10)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val speed = 0.15f + (progress / 100f) * (2.5f - 0.15f)
                    preview.setFlowSpeed(speed)
                    speedLabel.text = "Akış hızı: %.1fx".format(speed)
                    FlowSettings.saveSpeed(this@MainActivity, speed)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        root.addView(speedBar, LinearLayout.LayoutParams(-1, -2))

        root.addView(TextView(this).apply {
            text = "Sola: daha sakin   •   Sağa: daha hızlı"
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            textSize = 13f
            setPadding(0, 0, 0, 14)
        }, LinearLayout.LayoutParams(-1, -2))

        root.addView(Button(this).apply {
            text = "Set Live Wallpaper"
            setOnClickListener {
                startActivity(
                    Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(this@MainActivity, PulseWallpaperService::class.java)
                    )
                )
            }
        }, LinearLayout.LayoutParams(-1, -2))

        setContentView(root)
    }
}
