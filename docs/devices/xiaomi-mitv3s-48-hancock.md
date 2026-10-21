# Xiaomi Mi TV 3S 48" (hancock): `pm block`, a frozen PatchWall, and the vendor input path

Tested on MiTV3S-48 (`hancock`, MStar SoC, 774 MB RAM), Android 4.4.4 / API 19, firmware
`7.7.21.1653`, locale zh-CN. China-market PatchWall set: no Google services, no
`android.software.leanback`, no `TvInputManager`. The debloat list is
[`examples/xiaomi-mi-tv-3s-48/debloat.txt`](../../examples/xiaomi-mi-tv-3s-48/debloat.txt); this page
records the firmware behaviour that no package list can express.

## `pm disable-user` is patched to kill its caller

`pm disable-user`, `pm enable` and the Settings UI's 停用 button all fail here, and not in the usual
way: system_server aborts the calling process from inside
`PackageManager.setApplicationEnabledSetting`, so the shell dies with `Killed` and the package stays
enabled. The Settings button is greyed out for every Xiaomi/duokan/mitv package but works for AOSP
apps, which is the giveaway that this is vendor policy rather than a permission problem.

Neither OOM protection (`echo -17 > /proc/self/oom_adj`) nor `am force-stop com.mitv.tvhome` before
the call changes anything — the kill is synchronous inside the binder call, not the low-memory
killer, and MemFree idling at ~27 MB only makes the symptom look like OOM.

`pm clear` works. So does `pm block` / `pm unblock`, which is what the list uses: a blocked package
keeps its APK and data, but none of its components can start. Apply and undo with a shell loop:

```bash
for p in $(grep -v '^#' examples/xiaomi-mi-tv-3s-48/debloat.txt | grep .); do adb shell pm block $p; done
for p in $(grep -v '^#' examples/xiaomi-mi-tv-3s-48/debloat.txt | grep .); do adb shell pm unblock $p; done
```

`tools/disable.sh` / `tools/restore.sh` are not an option even with `adb` on the PATH: they emit
`pm disable-user` (which kills the shell) and this 4.4 `sh` has no `head`/`tail`/`grep` for their
helper functions.

## Home: freezing PatchWall

`com.mitv.tvhome` (PatchWall) answers `CATEGORY_HOME` and is what `pm block com.mitv.tvhome` replaces.
Two things to know before doing it:

- PatchWall runs as uid 1000 and shares its process with the vendor TV player, the MStar service and
  the settings providers. Freezing it also takes the stock 信号源 (input) entry with it, so a
  replacement launcher has to offer its own Inputs menu.
- It is the only installed `CATEGORY_HOME` activity besides AOSP `com.android.launcher` (already
  disabled on this firmware), so the switch is binary: PatchWall, or your launcher. `pm unblock
  com.mitv.tvhome` restores it exactly.

There is no root and no middle state, and `cmd package set-home-activity` does not exist on 4.4 —
install, press Home, and pick "always" in the chooser.

## Inputs without `TvInputManager`

`service list` has no `tv_input`; HDMI/AV switch through the MStar boot-classpath API:

```java
Class<?> mgr  = Class.forName("com.mstar.android.tv.TvCommonManager");
Class<?> en   = Class.forName("com.mstar.android.tvapi.common.vo.TvOsType$EnumInputSource");
Object   m    = mgr.getMethod("getInstance").invoke(null);
mgr.getMethod("setInputSource", en).invoke(m, Enum.valueOf((Class<Enum>) en, name));
```

Two firmware details matter more than the reflection itself:

1. **The enum names are not what other MStar sets use.** This build has `E_INPUT_SOURCE_HDMI`,
   `…_HDMI2`, `…_HDMI3`, `…_HDMI4` — the first port has no digit, and there is no
   `E_INPUT_SOURCE_HDMI1`, `E_INPUT_SOURCE_CVBS1` or `E_INPUT_SOURCE_PANEL` at all. Resolve names at
   run time and accept `_HDMI` / `_HDMI1` interchangeably, or the first port silently fails.
2. **A raw switch is reverted within the same second.** Xiaomi's `SourceManagerImpl` in
   system_server expects the Android source (`E_INPUT_SOURCE_STORAGE`, ordinal 34) and logs
   `reported source 23 is not expected 34` before setting 34 back, so `setInputSource(23)` reports
   success and the picture never changes. The switch is only honoured when the vendor TV player
   performs it:

```bash
am start -a com.xiaomi.mitv.tvplayer.EXTSRC_PLAY \
  -n com.xiaomi.mitv.tvplayer/.ExternalSourceActivity --ei input 23   # 23 HDMI1, 24 HDMI2, 25 HDMI3
```

`input` is the ordinal of `TvOsType.EnumInputSource`; the same numbers appear in
`mitv.tv.SourceManager` (`TYPE_INPUT_SOURCE_HDMI = 23`, `…_STORAGE = 34`). Pressing Home afterwards
switches back to the Android source on its own — the player owns that state machine, so a launcher
does not need a "return to Android" call.

A launcher can also be given the vendor's own source panel on sets that have one, via
`source_panel_service` / `source_panel_action` in `config.xml`.

## Casting and SMB shares

The TV-side casting stack is `com.xiaomi.mitv.smartshare` (小米投屏 / "Wireless Share": Miracast, DLNA,
iPhone/Mac), `com.duokan.airkan.tvbox` (Mi-Link), `com.xiaomi.dlnatvservice`, `com.xiaomi.upnp` and
`com.waxrain.airplaydmr` (AirPin PRO). Keep `smartshare`: freezing it breaks casting discovery.

Nothing on this firmware handles `smb://` — `am start -a android.intent.action.VIEW -d smb://… -t
video/x-matroska` fails with `unable to resolve Intent` — so casting a video that lives on an SMB
share fails with "no suitable player" even though the receiver itself is fine. Three ways around it:

- Play it on the TV instead: `com.xiaomi.mitv.mediaexplorer` (高清播放器) ships its own SMB client
  (`libsmbmnt.so`, `xmjcifs`, `addSmbDevice` / `smb_user_pwd`) and browses shares from its 设备 tab.
- Serve the file over HTTP or DLNA from the NAS/Pi and cast that URL, which every player handles.
- Install a player that registers `smb` (VLC and friends) so the pushed URI has a target.

Note that the media explorer answers `ACTION_MAIN` but declares no `CATEGORY_LAUNCHER`, so no stock
or third-party grid lists it; a launcher needs an explicit-component tile to offer 高清播放器.

## Odds and ends

- `pm install` shows a 安装确认 dialog on screen; adb blocks for over two minutes until someone
  presses 确定. D-pad centre is not always accepted, `input tap` is.
- Network ADB on 5555 survives reboots and drops under memory pressure; toggling
  Settings → 账号与安全 → ADB调试 off/on brings it back.
- MemFree idles around 30 MB with ~200 MB reclaimable cache; with PatchWall frozen and the list
  above applied, the launcher, casting and HDMI all run without the low-memory killer interfering.
