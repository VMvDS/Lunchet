# Noir Launcher 2.0

2.0 adds: Dynamic Island around the camera hole (system-wide), Control Center, Notification Center,
real Android widget hosting + built-in widget cards, 3 home layouts, accent/font/size controls,
no-typing weather (location or city search), hide apps, grid drawer, wallpaper parallax.
See the in-app Settings tabs (LOOK, HOME, ISLAND, PANELS, GESTURES, SYSTEM).

Native Android launcher (Kotlin, Jetpack Compose, MVVM, DataStore) built from the editorial
OLED-black reference designs: rotated day-name headline, vertical meta text, a staggered
monochrome app list, a procedural coastline, and a thin-line icon family.

## Build

Requirements: Android Studio Ladybug or newer (or JDK 17 + Android SDK 35), Gradle 8.9.

    # one-time, if gradle/wrapper/gradle-wrapper.jar is missing (Android Studio does this for you)
    gradle wrapper --gradle-version 8.9

    ./gradlew testDebugUnitTest          # JVM unit tests
    ./gradlew assembleDebug              # debug APK
    ./gradlew assembleRelease            # R8-minified APK (debug-signed unless keystore.properties exists)
    ./gradlew bundleRelease              # AAB for Play

Release signing: create `keystore.properties` in the project root:

    storeFile=/abs/path/release.jks
    storePassword=...
    keyAlias=...
    keyPassword=...

Then set it as default: Settings > Apps > Default apps > Home app.

## Layout

    app/src/main/java/com/noir/launcher
      MainActivity.kt            single activity, HOME intent handling
      model/Models.kt            AppInfo, IconStyle, LauncherSettings, Weather
      data/AppRepository.kt      LauncherApps discovery + live package callbacks
      data/SettingsStore.kt      Jetpack DataStore (Preferences)
      data/WeatherClient.kt      opt-in Open-Meteo call (HttpURLConnection)
      system/SystemActions.kt    notification shade, uninstall, lock-screen accessibility service
      util/Pure.kt               pure logic (unit tested)
      ui/LauncherViewModel.kt    state, icon LRU cache, favourites, clock/battery/weather flows
      ui/HomeScreen.kt           editorial home layout + gestures
      ui/DrawerScreen.kt         search, A-Z rail, lazy list
      ui/SettingsScreen.kt       typographic settings
      ui/ActionSheet.kt          long-press actions (pin, info, uninstall)
      ui/CoastBackdrop.kt        procedural OLED coastline
      ui/icons/                  Sobel line-art / silhouette icon pipeline
      ui/theme/Theme.kt          palette (#000000 / #050505 / #F4F4F2 / #777777) + type

## Gestures

Swipe up: app drawer. Swipe down: notification shade. Double-tap: lock screen (needs the
"Noir double-tap lock" accessibility service, Settings > Accessibility). Long-press empty
space: settings. Long-press an app: pin / info / uninstall. Home button: collapses overlays.

## Design notes and trade-offs

* Fonts are system families (condensed bold / light / regular) to keep the APK tiny. Drop Jost
  or Montserrat .ttf files into res/font and edit `NoirFont` for the exact geometric look.
* Icon styles: LINE (Sobel edges, default), GLYPH (themed monochrome layer on Android 13+, else
  foreground silhouette), ORIGINAL. Processed at 96 px, cached in an LRU of 160 bitmaps.
* Weather is off by default and only contacts api.open-meteo.com after you enable it and enter
  coordinates. No location permission is requested.
* Swipe-down shade uses a greylisted StatusBarManager method and silently no-ops where an OEM
  blocks it. Double-tap lock uses an accessibility service: Google Play requires a declared
  justification for those; remove the service and the gesture if you want a policy-free build.
* Portrait only; work profiles and widgets hosting are intentionally out of scope.
