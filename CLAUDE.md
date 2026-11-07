# Vane

## Theme
Theme files are generated from `design/seed` by `design/generate-theme.mjs` (`cd design && npm run generate`). Never edit them by hand; to change the theme, change the seed.
Generated: `app/src/main/java/com/theseuntaylor/vane/core/theme/Color.kt`, `app/src/main/res/font/*.ttf`, `app/src/main/assets/font-licenses/`.

## Build
Gradle picks its own JDK from `gradle/gradle-daemon-jvm.properties` (JDK 17; found locally or downloaded via foojay), so don't set `JAVA_HOME` for builds. To change it: `./gradlew updateDaemonJvm --jvm-version=<N>`.
AGP 9 built-in Kotlin: there is no `kotlin-android` plugin, and annotation processors use KSP (no kapt). Keep the Compose and serialization plugin versions equal to the Kotlin version AGP uses. Libraries built with a newer Kotlin than that (e.g. kotlinx-serialization 1.10+, kotlin-bom above it) fail to compile; those lines carry a `//noinspection` comment.

## Testing
E2E check for the home screen widget (app closed + offline): `scripts/e2e/widget_offline.sh [emulator-serial]` after `./gradlew :app:assembleDebug`. Artifacts land in `build/e2e/`.
E2E check that the build selects the daemon JDK regardless of the shell's Java: `scripts/e2e/daemon_toolchain.sh`.
E2E check of the minified release build (signed with the local debug key): `scripts/e2e/release_smoke.sh [emulator-serial]` after `./gradlew :app:assembleRelease`.
