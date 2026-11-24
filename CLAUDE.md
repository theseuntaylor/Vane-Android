# Vane

## Theme
Theme files are generated from `design/seed` by `design/generate-theme.mjs` (`cd design && npm run generate`). Never edit them by hand; to change the theme, change the seed.
Generated: `app/src/main/java/com/theseuntaylor/vane/core/theme/Color.kt`, `app/src/main/res/font/*.ttf`, `app/src/main/assets/font-licenses/`.

## Build
Gradle picks its own JDK from `gradle/gradle-daemon-jvm.properties` (JDK 17; found locally or downloaded via foojay), so don't set `JAVA_HOME` for builds. To change it: `./gradlew updateDaemonJvm --jvm-version=<N>`.
Kotlin comes from the `org.jetbrains.kotlin.android` plugin (`android.builtInKotlin=false`), not AGP's built-in Kotlin, and annotation processors use KSP (no kapt). Versions live in `gradle/libs.versions.toml`; keep the Kotlin, Compose compiler and serialization plugin versions equal.

## Testing
E2E check for the home screen widget (app closed + offline): `scripts/e2e/widget_offline.sh [emulator-serial]` after `./gradlew :app:assembleDebug`. Artifacts land in `build/e2e/`.
E2E check that the build selects the daemon JDK regardless of the shell's Java: `scripts/e2e/daemon_toolchain.sh`.
E2E check that the home screen shows a cached forecast offline: `scripts/e2e/home_offline.sh [emulator-serial]`.
E2E check of the location permission flow (denials, Settings, approximate only): `scripts/e2e/location_permission.sh [emulator-serial]`.
E2E check that the home screen leaves its spinners when location is off and recovers with "Try again": `scripts/e2e/home_states.sh [emulator-serial]`.
E2E check of the bottom navigation tabs and of the System default/Light/Dark setting surviving a restart (restores the setting and the device's dark mode): `scripts/e2e/navigation_appearance.sh [emulator-serial]`. Reads screenshot colours with `scripts/e2e/png_pixel.py` (standard-library Python).
Tabs in E2E scripts: `open_tab Home|Favourites|Settings` in `lib.sh`. Compose puts `selected`/`checked` on a row and its text on a child node, so check states with `has_state <text> selected|checked`. The rain and snow alerts switch is on the Settings tab: tap `$ALERTS_SWITCH` (its label) and read it with `alerts_on`; don't match `checkable="true"`, which the appearance radio buttons also have.
E2E check that adding a favourite shows it on the Favourites tab at once and after a restart (backs up and restores the app's database): `scripts/e2e/add_favourite.sh [emulator-serial]`.
E2E check of rain and snow alerts against canned forecasts (declined permission, dry, already raining, snow in an hour, no repeat alert): `scripts/e2e/rain_alerts.sh [emulator-serial]`. Needs `python3`; debug builds read the alert forecast's base URL from `files/e2e-alerts-base-url`.
Set `APK=<path>` on those five to test another build, e.g. one from master.
E2E check of the minified release build (signed with the local debug key): `scripts/e2e/release_smoke.sh [emulator-serial]` after `./gradlew :app:assembleRelease`.
E2E check that the release bundle is signed from `keystore.properties` (throwaway keystore): `scripts/e2e/release_signing.sh`.

## Release
Pushing a tag such as `v1.2.0` runs `.github/workflows/release.yml`: it builds a signed bundle and uploads it to the Play internal track. Don't bump the version by hand: the workflow sets `versionName` from the tag and `versionCode` to master's value plus one (`scripts/ci/set_version.sh`), then commits both to `master` once Play accepts the bundle.
