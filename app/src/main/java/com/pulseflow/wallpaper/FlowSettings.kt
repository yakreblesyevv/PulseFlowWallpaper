package com.pulseflow.wallpaper

import android.content.Context
import android.content.Intent

object FlowSettings {
    const val ACTION_SETTINGS_CHANGED = "com.pulseflow.wallpaper.SETTINGS_CHANGED"
    private const val PREF = "flow_settings"

    fun loadSpeed(context: Context) = prefs(context).getFloat("speed", 1.0f)
    fun loadSpeedMin(context: Context) = prefs(context).getFloat("speed_min", 0.45f)
    fun loadSpeedMax(context: Context) = prefs(context).getFloat("speed_max", 2.0f)
    fun loadScale(context: Context) = prefs(context).getFloat("scale", 1.0f)
    fun loadBrightness(context: Context) = prefs(context).getFloat("brightness", 1.0f)
    fun loadBlur(context: Context) = prefs(context).getFloat("blur", 0.72f)
    fun loadGraphicsMode(context: Context) = prefs(context).getString("graphics_mode", "blur") ?: "blur"
    fun loadPreserveReboot(context: Context) = prefs(context).getBoolean("preserve_reboot", true)
    fun loadPreservePause(context: Context) = prefs(context).getBoolean("preserve_pause", true)
    fun loadAdaptiveColors(context: Context) = prefs(context).getBoolean("adaptive_colors", false)
    fun loadPerformanceMode(context: Context) = prefs(context).getBoolean("performance_mode", false)
    fun loadDebugView(context: Context) = prefs(context).getBoolean("debug_view", false)
    fun loadLiveBeats(context: Context) = prefs(context).getBoolean("live_beats", false)
    fun loadBeatStrength(context: Context) = prefs(context).getFloat("beat_strength", 0.55f)
    fun loadAlbumColors(context: Context) = prefs(context).getBoolean("album_colors", false)

    fun saveSpeed(context: Context, value: Float) = saveFloat(context, "speed", value.coerceIn(0.05f, 4.5f))
    fun saveSpeedRange(context: Context, min: Float, max: Float) {
        val lo = min.coerceIn(0.05f, 4.49f)
        val hi = max.coerceIn(lo + 0.01f, 4.5f)
        prefs(context).edit().putFloat("speed_min", lo).putFloat("speed_max", hi).apply()
        broadcast(context)
    }
    fun saveScale(context: Context, value: Float) = saveFloat(context, "scale", value.coerceIn(0.65f, 1.65f))
    fun saveBrightness(context: Context, value: Float) = saveFloat(context, "brightness", value.coerceIn(0.35f, 1.35f))
    fun saveBlur(context: Context, value: Float) = saveFloat(context, "blur", value.coerceIn(0.25f, 1.0f))
    fun saveGraphicsMode(context: Context, value: String) = saveString(context, "graphics_mode", value)
    fun savePreserveReboot(context: Context, value: Boolean) = saveBool(context, "preserve_reboot", value)
    fun savePreservePause(context: Context, value: Boolean) = saveBool(context, "preserve_pause", value)
    fun saveAdaptiveColors(context: Context, value: Boolean) = saveBool(context, "adaptive_colors", value)
    fun savePerformanceMode(context: Context, value: Boolean) = saveBool(context, "performance_mode", value)
    fun saveDebugView(context: Context, value: Boolean) = saveBool(context, "debug_view", value)
    fun saveLiveBeats(context: Context, value: Boolean) = saveBool(context, "live_beats", value)
    fun saveBeatStrength(context: Context, value: Float) = saveFloat(context, "beat_strength", value.coerceIn(0f, 1f))
    fun saveAlbumColors(context: Context, value: Boolean) = saveBool(context, "album_colors", value)

    private fun prefs(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    private fun broadcast(context: Context) {
        context.sendBroadcast(Intent(ACTION_SETTINGS_CHANGED).setPackage(context.packageName))
    }

    private fun saveFloat(context: Context, key: String, value: Float) {
        prefs(context).edit().putFloat(key, value).apply()
        broadcast(context)
    }
    private fun saveBool(context: Context, key: String, value: Boolean) {
        prefs(context).edit().putBoolean(key, value).apply()
        broadcast(context)
    }
    private fun saveString(context: Context, key: String, value: String) {
        prefs(context).edit().putString(key, value).apply()
        broadcast(context)
    }
}
