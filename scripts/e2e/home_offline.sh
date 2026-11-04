#!/bin/sh
# End-to-end check: the home screen shows the last forecast when the device is offline.
#
# Usage: scripts/e2e/home_offline.sh [emulator-serial]
#
# 1. Online: installs the debug APK with an empty HTTP cache, opens the app and waits for the
#    current-location forecast.
# 2. Offline: closes the app, turns on airplane mode, reopens it at the same location and checks
#    that the same temperature shows with no error.
# 3. Writes screenshots and a report to build/e2e/home-offline-<timestamp>/.
#
# Use an emulator: the script toggles airplane mode and sends GPS fixes.
set -eu

SERIAL="${1:-emulator-5554}"
. "$(dirname "$0")/lib.sh"
OUT="$ROOT/build/e2e/home-offline-$(date +%Y%m%d-%H%M%S)"

# A temperature on a city card, e.g. text="12°" or text="-3°".
TEMPERATURE='text="-?[0-9]+°"'
ERROR='text="There was an error loading forecasts"'

current_temperature() { ui | grep -oE "$TEMPERATURE" | head -1; }
open_app_at_london() {
    adb emu geo fix -0.1276 51.5072 >/dev/null
    adb shell am start -S -W -n $PKG/.MainActivity >/dev/null
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
shot 2-offline
[ "$OFFLINE" = "$ONLINE" ] || fail "Offline temperature $OFFLINE differs from online $ONLINE"
pass "home screen showed the cached forecast offline: $OFFLINE (2-offline.png)"
ui | grep -qE "$ERROR" && fail "Offline: the load error snackbar also showed"
pass "no load error offline"
check_no_crash

adb shell svc power stayon false >/dev/null 2>&1 || true
echo "Done. Artifacts in $OUT"
cat "$OUT/report.md"
