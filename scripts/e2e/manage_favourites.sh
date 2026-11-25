#!/bin/sh
# End-to-end check: favourites can be moved, renamed and deleted (with Undo), and an upgrade from a
# build without the list order keeps the saved places.
#
# Usage: BASE_APK=<older build> scripts/e2e/manage_favourites.sh [emulator-serial]
#        (after ./gradlew :app:assembleDebug; BASE_APK defaults to build/e2e/baseline/master.apk)
#
# 1. Backs up the app's database (debug build, via run-as) and restores it at the end.
# 2. Installs BASE_APK (database version 1), adds Nairobi then Lagos from search, then installs
#    the new build over it: both places must still show, Nairobi first (the migration keeps the
#    order they were added in).
# 3. Moves Lagos up from its menu: Lagos is first, also after a restart.
# 4. Renames Nairobi to "Work": the card says Work, also after a restart.
# 5. Deletes Lagos and taps Undo: Lagos is back. Deletes it again: it stays gone after a restart.
# 6. Writes screenshots and a report to build/e2e/manage-favourites-<timestamp>/.
#
# Ways this could fail, which the steps above check: the migration crashes or drops places (2);
# new places land at the top instead of the end (2); a move isn't saved or reverses on the next
# Room emission (3); a rename is saved but the card still shows the old name (4); Undo doesn't
# bring the place back, or brings it back at the wrong spot (5); a delete comes back after a
# restart (5); any of these crash the app (logcat).
set -eu

SERIAL="${1:-emulator-5554}"
. "$(dirname "$0")/lib.sh"
OUT="$ROOT/build/e2e/manage-favourites-$(date +%Y%m%d-%H%M%S)"
BASE_APK="${BASE_APK:-$ROOT/build/e2e/baseline/master.apk}"

HOME_TITLE='text="Your Current Location"'
FAVOURITES_TITLE='text="Favourite Locations"'
ADD_BUTTON='content-desc="Add place"'

# The top edge of the first node whose text is exactly $1, or nothing.
top_of() {
    ui | grep -E "text=\"$1\"" | head -1 | sed -n 's/.*bounds="\[[0-9]*,\([0-9]*\)\].*/\1/p'
}
# Succeeds if the card $1 is above the card $2.
is_above() {
    a=$(top_of "$1"); b=$(top_of "$2")
    [ -n "$a" ] && [ -n "$b" ] && [ "$a" -lt "$b" ]
}
open_favourites() {
    start_vane -S
    wait_for "$HOME_TITLE" || { shot "$1"; fail "Home screen never opened ($1.png)"; }
    open_tab Favourites || { shot "$1"; fail "Couldn't open the Favourites tab ($1.png)"; }
}
add_place() {
    tap_node "$ADD_BUTTON" || fail "Couldn't tap 'Add place'"
    sleep 2
    adb shell input text "$1"
    # A search result reads "<address line>, <country code>", e.g. "Nairobi, Kenya, KE".
    wait_for "text=\"$1[^\"]*, [A-Z][A-Z]\"" 30 || { shot "search-$1"; fail "No search result for $1 (search-$1.png)"; }
    tap_node "text=\"$1[^\"]*, [A-Z][A-Z]\""
    sleep 2
    adb shell input keyevent BACK   # closes the keyboard
    sleep 1
    ui | grep -qE "$FAVOURITES_TITLE" || adb shell input keyevent BACK
    wait_for "text=\"$1\"" 30 || { shot "added-$1"; fail "$1 isn't on the Favourites tab after adding it (added-$1.png)"; }
}
# Opens the menu on the card $1 and taps the item $2.
menu() {
    tap_node "content-desc=\"More options for $1\"" || fail "No menu button for $1"
    wait_for "text=\"$2\"" 5 || { shot "menu-$1"; fail "The menu for $1 has no '$2' (menu-$1.png)"; }
    tap_node "text=\"$2\""
    sleep 1
}
restore_database() {
    adb shell am force-stop $PKG
    adb shell run-as $PKG sh -c "'rm -rf databases && cp -r files/e2e-databases-backup databases && rm -rf files/e2e-databases-backup'" \
        || echo "WARNING: couldn't restore the database backup in files/e2e-databases-backup"
}

[ -f "$BASE_APK" ] || { echo "Set BASE_APK to a build from before the list order (e.g. master's)"; exit 1; }
start_report "Manage favourites E2E"
echo "Base build: $BASE_APK" >> "$OUT/report.md"

step "Install the base build and back up the database"
wake_device
network_on
wait_for_network
adb install -r "$BASE_APK" >/dev/null
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb shell am force-stop $PKG
adb shell run-as $PKG sh -c "'rm -rf files/e2e-databases-backup && mkdir -p files databases && cp -r databases files/e2e-databases-backup'"
trap restore_database EXIT
adb shell run-as $PKG sh -c "'rm -rf databases'"
pass "database backed up to files/e2e-databases-backup; starting with no favourites"

step "Base build: add Nairobi, then Lagos"
open_favourites 1-base
wait_for "$ADD_BUTTON" || { shot 1-base; fail "No 'Add place' button (1-base.png)"; }
add_place Nairobi
add_place Lagos
shot 1-base
pass "the base build saved Nairobi and Lagos (1-base.png)"

step "Upgrade to the new build"
adb logcat -c
adb install -r "$APK" >/dev/null
open_favourites 2-upgraded
wait_for 'text="Lagos"' 30 && wait_for 'text="Nairobi"' 5 || { shot 2-upgraded; fail "A favourite is missing after the upgrade (2-upgraded.png)"; }
shot 2-upgraded
is_above Nairobi Lagos || fail "After the upgrade Lagos is above Nairobi; the order they were added in was lost (2-upgraded.png)"
pass "after the upgrade both places show, in the order they were added (2-upgraded.png)"

step "Move Lagos up"
menu Lagos "Move up"
sleep 1
shot 3-moved
is_above Lagos Nairobi || fail "Lagos didn't move above Nairobi (3-moved.png)"
open_favourites 3-moved-restarted
wait_for 'text="Lagos"' 30 || { shot 3-moved-restarted; fail "Lagos is gone after a restart (3-moved-restarted.png)"; }
shot 3-moved-restarted
is_above Lagos Nairobi || fail "The new order wasn't kept after a restart (3-moved-restarted.png)"
pass "Lagos moved up, and stayed first after a restart (3-moved.png, 3-moved-restarted.png)"

step "Rename Nairobi to Work"
menu Nairobi Rename
wait_for 'text="Rename place"' 5 || { shot 4-rename; fail "The rename dialog didn't open (4-rename.png)"; }
# The dialog opens with the name selected, so typing replaces it.
sleep 1
adb shell input text Work
shot 4-rename
tap_node 'text="Save"' || fail "No Save button in the rename dialog"
wait_for 'text="Work"' 10 || { shot 4-renamed; fail "The card doesn't say Work after renaming (4-renamed.png)"; }
ui | grep -qE 'text="Nairobi"' && { shot 4-renamed; fail "Nairobi still shows after renaming it (4-renamed.png)"; }
shot 4-renamed
open_favourites 4-renamed-restarted
wait_for 'text="Work"' 30 || { shot 4-renamed-restarted; fail "The new name wasn't kept after a restart (4-renamed-restarted.png)"; }
shot 4-renamed-restarted
pass "Nairobi was renamed to Work, also after a restart (4-renamed.png, 4-renamed-restarted.png)"

step "Delete Lagos, Undo, then delete it again"
menu Lagos Delete
wait_for 'text="Removed Lagos"' 5 || { shot 5-deleted; fail "No 'Removed Lagos' snackbar (5-deleted.png)"; }
shot 5-deleted
ui | grep -qE 'text="Lagos"' && fail "Lagos still shows after deleting it (5-deleted.png)"
tap_node 'text="Undo"' || fail "The snackbar has no Undo"
wait_for 'text="Lagos"' 10 || { shot 5-undone; fail "Undo didn't bring Lagos back (5-undone.png)"; }
shot 5-undone
is_above Lagos Work || fail "Undo put Lagos back in the wrong place (5-undone.png)"
pass "deleting Lagos showed Undo, which put it back first (5-deleted.png, 5-undone.png)"
menu Lagos Delete
wait_for 'text="Removed Lagos"' 5 || fail "No snackbar for the second delete"
open_favourites 5-deleted-restarted
wait_for 'text="Work"' 30 || { shot 5-deleted-restarted; fail "Work is gone after the restart (5-deleted-restarted.png)"; }
ui | grep -qE 'text="Lagos"' && { shot 5-deleted-restarted; fail "Lagos came back after a restart (5-deleted-restarted.png)"; }
shot 5-deleted-restarted
pass "Lagos stayed deleted after a restart (5-deleted-restarted.png)"
check_no_crash

adb shell svc power stayon false >/dev/null 2>&1 || true
echo "Done. Artifacts in $OUT"
cat "$OUT/report.md"
