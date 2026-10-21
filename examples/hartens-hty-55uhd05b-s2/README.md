# Hartens HTY-55UHD05B-S2 (YaOS)

A 55" 4K Hartens on YaOS, Yandex's TV OS (Android 9, CVTE chassis CV9632-K55, MediaTek MT9632). This folder holds
the debloat list ([`debloat.txt`](debloat.txt)) and the launcher theme ([`config.json`](config.json)) the set was
cleaned with. The header of `debloat.txt` has the details: how to get ADB, what must stay and why.

Every command runs from the repository root, with your TV's IP in `TV_HOST` instead of `192.168.1.50`. The first time, create the venv:
`python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt`.

## Turn ADB on and off

There is no USB debugging toggle in Developer options. On the Yandex home screen (or the new one), hold **Menu**
(three bars) for about a second, then press **Left, Up, Left, Up, Back**. In the service menu open
**other options** and set **ADB Mode** to on. Change nothing else there: **Aging mode** starts a colour test pattern
that only pulling the mains plug for a minute stops. Set ADB Mode back to off when you are done; it otherwise stays
on across reboots and leaves port 5555 open on your network.

## Debloat and undo

```sh
TV_HOST=192.168.1.50 tools/disable.sh examples/hartens-hty-55uhd05b-s2/debloat.txt
TV_HOST=192.168.1.50 tools/restore.sh examples/hartens-hty-55uhd05b-s2/debloat.txt com.yandex.tv.home
```

`restore.sh` enables every package in the list again and gives Home back to the Yandex launcher.

## Re-enable a single app

```sh
TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/tv.py 'pm enable ru.ivi.client'
```

Package names are in `debloat.txt`, each with a comment saying what it is. To keep an app for good, put `#` in
front of its line.

## Install apps without Google Play

YaOS has no Play Store, and Google Play services cannot be added without root. Install APKs over ADB instead;
the userland is 32-bit, so pick `armeabi-v7a` builds. `b64push.py` moves large files reliably:

```sh
TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/b64push.py SmartTube_stable_<version>_armeabi-v7a.apk /data/local/tmp/app.apk
TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/tv.py 'pm install -r /data/local/tmp/app.apk'
TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/tv.py 'rm /data/local/tmp/app.apk'
```

SmartTube comes from its GitHub releases (yuliskov/SmartTube). Apple TV needs the Android TV build that still
supports API 26 (17.0.0 was tested), a base APK and a `config.armeabi_v7a` split, installed as one session, one
`pm` command per call: `pm install-create -r -S <total bytes>`, then `pm install-write -S <bytes> <session> base
<path>` and the same for the split, then `pm install-commit <session>`. Check the signature first
(`apksigner verify --print-certs`). Apple TV films need Private DNS off, see below.

To dim the screen below the lowest backlight (falling asleep to sound), the tested set uses DimTV 1.0 (GitHub
MarshMeadow/DimTV, a fork of the open-source Dimmer: no INTERNET permission, signed with the public AOSP test key,
which grants nothing here since the platform is signed by Yandex). Its tile toggles a black overlay over every app.
The level is set over ADB, five points a step, and kept:

```sh
TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/tv.py 'am broadcast -a giraffine.dimmer.STEP_DOWN -n giraffine.dimmer/.TaskerReceiver'
```

`STEP_UP` goes back. The TV's own "Picture off" (Device Preferences > Power) is in the settings APK but hidden on
YaOS.

## Build and install the launcher

Needs JDK 17 and the Android SDK (`brew install openjdk@17 android-commandlinetools`, then
`sdkmanager "platforms;android-34" "build-tools;34.0.0"`).

```sh
TV_HOST=192.168.1.50 STOCK_LAUNCHER=none launcher/build-and-install.sh
TV_HOST=192.168.1.50 tools/theme.sh examples/hartens-hty-55uhd05b-s2
```

`STOCK_LAUNCHER=none` keeps `com.yandex.tv.home` enabled: the remote's Input, Settings and Alice buttons are routed
through it. The launcher is still Home. `INPUT_KEY_SERVICE` and `HOME_KEY_SERVICE` are not needed and would not stick:
YaOS removes any accessibility service it does not know within milliseconds.

## Change the dock or the wallpaper

Edit [`config.json`](config.json) and push it again with `tools/theme.sh examples/hartens-hty-55uhd05b-s2`.

- **Dock order**: `dock_apps`, in order. YouTube is `com.yandex.tv.ytplayer`, Kinopoisk `ru.kinopoisk.yandex.tv`,
  the USB media centre `com.cultraview.newfilemanager`. The dock can also be rearranged on the TV: hold OK on the
  Settings button and choose Edit dock.
- **Tile names**: `app_labels`, as `"package=Name"`.
- **A name after the greeting**: `"greeting_name": "Anna"` shows GOOD EVENING, ANNA (in the TV's language).
- **Wallpaper**: put a dark 1920x1080 JPEG under 300 KB in this folder as `wallpaper.jpg` and set
  `"wallpaper": true`. `"background_color"` changes the black behind everything.

All theme keys are listed in [`themes/README.md`](../../themes/README.md). `tools/theme.sh --reset` goes back to the
launcher's defaults.

## Things this set needs that others do not

- `"input_uri_scheme": "live-tv"`: YaOS plays HDMI in `com.yandex.tv.live`, whose source player answers
  `live-tv://android.media.tv/passthrough/<input>`. The standard `content://` URI lands in its channel player,
  which starts the last Kinopoisk film instead.
- `"settings_key_keycodes": [176]`: the gear button (KEYCODE_SETTINGS) opens the TV settings panel while the
  launcher is in front.
- `"live_tv_package": ""`: the MediaTek tuner app has no channels on YaOS, so the Inputs menu has no Live TV row.

## Ad-blocking DNS

AdGuard DNS as Private DNS blocks Yandex Metrica and many ad hosts, not YouTube ads. It also breaks Apple TV:
trailers play, films stop with an error after the pre-roll, because the licence depends on Apple's ad and
metrics hosts it blocks. The tested set runs without it. Off:

```sh
TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/tv.py 'settings put global private_dns_mode opportunistic'
```

Or set it again:

```sh
TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/tv.py 'settings put global private_dns_specifier dns.adguard-dns.com'
TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/tv.py 'settings put global private_dns_mode hostname'
```
