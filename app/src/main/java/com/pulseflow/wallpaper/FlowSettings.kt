package com.pulseflow.wallpaper

import android.content.Context
import android.content.Intent

object FlowSettings {
    const val ACTION_SETTINGS_CHANGED = "com.pulseflow.wallpaper.SETTINGS_CHANGED"
    private const val PREF = "flow_settings"

    fun loadSpeed(context: Context) = prefs(context).getFloat("speed", 1.0f)
    fun loadScale(context: Context) = prefs(context).getFloat("scale", 1.0f)
    fun loadBrightness(context: Context) = prefs(context).getFloat("brightness", 1.0f)
    fun loadBlur(context: Context) = prefs(context).getFloat("blur", 0.72f)

    fun saveSpeed(context: Context, value: Float) = save(context, "speed", value.coerceIn(0.12f, 2.5f))
    fun saveScale(context: Context, value: Float) = save(context, "scale", value.coerceIn(0.65f, 1.65f))
    fun saveBrightness(context: Context, value: Float) = save(context, "brightness", value.coerceIn(0.35f, 1.35f))
    fun saveBlur(context: Context, value: Float) = save(context, "blur", value.coerceIn(0.25f, 1.0f))

    private fun prefs(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun save(context: Context, key: String, value: Float) {
        prefs(context).edit().putFloat(key, value).apply()
        context.sendBroadcast(Intent(ACTION_SETTINGS_CHANGED).setPackage(context.packageName))
    }
}
