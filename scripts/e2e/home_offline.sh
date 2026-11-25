#!/bin/sh
# End-to-end check: the home screen shows the last forecast when the device is offline.
#
# Usage: scripts/e2e/home_offline.sh [emulator-serial]
#
# 1. Online: installs the debug APK with an empty HTTP cache, opens the app and waits for the
#    current-location forecast.
# 2. Offline: closes the app, turns on airplane mode, reopens it at the same location and checks
#    that the same temperature shows with no error, along with the Home tab's hourly forecast and
#    conditions (they come from the same cached response).
# 3. Still offline, empties the HTTP cache and reopens: Home says it's offline with no saved
#    forecast, with "Try again", rather than a generic error.
# 4. Writes screenshots and a report to build/e2e/home-offline-<timestamp>/.
#
# Use an emulator: the script toggles airplane mode and sends GPS fixes.
set -eu

SERIAL="${1:-emulator-5554}"
. "$(dirname "$0")/lib.sh"
OUT="$ROOT/build/e2e/home-offline-$(date +%Y%m%d-%H%M%S)"

# A temperature on a city card, e.g. text="12°" or text="-3°".
TEMPERATURE='text="-?[0-9]+°"'
# Any of the forecast error messages (see core/utils/ForecastErrors.kt).
ERROR='text="(Couldn.t load the forecast|You.re offline, and there.s no saved forecast|The weather service isn.t responding)[^"]*"'
OFFLINE_ERROR='text="You.re offline, and there.s no saved forecast yet[^"]*"'
TRY_AGAIN='text="Try again"'
DETAILS='text="Today.s Hourly Forecast"'
CONDITIONS='text="Today.s Weather Conditions"'

# Home can show its spinner again briefly while it reloads, so read for up to 10 seconds.
current_temperature() {
    for _ in 1 2 3 4 5 6 7 8 9 10; do
        t=$(ui | grep -oE "$TEMPERATURE" | head -1 || true)
        [ -n "$t" ] && { echo "$t"; return 0; }
        sleep 1
    done
}
open_app_at_london() {
    adb emu geo fix -0.1276 51.5072 >/dev/null
    start_vane -S
    # The emulator only reports GPS fixes it is sent while a request is active.
    for _ in 1 2 3; do sleep 1; adb emu geo fix -0.1276 51.5072 >/dev/null; done
}

start_report "Home screen offline E2E"
trap 'network_on' EXIT

step "Online: install with an empty HTTP cache, open the app"
wake_device
network_on
wait_for_network
adb install -r "$APK" >/dev/null
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb shell am force-stop $PKG
# The app recreates its cache directory on demand.
adb shell run-as $PKG rm -rf cache
adb logcat -c
open_app_at_london
wait_for "$TEMPERATURE" 45 || { shot 1-online; fail "Home screen never showed a forecast while online (1-online.png)"; }
ONLINE=$(current_temperature)
[ -n "$ONLINE" ] || { shot 1-online; fail "Couldn't read the temperature online (1-online.png)"; }
shot 1-online
pass "home screen showed a forecast online: $ONLINE (1-online.png)"
adb shell run-as $PKG ls cache > "$OUT/http-cache.txt" 2>&1 || true
grep -q journal "$OUT/http-cache.txt" || fail "No OkHttp cache journal was written (http-cache.txt)"
pass "OkHttp cache has entries after the online load (http-cache.txt)"

step "Offline: close the app, airplane mode, reopen"
adb shell am force-stop $PKG
network_off
sleep 3
adb shell ping -c1 -W2 1.1.1.1 >/dev/null 2>&1 && fail "Device still has network"
pass "device is offline"
open_app_at_london
if ! wait_for "$TEMPERATURE" 30; then
    shot 2-offline
    ui | grep -qE "$ERROR" && fail "Offline: home screen showed the load error instead of the cached forecast (2-offline.png)"
    fail "Offline: home screen never showed a forecast (2-offline.png)"
fi
OFFLINE=$(current_temperature)
[ -n "$OFFLINE" ] || { shot 2-offline; fail "Couldn't read the temperature offline (2-offline.png)"; }
shot 2-offline
[ "$OFFLINE" = "$ONLINE" ] || fail "Offline temperature $OFFLINE differs from online $ONLINE"
pass "home screen showed the cached forecast offline: $OFFLINE (2-offline.png)"
ui | grep -qE "$DETAILS" || fail "Offline: the Home tab has no hourly forecast (2-offline.png)"
# The conditions are further down the list.
ui | grep -qE "$CONDITIONS" || { adb shell input swipe 500 1500 500 600 300; sleep 1; }
ui | grep -qE "$CONDITIONS" || { shot 3-offline-conditions; fail "Offline: the Home tab has no weather conditions (3-offline-conditions.png)"; }
shot 3-offline-conditions
pass "the Home tab's hourly forecast and conditions show offline too (3-offline-conditions.png)"
ui | grep -qE "$ERROR" && fail "Offline: the load error snackbar also showed"
pass "no load error offline"

step "Offline with no saved forecast: the error says so"
adb shell am force-stop $PKG
adb shell run-as $PKG rm -rf cache
open_app_at_london
wait_for "$OFFLINE_ERROR" 30 || { shot 4-offline-no-cache; fail "Offline with an empty cache, Home didn't say it's offline (4-offline-no-cache.png)"; }
ui | grep -qE "$TRY_AGAIN" || { shot 4-offline-no-cache; fail "The offline message has no 'Try again' (4-offline-no-cache.png)"; }
shot 4-offline-no-cache
pass "offline with no saved forecast, Home says so and offers 'Try again' (4-offline-no-cache.png)"
check_no_crash

adb shell svc power stayon false >/dev/null 2>&1 || true
echo "Done. Artifacts in $OUT"
cat "$OUT/report.md"
