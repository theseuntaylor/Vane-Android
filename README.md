# Vane

A weather app for Android. Vane shows the forecast where you are and for the places you save, with a home screen widget and alerts before rain or snow. Forecasts come from [Open-Meteo](https://open-meteo.com/), which needs no API key.

## Features

- **Home:** the forecast at your location, with today's hourly forecast and conditions (wind, precipitation, UV index, pressure, sunrise and sunset).
- **Favourites:** save other places and open a detailed forecast for each.
- **Widget:** a home screen widget that refreshes in the background and still shows the last forecast offline.
- **Rain and snow alerts:** a notification about an hour before rain or snow starts where you last opened Vane. Turn them on in Settings.
- **Weather background:** Home and the details screen are tinted by the sky (clear, cloudy, fog, rain, snow or storm) and the light (day, dawn, dusk or night), blended into the light or dark theme.
- **Offline:** the last forecast stays on screen when you have no connection. If there's no saved forecast, Vane says you're offline.
- **Settings:**
  - Appearance: follow the system, or always light or dark.
  - Units: °C or °F; km/h, mph or m/s; mm or inches; and a 12- or 24-hour clock.
  - Open-source licences.
- **Location:** approximate location is enough. If you deny it, Vane explains why it needs it and can open Settings for you.

## Requirements

- Android 7.0 (API 24) or later.
- To build: Android Studio or the Android SDK. Gradle picks its own JDK 17 from `gradle/gradle-daemon-jvm.properties`, so you don't need to set `JAVA_HOME`.

## Build and run

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:installDebug
```

A release build needs a `keystore.properties` file at the project root, which isn't in Git:

```properties
storeFile=path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

```bash
./gradlew :app:bundleRelease
```

## Tech stack

- Kotlin and Jetpack Compose with Material 3, and Glance for the widget.
- Hilt for dependency injection, Room for saved places, and WorkManager for the widget refresh and the alerts.
- Retrofit and OkHttp for Open-Meteo. OkHttp's cache keeps the last forecast for offline use.

### Project layout

```
app/src/main/java/com/theseuntaylor/vane/
├── core/       theme, navigation, units, shared components and utilities
└── feature/
    ├── home/                 current location forecast
    ├── favouriteLocations/   saved places
    ├── detailsScreen/        detailed forecast
    ├── settings/             appearance, units, alerts
    ├── alerts/               rain and snow notifications
    └── widget/               home screen widget
```

## Theme

The colours and fonts are generated from a seed in `design/seed`. Don't edit the generated files (`core/theme/Color.kt` and `res/font/`). To change the theme, change the seed and run:

```bash
cd design && npm install && npm run generate
```

## Testing

Vane is tested with end-to-end scripts in `scripts/e2e/` that drive the app on an emulator with `adb`. Each one writes screenshots and a report to `build/e2e/`. Build the debug APK first, then run a script, for example:

```bash
scripts/e2e/home_offline.sh emulator-5554
```

| Script | Checks |
| --- | --- |
| `home_offline.sh` | Home shows a cached forecast offline, and says when there is none |
| `home_states.sh` | Home recovers with "Try again" when location is off |
| `location_permission.sh` | Location permission denials, Settings and approximate location |
| `navigation_appearance.sh` | Bottom navigation, and the light/dark setting surviving a restart |
| `add_favourite.sh` | A new favourite shows at once and after a restart |
| `weather_backdrop.sh` | Home's background matches the weather and time of day (checked against Open-Meteo) |
| `units.sh` | Units chosen in Settings change Home and survive a restart |
| `rain_alerts.sh` | Rain and snow alerts against canned forecasts |
| `widget_offline.sh` | The widget with the app closed and offline |
| `release_smoke.sh` | The minified release build (after `./gradlew :app:assembleRelease`) |
| `release_signing.sh` | The release bundle is signed from `keystore.properties` |
| `daemon_toolchain.sh` | The build uses the daemon JDK whatever Java the shell has |

Set `APK=<path>` on the app scripts to test another build.

## Release

Pushing a version tag such as `v1.2.0` runs `.github/workflows/release.yml`. It builds a signed bundle, uploads it to the Play production track, and commits the new version to `master`. It needs these repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` and `SERVICE_ACCOUNT_JSON`.

The Play store listing (title, descriptions and phone screenshots) lives in `fastlane/metadata/android/`. To update it, run the Store listing workflow by hand from the Actions tab: `validate` only asks Play to check it, and `upload` replaces the live listing.
