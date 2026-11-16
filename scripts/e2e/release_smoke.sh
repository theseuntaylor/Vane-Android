#!/bin/sh
# End-to-end check: the minified release build works on a device and logs no HTTP traffic.
#
# Usage: scripts/e2e/release_smoke.sh [emulator-serial]   (after ./gradlew :app:assembleRelease)
#
# R8 problems (a missing keep rule) only show up at runtime, so this drives the main flows:
# 1. Signs the release APK (unsigned, or signed with keystore.properties) with the local debug key (same key as debug builds, so it
#    installs over them and keeps their data) and checks the installed app is not debuggable.
# 2. Home tab: the current-location forecast and its hourly forecast load (Retrofit + Gson through R8).
# 3. Details: on the Favourites tab, tapping a saved place opens its details screen (navigation
#    arguments). Skipped when there are no favourites.
# 4. Rain and snow alerts (Settings tab): turning them on reads the 15-minute forecast (Gson model
#    through R8), then off again.
# 5. Add place (Favourites tab): searching "Paris" lists results (Geocoder).
# 6. Widget: if a Vane widget is on the home screen, it shows a temperature (Glance).
# 7. Logcat has no OkHttp request or response lines, no widget snapshot error (kotlinx serialization),
#    and no Vane crash.
# 8. Writes screenshots and a report to build/e2e/release-smoke-<timestamp>/.
set -eu

SERIAL="${1:-emulator-5554}"
# Shared helpers (open_tab, has_state, ALERTS_SWITCH); the definitions below override the rest.
. "$(dirname "$0")/lib.sh"
PKG=com.theseuntaylor.vane
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# app-release.apk when keystore.properties sets up signing, app-release-unsigned.apk otherwise.
RELEASE=$(ls -t "$ROOT"/app/build/outputs/apk/release/app-release*.apk 2>/dev/null | head -1)
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
OUT="$ROOT/build/e2e/release-smoke-$(date +%Y%m%d-%H%M%S)"
APK="$OUT/app-release-debugsigned.apk"

TEMPERATURE='text="-?[0-9]+°"'
HOME_TITLE='text="Your Current Location"'
DETAILS_TITLE='text="Today.s Hourly Forecast"'
FAVOURITES_TITLE='text="Favourite Locations"'
FAVOURITES_DONE='text="Tap Add place to save a place and see its weather here."|text="-?[0-9]+°"'
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

widget_bound() { adb shell dumpsys appwidget | grep -q "provider=ProviderId{.*cmp:ComponentInfo{$PKG/$PKG.feature.widget.ui.VaneWeatherWidgetReceiver}"; }

[ -n "$RELEASE" ] || { echo "Build the release APK first: ./gradlew :app:assembleRelease"; exit 1; }
mkdir -p "$OUT"
echo "# Release build smoke E2E — $(date)" > "$OUT/report.md"
echo "Device: $SERIAL ($(adb shell getprop ro.product.model | tr -d '\r'), API $(adb shell getprop ro.build.version.sdk | tr -d '\r'))" >> "$OUT/report.md"

step "Sign the release APK with the debug key and install it"
APKSIGNER=$(ls -d "$SDK"/build-tools/*/apksigner | sort -V | tail -1)
"$APKSIGNER" sign --ks "$HOME/.android/debug.keystore" --ks-pass pass:android --key-pass pass:android \
    --ks-key-alias androiddebugkey --out "$APK" "$RELEASE"
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
ui | grep -qE "$DETAILS_TITLE" || { shot 1-home; fail "No hourly forecast on the Home tab (1-home.png)"; }
shot 1-home
pass "Home tab shows the forecast and its hourly forecast, so Retrofit and Gson work after R8 (1-home.png)"

step "Details: tap a saved place on the Favourites tab"
open_tab Favourites || { shot 2-favourites; fail "Couldn't open the Favourites tab (2-favourites.png)"; }
wait_for "$FAVOURITES_DONE" 45 || { shot 2-favourites; fail "Favourites tab never finished loading (2-favourites.png)"; }
if ui | grep -qE "$TEMPERATURE"; then
    tap_node "$TEMPERATURE" || fail "Couldn't tap a saved place"
    wait_for "$DETAILS_TITLE" 30 || { shot 2-details; fail "Details screen didn't show its forecast (2-details.png)"; }
    shot 2-details
    pass "details screen shows its forecast (2-details.png)"
    adb shell input keyevent BACK
    wait_for "$FAVOURITES_TITLE" || fail "Back didn't return to the Favourites tab"
else
    echo "- SKIP: no favourites saved, so the details screen wasn't opened" >> "$OUT/report.md"
fi

step "Rain and snow alerts (Settings tab): the check reads the real 15-minute forecast"
open_tab Settings || { shot 3-settings; fail "Couldn't open the Settings tab (3-settings.png)"; }
wait_for "$ALERTS_SWITCH" || { shot 3-settings; fail "No alerts switch on the Settings tab (3-settings.png)"; }
shot 3-settings
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS 2>/dev/null || true
if ! alerts_on; then
    tap_node "$ALERTS_SWITCH" || fail "Couldn't tap the alerts switch"
    i=0
    # Any outcome line means the response parsed; "check failed" means it didn't.
    until adb logcat -d -s RainAlerts:* | grep -qE "No rain or snow|Already raining|Already alerted|Posted|check failed"; do
        i=$((i + 1)); [ $i -gt 30 ] && fail "The alerts check never ran (see logcat)"
        sleep 1
    done
    adb logcat -d -s RainAlerts:* | grep -q "check failed" && fail "The alerts check failed after R8 (see logcat.txt)"
    tap_node "$ALERTS_SWITCH"   # back off, as it was
    pass "turning alerts on reads the 15-minute forecast after R8: $(adb logcat -d -s RainAlerts:I | grep -oE 'RainAlerts: .*' | tail -1)"
else
    echo "- SKIP: rain and snow alerts were already on; left as they were" >> "$OUT/report.md"
fi

step "Add place (Favourites tab): search for Paris"
open_tab Favourites || fail "Couldn't open the Favourites tab"
tap_node 'content-desc="Add place"' || fail "Couldn't tap 'Add place'"
sleep 2
adb shell input text Paris
wait_for "$SEARCH_RESULTS" 30 || { shot 4-search; fail "No search results for Paris (4-search.png)"; }
shot 4-search
pass "searching Paris lists results (4-search.png)"
adb shell input keyevent BACK
adb shell input keyevent BACK

step "Widget: shows a temperature, if one is on the home screen"
adb shell input keyevent HOME
sleep 3
if widget_bound; then
    i=0
    until ui | grep -qE "$TEMPERATURE"; do
        i=$((i + 1)); [ $i -gt 30 ] && { shot 5-widget; fail "The widget never showed a temperature (5-widget.png)"; }
        sleep 1
    done
    shot 5-widget
    pass "the home screen widget shows a temperature (5-widget.png)"
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
