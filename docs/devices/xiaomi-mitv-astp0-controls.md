# MiTV-ASTP0: native inputs, MENU and the Home chooser

Follow-up to the [initial standby/wake record](xiaomi-mitv-astp0-mulan.md),
on the same internal model `MiTV-ASTP0` / `mulan`, Android 9/API 28,
firmware `24.7.4.3982`, on 2026-10-21. The compatible launcher remains
CleanYourTV 2.4 (`versionCode 5`), application ID `com.ts.launcher`, target SDK 25.

## Input sources

This firmware enumerates TvInputManager inputs, but has no activity handling
the standard `ACTION_VIEW` passthrough-channel URI. Generic HDMI rows therefore
could not switch sources. The [device overlay](../../themes/xiaomi-mitv-astp0/)
sets an empty `live_tv_package` and explicitly opens
`com.xiaomi.mitv.settings/.TvSettingService` with action
`com.xiaomi.mitv.settings.INPUTSOURCE_POPUP`.

The native picker lists the firmware's actual choices, including HDMI1, HDMI2,
TV, AV and DTMB. Selecting HDMI1 opened
`com.xiaomi.mitv.tvplayer/.ExternalSourceActivity`, with a live TvInputManager
session on `com.droidlogic.tvinput/.services.Hdmi1InputService/HW5`.
The owner confirmed normal HDMI picture. Home returned directly from the player.

## Short MENU press

The overlay configures keycode 82 and the same explicit settings service with
action `com.xiaomi.intent.action.show.globalsetting`. This opens the native
network, picture, sound, general, peripherals and source menu.

The firmware's `MitvPhoneWindowManager` consumes the initial short MENU event,
then re-injects a DOWN/UP pair with exactly `MY_SPECIAL_FLAG = 30001` (`0x7531`).
That value includes Android's `FLAG_CANCELED` bit. The previous canceled-event
guard silently ignored the valid re-injected release.

The shared settings-key policy accepts only an UP event that is not canceled,
or a MENU UP with flags exactly 30001. Other canceled keys and flag combinations
remain ignored. MainActivity and the optional HomeKeyService use the same policy.
No accessibility service was enabled for this device; the fix works on Home.
Four JVM tests cover ordinary release, DOWN, cancellation and the narrow exception.

`settings_key_service` and `settings_key_action` default to empty strings.
Vendor settings are opt-in through config.xml or a theme. Components may be
activities or services; an unavailable settings component falls back to the
existing panel/Settings behavior. Source-panel failure retains its unavailable
message. The default package ID and target SDK 34 build remain unchanged.

## Home chooser and stock updates

During follow-up inspection, Xiaomi Home had automatically updated from
3.46.1185 to 3.49.1601 and was enabled again. That changed the HOME candidate
set saved in the preferred activity, so Home resolved to Android's chooser.
Re-selecting CleanYourTV restored direct Home navigation.

Keeping stock Home enabled allowed a brief advertising screen during the next
physical standby/wake test. The earlier reversible disable step is therefore
still needed on this firmware. Change enabled packages first, then select Home
again, since changing the candidate set after selection can invalidate it:

```bash
adb shell pm disable-user --user 0 com.mitv.tvhome
adb shell cmd package set-home-activity --user 0 com.ts.launcher/com.gathin.tvhome.MainActivity
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
adb shell input keyevent KEYCODE_HOME
```

Expected HOME: `com.ts.launcher/com.gathin.tvhome.MainActivity`.
The stock package stays installed. To restore, enable stock Home before selecting
`com.mitv.tvhome/.MainActivityUserMode`, as in the initial device record.
Future stock-package or firmware updates can change these settings; update
persistence and unplugged cold boot remain unverified.

## Verification

- Both default and legacy-profile builds pass unit tests, lint and release assembly:
  135 JVM tests, zero failures/errors.
- The source snapshot's Python suite passes 58 tests with one expected skip.
- On-device regression checks pass Home, native MENU, native Inputs, returning
  Home from each native panel and from HDMI playback.
- The owner confirmed physical remote MENU, working HDMI picture and repeated
  Home presses with no chooser. The subsequent standby/wake check identified
  the brief stock advertising screen described above.
- After disabling stock Home and re-selecting the replacement, the owner repeated
  remote off, five-second wait, on and confirmed direct entry into the new Home
  without the advertising screen. No Home/Back press or ADB reopening was needed.

No private network addresses, identifiers, signing keys, raw logs or vendor APKs
are included in this record.
