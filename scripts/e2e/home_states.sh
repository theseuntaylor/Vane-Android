#!/bin/sh
# End-to-end check: the Home tab never stays on a spinner when the location can't be found.
#
# Usage: scripts/e2e/home_states.sh [emulator-serial]
#
# 1. Turns device location off and opens the app: the Home tab must say location is off and offer
#    "Try again", on screen.
# 2. Opens the Favourites tab: it must finish loading (cards or the empty hint) even with location
#    off, then goes back to Home.
# 3. Turns location back on and taps "Try again": the current-location forecast must load.
# 4. Writes screenshots and a report to build/e2e/home-states-<timestamp>/.
#
# Saved favourites are left alone, so the Favourites tab may show cards or the empty hint.
set -eu

SERIAL="${1:-emulator-5554}"
. "$(dirname "$0")/lib.sh"
OUT="$ROOT/build/e2e/home-states-$(date +%Y%m%d-%H%M%S)"

LOCATION_OFF='text="Location is turned off. Turn it on to see the weather where you are."'
TRY_AGAIN='text="Try again"'
FAVOURITES_DONE='text="Tap Add place to save a place and see its weather here."|text="-?[0-9]+°"'
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
# Play services may warn that device location is off; that dialog hides the app from the UI dump.
sleep 3
ui | grep -q 'text="No location access"' && tap_node 'text="Close"' && sleep 1
wait_for "$HOME_TITLE" || { shot 1-location-off; fail "Home screen never opened (1-location-off.png)"; }
wait_for "$LOCATION_OFF" 15 || { shot 1-location-off; fail "No 'location is off' message within 15 s (1-location-off.png)"; }
ui | grep -qE "$TRY_AGAIN" || { shot 1-location-off; fail "No 'Try again' button (1-location-off.png)"; }
bottom=$(node_bottom "$TRY_AGAIN")
[ -n "$bottom" ] && [ "$bottom" -le "$(screen_height)" ] || fail "'Try again' is off screen (bottom $bottom)"
shot 1-location-off
pass "with location off, the Home tab says so and offers 'Try again' on screen (1-location-off.png)"

step "Location off: the Favourites tab still loads"
open_tab Favourites || { shot 2-favourites; fail "Couldn't open the Favourites tab (2-favourites.png)"; }
wait_for "$FAVOURITES_DONE" 30 || { shot 2-favourites; fail "Favourites tab never finished loading (2-favourites.png)"; }
shot 2-favourites
pass "Favourites tab finished loading with location off (2-favourites.png)"
open_tab Home || fail "Couldn't go back to the Home tab"
wait_for "$TRY_AGAIN" 10 || { shot 2-home; fail "Home tab lost its 'Try again' button (2-home.png)"; }

step "Location on: tap Try again"
adb shell cmd location set-location-enabled true
adb emu geo fix -0.1276 51.5072 >/dev/null
tap_node "$TRY_AGAIN" || fail "Couldn't tap 'Try again'"
# The emulator only reports GPS fixes it is sent while a request is active.
i=0
until ui | grep -qE "$TEMPERATURE"; do
    i=$((i + 1)); [ $i -gt 45 ] && { shot 3-retried; fail "Current-location forecast didn't load after 'Try again' (3-retried.png)"; }
    adb emu geo fix -0.1276 51.5072 >/dev/null
    sleep 1
done
shot 3-retried
pass "after turning location on, 'Try again' loads the current-location forecast (3-retried.png)"
check_no_crash

adb shell svc power stayon false >/dev/null 2>&1 || true
echo "Done. Artifacts in $OUT"
cat "$OUT/report.md"
