# Verification status

**Read this first.** This project was authored in a sandbox with a JDK but no Android SDK,
Gradle, Kotlin compiler or network. Nothing here has been compiled, packaged, installed or
profiled. No test or profiler results exist, and none are claimed.

## What to run (in order)

| Step | Command | Pass criteria |
|---|---|---|
| 1. Compile | `./gradlew assembleDebug` | BUILD SUCCESSFUL, no AAPT2 errors |
| 2. Unit tests | `./gradlew testDebugUnitTest` | 8 tests, 0 failures (IconPixelOps x4, PureLogic x4) |
| 3. Lint | `./gradlew lintDebug` | no errors (warnings expected: portrait lock, greylist reflection) |
| 4. Release | `./gradlew assembleRelease bundleRelease` | APK + AAB produced, R8 clean |
| 5. Install | `adb install -r app/build/outputs/apk/release/app-release.apk` | installs, appears in Default home app list |

## Manual functional checklist

- [ ] Home shows day name rotated, date/weather/battery meta, 5 default favourites with line icons
- [ ] Swipe up opens drawer; search filters; Search key launches first match; A-Z rail jumps
- [ ] Long-press an app: pin/unpin, app info, uninstall all work
- [ ] Swipe down expands the shade; double-tap locks once the accessibility service is on
- [ ] Settings: name, icon style, wallpaper mode, 12/24h, weather coords all persist after reboot
- [ ] Install/uninstall an app while the drawer is open: list updates without restart
- [ ] Home button from drawer/settings returns to the clean home screen
- [ ] Cutout and gesture-nav devices: no clipped content

## Performance protocol (do on a real mid-range device, release build)

1. Jank: `adb shell dumpsys gfxinfo com.noir.launcher framestats` after scrolling the drawer for 30 s.
   Target: < 5% janky frames.
2. Memory: `adb shell dumpsys meminfo com.noir.launcher` after opening the drawer on a 150+ app
   device. Target: TOTAL PSS well under 150 MB.
3. Battery/idle: leave on the home screen for 1 h and check `dumpsys batterystats`; the only
   periodic work is a 60 s clock tick (lifecycle-gated) and the opt-in 30 min weather poll.
4. Startup: `adb shell am start -W -n com.noir.launcher/.MainActivity` (cold). Consider adding a
   Baseline Profile + Macrobenchmark module after the first green build.

## Known risks to check on first build

Written without a compiler, so expect to fix small things if any appear. Most likely places:
Compose/Material API signatures in `ui/*.kt`, and `LauncherApps.Callback` overrides in
`data/AppRepository.kt` (nullability of platform types).
