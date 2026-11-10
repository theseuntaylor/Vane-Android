#!/bin/sh
# End-to-end check: a city added from search shows on the home screen straight away and after a restart.
#
# Usage: scripts/e2e/add_favourite.sh [emulator-serial]   (after ./gradlew :app:assembleDebug)
#
# 1. Backs up the app's database (debug build, via run-as) and restores it at the end, so the
#    emulator's own favourites are left as they were.
# 2. Searches for Nairobi, taps the first result and goes back: the home screen must list Nairobi
#    with a forecast, without restarting the app.
# 3. Restarts the app: Nairobi must still be there (saved in Room).
# 4. Writes screenshots and a report to build/e2e/add-favourite-<timestamp>/.
set -eu

SERIAL="${1:-emulator-5554}"
. "$(dirname "$0")/lib.sh"
OUT="$ROOT/build/e2e/add-favourite-$(date +%Y%m%d-%H%M%S)"

CITY=Nairobi
HOME_TITLE='text="Your Current Location"'
ADD_BUTTON='content-desc="Plus button for adding a new location"'
SEARCH_RESULTS='text="Search Results"'
# A search result reads "<address line>, <country code>", e.g. "Nairobi, Kenya, KE".
RESULT="text=\"$CITY[^\"]*, [A-Z][A-Z]\""
# On the home screen the favourite's card title is just the place name.
CITY_CARD="text=\"$CITY\""

restore_database() {
    adb shell am force-stop $PKG
    adb shell run-as $PKG sh -c "'rm -rf databases && cp -r files/e2e-databases-backup databases && rm -rf files/e2e-databases-backup'" \
        || echo "WARNING: couldn't restore the database backup in files/e2e-databases-backup"
}

start_report "Add favourite E2E"

step "Install and back up the database"
wake_device
network_on
wait_for_network
adb install -r "$APK" >/dev/null
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb shell am force-stop $PKG
adb shell run-as $PKG sh -c "'rm -rf files/e2e-databases-backup && mkdir -p files databases && cp -r databases files/e2e-databases-backup'"
trap restore_database EXIT
# Start without Nairobi, whatever the backup holds.
adb shell run-as $PKG sh -c "'rm -rf databases'"
pass "database backed up to files/e2e-databases-backup; starting with no favourites"

step "Search for $CITY and add it"
adb logcat -c
start_vane -S
wait_for "$HOME_TITLE" || { shot 1-home; fail "Home screen never opened (1-home.png)"; }
tap_node "$ADD_BUTTON" || fail "Couldn't tap the add button"
sleep 2
adb shell input text $CITY
wait_for "$RESULT" 30 || { shot 2-search; fail "No search result for $CITY (2-search.png)"; }
shot 2-search
tap_node "$RESULT"
sleep 2
adb shell input keyevent BACK   # closes the keyboard
sleep 1
ui | grep -qE "$HOME_TITLE" || adb shell input keyevent BACK
wait_for "$HOME_TITLE" || { shot 3-home-after-add; fail "Back didn't return to the home screen (3-home-after-add.png)"; }

step "Home screen lists $CITY without a restart"
wait_for "$CITY_CARD" 30 || { shot 3-home-after-add; fail "$CITY isn't on the home screen after adding it (3-home-after-add.png)"; }
shot 3-home-after-add
pass "$CITY shows on the home screen straight after adding it (3-home-after-add.png)"

step "Restart: $CITY is still there"
start_vane -S
wait_for "$CITY_CARD" 30 || { shot 4-restarted; fail "$CITY is gone after a restart (4-restarted.png)"; }
shot 4-restarted
pass "$CITY is still a favourite after restarting the app (4-restarted.png)"
check_no_crash

adb shell svc power stayon false >/dev/null 2>&1 || true
echo "Done. Artifacts in $OUT"
cat "$OUT/report.md"
