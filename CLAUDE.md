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
E2E check that the release bundle is signed from the environment (throwaway keystore): `scripts/e2e/release_signing.sh`.

## Release
Pushing a tag such as `v1.2.0` runs `.github/workflows/release.yml`: it builds a signed bundle and uploads it to the Play internal track. Don't bump the version by hand: the workflow sets `versionName` from the tag and `versionCode` to master's value plus one (`scripts/ci/set_version.sh`), then commits both to `master` once Play accepts the bundle.
