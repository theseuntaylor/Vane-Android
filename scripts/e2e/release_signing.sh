#!/bin/sh
# End-to-end check: the release bundle is signed from keystore.properties, the way
# .github/workflows/release.yml builds it.
#
# Usage: scripts/e2e/release_signing.sh
#
# Ways this can fail, each checked below:
#   1. A release build without keystore.properties breaks (local builds need no keystore).
#   2. The bundle built with keystore.properties is not signed.
#   3. The bundle is signed with a key other than the one keystore.properties names.
#   4. scripts/ci/set_version.sh does not write the release version into app/build.gradle,
#      or accepts a version that is not safe to write.
#
# Uses a throwaway keystore, never the real one. An existing keystore.properties is moved aside
# and put back at the end. Writes a log to
# build/e2e/release-signing-<timestamp>.log.
set -eu

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/build/e2e"
LOG="$OUT/release-signing-$(date +%Y%m%d-%H%M%S).log"
AAB="$ROOT/app/build/outputs/bundle/release/app-release.aab"
TMP="$(mktemp -d)"
mkdir -p "$OUT"
cd "$ROOT"

PROPS="$ROOT/keystore.properties"
[ -f "$PROPS" ] && mv "$PROPS" "$TMP/keystore.properties.saved"
restore() {
    rm -f "$PROPS"
    [ -f "$TMP/keystore.properties.saved" ] && mv "$TMP/keystore.properties.saved" "$PROPS"
    rm -rf "$TMP"
}
trap restore EXIT

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

sh ./gradlew :app:bundleRelease -q >> "$LOG" 2>&1 || fail "bundleRelease failed without keystore.properties (see log)"
if jarsigner -verify "$AAB" 2>&1 | grep -q "jar verified"; then
    fail "bundle is signed although there is no keystore.properties"
fi
echo "- PASS: bundleRelease without keystore.properties builds an unsigned bundle" >> "$LOG"

keytool -genkeypair -keystore "$TMP/e2e.jks" -storepass e2e-store -keypass e2e-store -alias e2e \
    -keyalg RSA -keysize 2048 -validity 1 -dname "CN=Vane E2E" >> "$LOG" 2>&1 \
    || fail "could not create the throwaway keystore (see log)"
expected=$(keytool -list -v -keystore "$TMP/e2e.jks" -storepass e2e-store -alias e2e 2>> "$LOG" | sha256)

printf 'storeFile=%s\nstorePassword=e2e-store\nkeyAlias=e2e\nkeyPassword=e2e-store\n' "$TMP/e2e.jks" > "$PROPS"
sh ./gradlew :app:bundleRelease -q >> "$LOG" 2>&1 || fail "bundleRelease failed with keystore.properties (see log)"

jarsigner -verify "$AAB" 2>&1 | tee -a "$LOG" | grep -q "jar verified" || fail "bundle is not signed (see log)"
echo "- PASS: bundle built with keystore.properties is signed" >> "$LOG"

actual=$(keytool -printcert -jarfile "$AAB" 2>> "$LOG" | sha256)
[ -n "$expected" ] && [ "$expected" = "$actual" ] || fail "bundle certificate ($actual) is not the keystore's ($expected)"
echo "- PASS: bundle certificate matches the keystore ($actual)" >> "$LOG"

echo "Done. Log: $LOG"
grep -E "^(# |- PASS)" "$LOG"
