# Vane

## Theme
Theme files are generated from `design/seed` by `design/generate-theme.mjs` (`cd design && npm run generate`). Never edit them by hand; to change the theme, change the seed.
Generated: `app/src/main/java/com/theseuntaylor/vane/core/theme/Color.kt`, `app/src/main/res/font/*.ttf`, `app/src/main/assets/font-licenses/`.

## Build
Gradle picks its own JDK from `gradle/gradle-daemon-jvm.properties` (JDK 17; found locally or downloaded via foojay), so don't set `JAVA_HOME` for builds. To change it: `./gradlew updateDaemonJvm --jvm-version=<N>`.
AGP 9 built-in Kotlin: there is no `kotlin-android` plugin, and annotation processors use KSP (no kapt). Keep the Compose and serialization plugin versions equal to the Kotlin version AGP uses.

## Testing
E2E check for the home screen widget (app closed + offline): `scripts/e2e/widget_offline.sh [emulator-serial]` after `./gradlew :app:assembleDebug`. Artifacts land in `build/e2e/`.
E2E check that the build selects the daemon JDK regardless of the shell's Java: `scripts/e2e/daemon_toolchain.sh`.
E2E check that the home screen shows a cached forecast offline: `scripts/e2e/home_offline.sh [emulator-serial]`.
E2E check of the location permission flow (denials, Settings, approximate only): `scripts/e2e/location_permission.sh [emulator-serial]`.
E2E check that the home screen leaves its spinners when location is off and recovers with "Try again": `scripts/e2e/home_states.sh [emulator-serial]`.
Set `APK=<path>` on those three to test another build, e.g. one from master.
