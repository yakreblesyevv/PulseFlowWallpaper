# PulseFlow v0.22 — manual image test build

The user's phone blocked v0.21 in Play Protect after notification-listener access was restored. v0.22 genuinely removes that capability: no notification listener service, notification settings link, notification metadata access, SMS or accessibility access. This is not a claim of Play Protect approval.

## What works in this build

- Select a photo/cover with **GALERİDEN GÖRSEL SEÇ**. Only that document is read; no broad storage/photo permission is requested.
- Its colors feed the same liquid texture renderer, including gray and dark colors. Presets remain available.
- Speed range, scale, softness, brightness, fluted glass and full-screen preview remain.
- Optional Live Beats retains Android Visualizer audio analysis and the audio permission. Phone/player compatibility still requires device testing.
- Optional image persistence across reboot, adaptive wallpaper colors, and debug display remain.

**Automatic album changes are unavailable in v0.22.** Changing the song does not change the selected image. v0.21's implementation remains in Git history for future reviewed distribution.

## Build and checks

JDK 17, Gradle 8.11.1, Android SDK 35/build-tools 35.0.0:

```sh
gradle :app:assembleRelease :app:lintRelease
```

Actions signs using the unchanged AOSP test key and package `com.pulseflow.wallpaper.dev`, versionCode 22. This preserves the existing development update channel; it is not a production signing identity.

Optional portable shader test: `python3 tests/verify_shader.py` (requires skia-python and numpy).

The shader is unchanged from v0.21. Android 13+ uses the GPU liquid renderer; Android 8–12 use the existing Canvas fallback. Device installation, image picker and audio behavior remain to be tested on the user's phone.
