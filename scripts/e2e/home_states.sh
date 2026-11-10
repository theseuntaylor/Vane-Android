#!/bin/sh
# End-to-end check: the home screen never stays on a spinner when the location can't be found.
#
# Usage: scripts/e2e/home_states.sh [emulator-serial]
#
# 1. Turns device location off and opens the app: the current-location section must say location
#    is off and offer "Try again", and the favourites section must finish loading on screen.
# 2. Turns location back on and taps "Try again": the current-location forecast must load.
# 3. Writes screenshots and a report to build/e2e/home-states-<timestamp>/.
#
# Saved favourites are left alone, so the favourites section may show cards or the empty hint.
set -eu

SERIAL="${1:-emulator-5554}"
. "$(dirname "$0")/lib.sh"
OUT="$ROOT/build/e2e/home-states-$(date +%Y%m%d-%H%M%S)"

LOCATION_OFF='text="Location is turned off. Turn it on to see the weather where you are."'
TRY_AGAIN='text="Try again"'
FAVOURITES_DONE='text="Tap \+ to add a favourite location."|text="-?[0-9]+°"'
TEMPERATURE='text="-?[0-9]+°"'
HOME_TITLE='text="Your Current Location"'

# Height of the screen in pixels, to check that a node is actually visible.
screen_height() { adb shell wm size | sed -n 's/.*: [0-9]*x\([0-9]*\).*/\1/p' | tail -1; }
# Bottom edge of the first node matching the regex $1.
node_bottom() { ui | grep -E "$1" | head -1 | sed -n 's/.*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,\([0-9]*\)\]".*/\1/p'; }

start_report "Home screen states E2E"
trap 'adb shell cmd location set-location-enabled true' EXIT

step "Location off: open the app"
wake_device
network_on
wait_for_network
adb install -r "$APK" >/dev/null
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb shell cmd location set-location-enabled false
adb logcat -c
start_vane -S
wait_for "$HOME_TITLE" || { shot 1-location-off; fail "Home screen never opened (1-location-off.png)"; }
wait_for "$LOCATION_OFF" 15 || { shot 1-location-off; fail "No 'location is off' message within 15 s (1-location-off.png)"; }
ui | grep -qE "$TRY_AGAIN" || { shot 1-location-off; fail "No 'Try again' button (1-location-off.png)"; }
pass "with location off, the current-location section says so and offers 'Try again'"
wait_for "$FAVOURITES_DONE" 30 || { shot 1-location-off; fail "Favourites section never finished loading (1-location-off.png)"; }
bottom=$(node_bottom "$FAVOURITES_DONE")
[ -n "$bottom" ] && [ "$bottom" -le "$(screen_height)" ] || fail "Favourites content is off screen (bottom $bottom)"
shot 1-location-off
pass "favourites section finished loading and is on screen (1-location-off.png)"
# Favourite cards also show temperatures; the current-location card adds one more.
favourite_temperatures=$(ui | grep -cE "$TEMPERATURE" || true)

step "Location on: tap Try again"
adb shell cmd location set-location-enabled true
adb emu geo fix -0.1276 51.5072 >/dev/null
tap_node "$TRY_AGAIN" || fail "Couldn't tap 'Try again'"
# The emulator only reports GPS fixes it is sent while a request is active.
i=0
until [ "$(ui | grep -cE "$TEMPERATURE" || true)" -gt "$favourite_temperatures" ]; do
    i=$((i + 1)); [ $i -gt 45 ] && { shot 2-retried; fail "Current-location forecast didn't load after 'Try again' (2-retried.png)"; }
    adb emu geo fix -0.1276 51.5072 >/dev/null
    sleep 1
done
shot 2-retried
pass "after turning location on, 'Try again' loads the current-location forecast (2-retried.png)"
check_no_crash

adb shell svc power stayon false >/dev/null 2>&1 || true
echo "Done. Artifacts in $OUT"
cat "$OUT/report.md"
