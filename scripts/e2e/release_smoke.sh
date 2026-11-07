#!/bin/sh
# End-to-end check: the minified release build works on a device and logs no HTTP traffic.
#
# Usage: scripts/e2e/release_smoke.sh [emulator-serial]   (after ./gradlew :app:assembleRelease)
#
# R8 problems (a missing keep rule) only show up at runtime, so this drives the main flows:
# 1. Signs the unsigned release APK with the local debug key (same key as debug builds, so it
#    installs over them and keeps their data) and checks the installed app is not debuggable.
# 2. Home: the current-location forecast loads (Retrofit + Gson through R8).
# 3. Details: tapping the card opens the details screen with its forecast (navigation arguments).
# 4. Add location: searching "Paris" lists results (Geocoder).
# 5. Widget: if a Vane widget is on the home screen, it shows a temperature (Glance).
# 6. Logcat has no OkHttp request or response lines, no widget snapshot error (kotlinx serialization),
#    and no Vane crash.
# 7. Writes screenshots and a report to build/e2e/release-smoke-<timestamp>/.
set -eu

SERIAL="${1:-emulator-5554}"
PKG=com.theseuntaylor.vane
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
UNSIGNED="$ROOT/app/build/outputs/apk/release/app-release-unsigned.apk"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
OUT="$ROOT/build/e2e/release-smoke-$(date +%Y%m%d-%H%M%S)"
APK="$OUT/app-release-debugsigned.apk"

TEMPERATURE='text="-?[0-9]+°"'
HOME_TITLE='text="Your Current Location"'
DETAILS_TITLE='text="Today.s Hourly Forecast"'
SEARCH_RESULTS='text="Search Results"'

adb() { "$ADB" -s "$SERIAL" "$@"; }
step() { echo "==> $*"; }
pass() { echo "- PASS: $*" >> "$OUT/report.md"; }
fail() { echo "- FAIL: $*" | tee -a "$OUT/report.md"; echo "Artifacts in $OUT"; exit 1; }
shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
ui() {
    adb shell uiautomator dump /sdcard/vane-ui.xml >/dev/null 2>&1
    adb shell cat /sdcard/vane-ui.xml | tr '>' '\n'
}
wait_for() {
    i=0
    until ui | grep -qE "$1"; do
        i=$((i + 1)); [ $i -gt "${2:-20}" ] && return 1
        # The emulator only reports GPS fixes it is sent while a request is active.
        adb emu geo fix -0.1276 51.5072 >/dev/null
        sleep 1
    done
}
tap_node() {
    bounds=$(ui | grep -E "$1" | head -1 \
        | sed -n 's/.*bounds="\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]".*/\1 \2 \3 \4/p')
    [ -n "$bounds" ] || return 1
    # shellcheck disable=SC2086
    set -- $bounds
    adb shell input tap $((($1 + $3) / 2)) $((($2 + $4) / 2))
}

widget_bound() { adb shell dumpsys appwidget | grep -q "provider=ProviderId{.*cmp:ComponentInfo{$PKG/$PKG.feature.widget.ui.VaneWeatherWidgetReceiver}"; }

[ -f "$UNSIGNED" ] || { echo "Build the release APK first: ./gradlew :app:assembleRelease"; exit 1; }
mkdir -p "$OUT"
echo "# Release build smoke E2E — $(date)" > "$OUT/report.md"
echo "Device: $SERIAL ($(adb shell getprop ro.product.model | tr -d '\r'), API $(adb shell getprop ro.build.version.sdk | tr -d '\r'))" >> "$OUT/report.md"

step "Sign the release APK with the debug key and install it"
APKSIGNER=$(ls -d "$SDK"/build-tools/*/apksigner | sort -V | tail -1)
"$APKSIGNER" sign --ks "$HOME/.android/debug.keystore" --ks-pass pass:android --key-pass pass:android \
    --ks-key-alias androiddebugkey --out "$APK" "$UNSIGNED"
echo "APK size: $(wc -c < "$APK" | tr -d ' ') bytes" >> "$OUT/report.md"
adb install -r "$APK" >/dev/null
adb shell dumpsys package $PKG | grep -q "pkgFlags=\[.*DEBUGGABLE" && fail "The installed build is debuggable"
pass "installed the minified release build; it is not debuggable"

step "Home: current-location forecast"
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb shell cmd location set-location-enabled true
adb logcat -c
# FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK: start from a clean task.
adb shell am start -S -W -f 0x10008000 -n $PKG/.MainActivity >/dev/null
wait_for "$HOME_TITLE" || { shot 1-home; fail "Home screen never opened (1-home.png)"; }
wait_for "$TEMPERATURE" 45 || { shot 1-home; fail "No forecast on the home screen (1-home.png)"; }
shot 1-home
pass "home screen shows a forecast, so Retrofit and Gson work after R8 (1-home.png)"

step "Details: tap the first city card"
tap_node "$TEMPERATURE" || fail "Couldn't tap a city card"
wait_for "$DETAILS_TITLE" 30 || { shot 2-details; fail "Details screen didn't show its forecast (2-details.png)"; }
shot 2-details
pass "details screen shows its forecast (2-details.png)"
adb shell input keyevent BACK
wait_for "$HOME_TITLE" || fail "Back didn't return to the home screen"

step "Add location: search for Paris"
tap_node 'content-desc="Plus button for adding a new location"' || fail "Couldn't tap the add button"
sleep 2
adb shell input text Paris
wait_for "$SEARCH_RESULTS" 30 || { shot 3-search; fail "No search results for Paris (3-search.png)"; }
shot 3-search
pass "searching Paris lists results (3-search.png)"
adb shell input keyevent BACK
adb shell input keyevent BACK

step "Widget: shows a temperature, if one is on the home screen"
adb shell input keyevent HOME
sleep 3
if widget_bound; then
    i=0
    until ui | grep -qE "$TEMPERATURE"; do
        i=$((i + 1)); [ $i -gt 30 ] && { shot 4-widget; fail "The widget never showed a temperature (4-widget.png)"; }
        sleep 1
    done
    shot 4-widget
    pass "the home screen widget shows a temperature (4-widget.png)"
else
    echo "- SKIP: no Vane widget on the home screen; add one to also check the widget renders" >> "$OUT/report.md"
fi

step "Logcat: no HTTP logging, no crash"
adb logcat -d > "$OUT/logcat.txt"
grep -E "okhttp|--> GET|<-- 200" "$OUT/logcat.txt" | grep -qi "open-meteo\|--> GET\|<-- 200" \
    && fail "Release build logged HTTP traffic (see logcat.txt)"
pass "no HTTP request or response lines in logcat"
grep -E "FATAL EXCEPTION" -A3 "$OUT/logcat.txt" | grep -q "$PKG" && fail "Vane crashed (see logcat.txt)"
# The home screen saves a widget snapshot with kotlinx serialization on every load, widget or not.
grep -qE "Couldn't update widget|SerializationException|No session available|androidx.glance.session.SessionWorker.*\] failed" "$OUT/logcat.txt" \
    && fail "Saving or rendering the widget data failed (see logcat.txt)"
pass "the widget snapshot was saved without serialization errors"
pass "no Vane crash in logcat"

echo "Done. Artifacts in $OUT"
cat "$OUT/report.md"
