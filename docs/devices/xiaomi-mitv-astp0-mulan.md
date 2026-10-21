# Xiaomi MiTV-ASTP0 (mulan): Home after standby wake

Follow-up: [native inputs, short MENU and Home chooser recovery](xiaomi-mitv-astp0-controls.md)
records the later stock-Home update and the additional control fixes.

小米 MiTV-ASTP0（mulan），Android 9，固件 24.7.4.3982。遥控器关机后再开机
实际进入的是待机唤醒，系统会绕过默认桌面启动小米原桌面。本记录对应已在这台电视
实测的兼容方案：使用兼容包名、停用原桌面，并处理小米的待机唤醒广播。日常使用
无需开启 ADB。断电冷启动、其他固件和长期使用尚未验证。

## Tested device

| Field | Recorded value |
|---|---|
| Manufacturer / internal model | Xiaomi / `MiTV-ASTP0` |
| Device codename | `mulan` |
| Android | 9 / API 28 |
| Firmware | `24.7.4.3982` |
| Stock Home | `com.mitv.tvhome/.MainActivityUserMode` |
| Stock Home version | `3.46.1185` |
| Tested launcher | CleanYourTV 2.4 (`versionCode 5`) |
| Compatible application ID | `com.ts.launcher` |
| Compatible Home component | `com.ts.launcher/com.gathin.tvhome.MainActivity` |
| Compatibility build | compile SDK 34, min SDK 26, target SDK 25 |
| Test date | 2026-10-21 |

`MiTV-ASTP0` is the internal model reported by the device, not a verified retail
screen-size/model name. This is a launcher compatibility record. No general
debloat list or long-term package-removal recommendation is provided.

## Observed behavior and cause

1. The standard `com.gathin.tvhome` APK ran, but setting it as Home reported
   `Success` while HOME still resolved to Xiaomi. The alternate identity
   `com.ts.launcher` was accepted, as on the separately documented
   [MiTV-AETP0](xiaomi-mitv-aetp0-dofus.md). It was not already installed here.
2. With Xiaomi Home enabled, the Home key opened the new launcher, but the remote
   power-key off/on cycle restored Xiaomi Home and its advertising screen.
   The boot counter did not change: this was suspend-to-RAM (STR) wake, not a cold
   boot. Device logs and framework inspection showed `wakeUpCommonProcess`
   explicitly starting Xiaomi's `BootUpService`, followed by its `BootActivity`.
3. Disabling the stock launcher for user 0 stopped that takeover on this firmware.
   Individual system-app components could not be disabled: the firmware rejected
   shell component changes with `SecurityException`.
4. The firmware sends `mitv.action.STR_BOOT_COMPLETED` without
   `FLAG_RECEIVER_INCLUDE_BACKGROUND`. With target SDK 34, Android rejected delivery
   to the manifest receiver as `Background execution not allowed`. The opt-in
   target SDK 25 profile allowed delivery on this Android 9 device.
5. The receiver opens the launcher only if it is still the selected default Home.
   It accepts system boot and the Xiaomi STR action, requires the sender's
   `android.permission.DEVICE_POWER`, and uses no service, polling or accessibility
   redirection. The default build retains target SDK 34.

## Build and install

Use this profile only for matching, inspected firmware. `com.ts.launcher` is also
another application's package name; do not overwrite an existing application
with that ID. This is an unofficial compatibility APK, signed with the local
Android debug key used for sideloading. Other builders' APKs may have a different
signature. The legacy target is for this Android 9 installation, not Play Store
publication or a general recommendation for newer Android versions.

With the repository's normal JDK 17, Android SDK 34/build-tools 34.0.0, and
`tools/venv` dependencies ready, run from the repository root:

```bash
export TV_HOST='<your-TV-IP>'
PY=tools/venv/bin/python
"$PY" -I tools/tv.py 'pm path com.ts.launcher'
"$PY" -I tools/tv.py 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME'
```

For a first install, proceed only if `pm path com.ts.launcher` finds no installed
package. For an update to this compatibility build, first verify its identity
and signing certificate. Record the original Home before changing it.

```bash
(cd launcher && ./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease \
  -PlauncherApplicationId=com.ts.launcher -PlegacyXiaomiWake=true)
"$PY" -I tools/b64push.py launcher/app/build/outputs/apk/release/app-release.apk /data/local/tmp/tvhome-astp0.apk
"$PY" -I tools/tv.py 'pm install -r /data/local/tmp/tvhome-astp0.apk'
"$PY" -I tools/tv.py 'am start -W -n com.ts.launcher/com.gathin.tvhome.MainActivity'
```

Verify that the launcher and remote work before changing Home:

```bash
"$PY" -I tools/tv.py 'cmd package set-home-activity --user 0 com.ts.launcher/com.gathin.tvhome.MainActivity'
"$PY" -I tools/tv.py 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME'
"$PY" -I tools/tv.py 'input keyevent KEYCODE_HOME'
```

The expected resolved component is
`com.ts.launcher/com.gathin.tvhome.MainActivity`. Only after that check, the tested
ASTP0 configuration disables the installed stock launcher for user 0:

```bash
"$PY" -I tools/tv.py 'pm disable-user --user 0 com.mitv.tvhome'
"$PY" -I tools/tv.py 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME'
"$PY" -I tools/tv.py 'input keyevent KEYCODE_HOME'
```

This differs from the AETP0 record, which keeps stock Home enabled. Do not apply
the ASTP0 disable step to other models without verifying their fallback behavior.
The stock package remains installed and can be re-enabled.

The normal build/install/theme/restore helpers use `com.gathin.tvhome`; use the
explicit compatible component above for this build. An optional theme overlay
belongs at `/sdcard/Android/data/com.ts.launcher/files/config.json`.

## Physical verification and limits

The physical test used upstream snapshot `687b5ee2a3f3` plus this wake fix,
preserved as fork commit `6392b6da0cde` and its matching release APK. Builds
against later upstream sources were not installed as part of this device test.

The owner verified the Home key and D-pad. After installing the wake fix, a remote
power-key off, five-second wait, and power-key on entered the new launcher without
pressing Home or Back. A settings page left open during maintenance initially
flashed. Opening Settings from the launcher and backing all the way out cleared
that old task; the next off/on cycle no longer flashed Settings. The final test
required no ADB reconnection or developer-options change.

The tested result is standby/wake entry into the new Home with no observed Xiaomi
Home, advertising screen or Settings flash in that cycle. Unplugged cold boot,
OTA persistence, extended daily use, HDMI playback and paid streaming playback
have not been validated. The receiver also handles `BOOT_COMPLETED`, but that
code path is not evidence of a successful physical cold-boot test.

ADB/network debugging closes across power cycles on this firmware. Re-enable it
when doing computer maintenance; it is not required for ordinary launcher use.
Do not use a global synthetic STR broadcast as a test: Xiaomi's other receivers
also react to it and can interrupt networking. Use the actual remote power key.

The screenshot shows the compatible launcher during installation, before the
final wake fix; the physical off/on result above was confirmed by the owner.

![Compatible launcher on MiTV-ASTP0](xiaomi-mitv-astp0-home.png)

## Restore Xiaomi Home

With ADB re-enabled if needed, restore the recorded stock launcher first:

```bash
"$PY" -I tools/tv.py 'pm enable --user 0 com.mitv.tvhome'
"$PY" -I tools/tv.py 'cmd package set-home-activity --user 0 com.mitv.tvhome/.MainActivityUserMode'
"$PY" -I tools/tv.py 'input keyevent KEYCODE_HOME'
"$PY" -I tools/tv.py 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME'
```

Changing the default Home also makes the compatibility receiver leave it alone.
No root or stock-launcher uninstall was used. This public record excludes private
network addresses, device identifiers, signing keys and raw diagnostic backups.
