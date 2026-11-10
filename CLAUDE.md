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
E2E check of the minified release build (signed with the local debug key): `scripts/e2e/release_smoke.sh [emulator-serial]` after `./gradlew :app:assembleRelease`.
