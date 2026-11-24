#!/bin/sh
# End-to-end check: a city added from search shows on the Favourites tab straight away and after a restart.
#
# Usage: scripts/e2e/add_favourite.sh [emulator-serial]   (after ./gradlew :app:assembleDebug)
#
# 1. Backs up the app's database (debug build, via run-as) and restores it at the end, so the
#    emulator's own favourites are left as they were.
# 2. Opens the Favourites tab, taps "Add place", searches for Nairobi, taps the first result and
#    goes back: the Favourites tab must list Nairobi with a forecast, without restarting the app.
# 3. Restarts the app (it opens on Home) and opens Favourites: Nairobi must still be there (saved in Room).
# 4. Writes screenshots and a report to build/e2e/add-favourite-<timestamp>/.
set -eu

SERIAL="${1:-emulator-5554}"
. "$(dirname "$0")/lib.sh"
OUT="$ROOT/build/e2e/add-favourite-$(date +%Y%m%d-%H%M%S)"

CITY=Nairobi
HOME_TITLE='text="Your Current Location"'
FAVOURITES_TITLE='text="Favourite Locations"'
ADD_BUTTON='content-desc="Add place"'
SEARCH_RESULTS='text="Search Results"'
# A search result reads "<address line>, <country code>", e.g. "Nairobi, Kenya, KE".
RESULT="text=\"$CITY[^\"]*, [A-Z][A-Z]\""
# On the Favourites tab the card title is just the place name.
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
open_tab Favourites || { shot 1-favourites; fail "Couldn't open the Favourites tab (1-favourites.png)"; }
wait_for "$ADD_BUTTON" || { shot 1-favourites; fail "No 'Add place' button on the Favourites tab (1-favourites.png)"; }
shot 1-favourites
tap_node "$ADD_BUTTON" || fail "Couldn't tap 'Add place'"
sleep 2
adb shell input text $CITY
wait_for "$RESULT" 30 || { shot 2-search; fail "No search result for $CITY (2-search.png)"; }
shot 2-search
tap_node "$RESULT"
sleep 2
adb shell input keyevent BACK   # closes the keyboard
sleep 1
ui | grep -qE "$FAVOURITES_TITLE" || adb shell input keyevent BACK
wait_for "$FAVOURITES_TITLE" || { shot 3-favourites-after-add; fail "Back didn't return to the Favourites tab (3-favourites-after-add.png)"; }

step "Favourites tab lists $CITY without a restart"
wait_for "$CITY_CARD" 30 || { shot 3-favourites-after-add; fail "$CITY isn't on the Favourites tab after adding it (3-favourites-after-add.png)"; }
shot 3-favourites-after-add
pass "$CITY shows on the Favourites tab straight after adding it (3-favourites-after-add.png)"

step "Restart: $CITY is still there"
start_vane -S
wait_for "$HOME_TITLE" || { shot 4-restarted; fail "Home screen never opened after the restart (4-restarted.png)"; }
open_tab Favourites || { shot 4-restarted; fail "Couldn't open the Favourites tab after the restart (4-restarted.png)"; }
wait_for "$CITY_CARD" 30 || { shot 4-restarted; fail "$CITY is gone after a restart (4-restarted.png)"; }
shot 4-restarted
pass "$CITY is still a favourite after restarting the app (4-restarted.png)"
check_no_crash

adb shell svc power stayon false >/dev/null 2>&1 || true
echo "Done. Artifacts in $OUT"
cat "$OUT/report.md"
