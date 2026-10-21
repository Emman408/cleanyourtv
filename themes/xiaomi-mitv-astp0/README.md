# Xiaomi MiTV-ASTP0 native controls

Tested with internal model `MiTV-ASTP0` (`mulan`), Android 9/API 28,
firmware `24.7.4.3982`, and the matching CleanYourTV control fix.
See the [device control record](../../docs/devices/xiaomi-mitv-astp0-controls.md).

This overlay opens Xiaomi's own signal-source selector from the Inputs pill,
and Xiaomi's picture/sound/source settings from a short MENU press on Home.
It leaves the launcher's appearance and app arrangement at their existing defaults.
No accessibility service is needed for the MENU key while Home is foreground.

The compatible APK uses `com.ts.launcher`, so install this JSON at
`/sdcard/Android/data/com.ts.launcher/files/config.json`, then restart the launcher.
The normal theme helper targets `com.gathin.tvhome` and does not install this
alternate-package profile automatically. Back up an existing overlay and merge
these keys if it contains personal settings.

For a first profile installation, with Android platform-tools connected to the TV:

```bash
adb shell mkdir -p /sdcard/Android/data/com.ts.launcher/files
adb push themes/xiaomi-mitv-astp0/config.json /sdcard/Android/data/com.ts.launcher/files/config.json
adb shell am force-stop com.ts.launcher
adb shell am start -n com.ts.launcher/com.gathin.tvhome.MainActivity
```

Only use these vendor components on matching firmware. If they are absent, the
launcher reports that Inputs is unavailable, or falls back to normal Settings
for the configured settings key. Other themes keep their existing behavior.
