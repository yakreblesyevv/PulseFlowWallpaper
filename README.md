# PulseFlow Wallpaper

Native Android music-reactive live wallpaper.

## v0.23 decision

The user explicitly rejected gallery/manual image selection and requested Diffuse-style automatic album artwork. This build restores the v0.21 notification/media-session path and removes the v0.22 document picker and its loader completely. Album art is obtained only after notification access is granted and automatic colors are enabled. This is the feature path described by Diffuse's public store listing; its proprietary renderer is not copied.

**Known installation limitation:** the user's phone blocked v0.21 in Play Protect. v0.23 retains the necessary notification-listener declaration and does not resolve or claim to bypass that installation block. Rebuilding and incrementing the version cannot establish approval. Distribution/review remains unresolved. Do not reintroduce gallery selection as a substitute for automatic synchronization without the user's instruction.

 v0.23 restores album artwork sync while retaining the existing package (`com.pulseflow.wallpaper.dev`) and update channel.

## Use

1. Install the APK as an update to v0.9 or newer from the stable test channel.
2. Enable **Albüm kapağından otomatik renkler**, then grant PulseFlow notification access in Android settings.
3. Start a track in your music player. The settings screen displays its title and artist.
4. Optionally enable **Live Beats** and grant audio permission. **Debug View** shows whether the system audio visualizer is receiving a non-silent signal.
5. Set PulseFlow as the live wallpaper. Use full-screen preview to inspect the result.

## Automatic artwork behavior

- Restores the notification listener manifest entry, listens to media-session metadata and playback changes, and prefers the playing session.
- Uses the album's spatial colors as a blurred texture in the AGSL liquid field; preserves gray/dark covers without injecting preset hues.
- Crossfades artwork over 1.6 seconds, preserves the last cover according to pause/reboot options, and treats presets as fallback colors.
- Advances animation time incrementally so speed slider changes do not reset the current phase.
- Connects adaptive wallpaper colors and debug display to their Android behavior; releases audio analysis when no visible surface needs it.
- Adds a full-screen preview and fixes speed-range endpoint constraints.

## Build

JDK 17, Gradle 8.11.1, Android SDK 35/build-tools 35.0.0:

```sh
gradle :app:assembleRelease :app:lintRelease
```

The existing Actions workflow signs with the same public AOSP test key used by prior test APKs. This remains a development channel, not a production signing identity.

Portable rendering regression check:

```sh
python3 -m pip install skia-python numpy
python3 tests/verify_shader.py
```

This compiles the actual shader and checks neutral colors, animation continuity, beat deformation and glass mode. It does not simulate Android notification permissions or real music-player sessions.

## Device verification remaining

Spotify / YouTube Music artwork delivery, pause/resume, reboot persistence, and audio visualizer availability must be checked on the target phone. Android audio capture behavior varies by player/device. No silent input is presented as an active beat signal. The GPU liquid renderer requires Android 13+; Android 8–12 use the existing Canvas fallback. Visual similarity to Diffuse remains a reference target, not a pixel-identical claim.
