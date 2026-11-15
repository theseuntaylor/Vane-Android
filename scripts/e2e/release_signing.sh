#!/bin/sh
# End-to-end check: the release bundle is signed from the environment, the way
# .github/workflows/release.yml builds it.
#
# Usage: scripts/e2e/release_signing.sh
#
# Ways this can fail, each checked below:
#   1. A release build without signing variables breaks (local builds need no keystore).
#   2. The bundle built with signing variables is not signed.
#   3. The bundle is signed with a key other than the one in KEYSTORE_FILE.
#   4. scripts/ci/set_version.sh does not write the release version into app/build.gradle,
#      or accepts a version that is not safe to write.
#
# Uses a throwaway keystore, never the real one. Writes a log to
# build/e2e/release-signing-<timestamp>.log.
set -eu

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/build/e2e"
LOG="$OUT/release-signing-$(date +%Y%m%d-%H%M%S).log"
AAB="$ROOT/app/build/outputs/bundle/release/app-release.aab"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$OUT"
cd "$ROOT"

fail() { echo "FAIL: $*" | tee -a "$LOG"; exit 1; }
sha256() { sed -n 's/.*SHA256: *//p' | head -1; }

echo "# Release signing E2E — $(date)" > "$LOG"

cp app/build.gradle "$TMP/build.gradle"
scripts/ci/set_version.sh 4321 9.8.7 "$TMP/build.gradle" >> "$LOG" 2>&1 || fail "set_version.sh failed (see log)"
grep -q '^ *versionCode = 4321$' "$TMP/build.gradle" || fail "set_version.sh did not write versionCode"
grep -q '^ *versionName = "9.8.7"$' "$TMP/build.gradle" || fail "set_version.sh did not write versionName"
[ "$(diff app/build.gradle "$TMP/build.gradle" | grep -c '^>')" = 2 ] || fail "set_version.sh changed more than the two version lines"
if scripts/ci/set_version.sh 5 '1.0"; rm' "$TMP/build.gradle" >> "$LOG" 2>&1; then
    fail "set_version.sh accepted an unsafe versionName"
fi
echo "- PASS: set_version.sh writes versionCode and versionName, and rejects unsafe values" >> "$LOG"

unset KEYSTORE_FILE KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD
sh ./gradlew :app:bundleRelease -q >> "$LOG" 2>&1 || fail "bundleRelease failed without signing variables (see log)"
if jarsigner -verify "$AAB" 2>&1 | grep -q "jar verified"; then
    fail "bundle is signed although no signing variables were set"
fi
echo "- PASS: bundleRelease without signing variables builds an unsigned bundle" >> "$LOG"

keytool -genkeypair -keystore "$TMP/e2e.jks" -storepass e2e-store -keypass e2e-store -alias e2e \
    -keyalg RSA -keysize 2048 -validity 1 -dname "CN=Vane E2E" >> "$LOG" 2>&1 \
    || fail "could not create the throwaway keystore (see log)"
expected=$(keytool -list -v -keystore "$TMP/e2e.jks" -storepass e2e-store -alias e2e 2>> "$LOG" | sha256)

KEYSTORE_FILE="$TMP/e2e.jks" KEYSTORE_PASSWORD=e2e-store KEY_ALIAS=e2e KEY_PASSWORD=e2e-store \
    sh ./gradlew :app:bundleRelease -q >> "$LOG" 2>&1 || fail "bundleRelease failed with signing variables (see log)"

jarsigner -verify "$AAB" 2>&1 | tee -a "$LOG" | grep -q "jar verified" || fail "bundle is not signed (see log)"
echo "- PASS: bundle built with signing variables is signed" >> "$LOG"

actual=$(keytool -printcert -jarfile "$AAB" 2>> "$LOG" | sha256)
[ -n "$expected" ] && [ "$expected" = "$actual" ] || fail "bundle certificate ($actual) is not the keystore's ($expected)"
echo "- PASS: bundle certificate matches the keystore ($actual)" >> "$LOG"

echo "Done. Log: $LOG"
grep -E "^(# |- PASS)" "$LOG"
