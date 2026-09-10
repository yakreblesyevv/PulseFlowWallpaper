package com.pulseflow.wallpaper

import android.content.Context
import android.content.Intent

object FlowSettings {
    const val ACTION_SPEED_CHANGED = "com.pulseflow.wallpaper.SPEED_CHANGED"
    private const val PREF = "flow_settings"
    private const val KEY_SPEED = "speed"

    fun loadSpeed(context: Context): Float =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getFloat(KEY_SPEED, 1.0f)

    fun saveSpeed(context: Context, speed: Float) {
        val clamped = speed.coerceIn(0.15f, 2.5f)
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_SPEED, clamped)
            .apply()
        context.sendBroadcast(Intent(ACTION_SPEED_CHANGED).setPackage(context.packageName))
    }
}
