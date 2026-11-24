#!/bin/sh
# End-to-end check: the bottom navigation tabs, and the appearance setting surviving a restart.
#
# Usage: scripts/e2e/navigation_appearance.sh [emulator-serial]   (after ./gradlew :app:assembleDebug)
#
# 1. Opens the app: it starts on Home, with a bottom bar of Home, Favourites and Settings.
# 2. Switches to each tab and checks its content: Favourites has "Add place", Settings has the
#    appearance options and the rain and snow alerts switch. Back from a tab returns to Home.
# 3. Picks Dark, restarts the app: Home is dark, and Settings still shows Dark.
# 4. Picks Light with the device in dark mode, restarts: Home is light.
# 5. Picks System default: Home follows the device's dark mode, on and off.
# Dark or light is read from the screenshot's background (the left edge of the screen, outside
# the cards), and compared with the theme's surface colours from Color.kt (generated from design/seed).
# The app's saved settings and the device's dark mode are restored at the end.
# Writes screenshots and a report to build/e2e/navigation-appearance-<timestamp>/.
set -eu

SERIAL="${1:-emulator-5554}"
. "$(dirname "$0")/lib.sh"
OUT="$ROOT/build/e2e/navigation-appearance-$(date +%Y%m%d-%H%M%S)"

HOME_TITLE='text="Your Current Location"'
FAVOURITES_TITLE='text="Favourite Locations"'
ADD_PLACE='content-desc="Add place"'
SYSTEM_DEFAULT='text="System default"'
LIGHT='text="Light"'
DARK='text="Dark"'

ORIGINAL_NIGHT_MODE=""

# "dark" or "light", from the background colour at the left edge of screenshot $1.png.
# The light surface is #F8F9FF and the dark one #111318, so the brightness is far from 128 either way.
background_of() {
    # shellcheck disable=SC2046
    set -- $(python3 "$ROOT/scripts/e2e/png_pixel.py" "$OUT/$1.png" 3 0.6)
    if [ $(($1 + $2 + $3)) -lt 384 ]; then echo dark; else echo light; fi
}
# Checks that screenshot $1 has a $2 ("dark" or "light") background.
expect_background() {
    actual=$(background_of "$1")
    [ "$actual" = "$2" ] || fail "Expected a $2 background, got $actual ($1.png)"
}
# Appearance option $1 ("System default", "Light" or "Dark") is the checked radio button.
expect_option_selected() {
    wait_for_state "$1" checked || { shot "$2"; fail "$1 isn't selected ($2.png)"; }
}
# Starts the app fresh and waits for the Home tab; Home's background still shows while the
# location loads, so there's no need to wait for the forecast.
restart_on_home() {
    start_vane -S
    wait_for "$HOME_TITLE" 30 || { shot "$1"; fail "Home tab never opened ($1.png)"; }
    sleep 1
    shot "$1"
}
night_mode() { adb shell cmd uimode night "$1" >/dev/null; }

cleanup() {
    adb shell am force-stop $PKG
    adb shell run-as $PKG sh -c "'rm -f shared_prefs/vane_settings.xml; \
        [ -f files/e2e-vane_settings.xml ] && mv files/e2e-vane_settings.xml shared_prefs/vane_settings.xml; true'" \
        || echo "WARNING: couldn't restore the appearance setting"
    [ -n "$ORIGINAL_NIGHT_MODE" ] && night_mode "$ORIGINAL_NIGHT_MODE"
    adb shell svc power stayon false >/dev/null 2>&1 || true
}

start_report "Navigation and appearance E2E"

step "Install, back up the appearance setting"
wake_device
adb install -r "$APK" >/dev/null
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb shell am force-stop $PKG
# "Night mode: yes", "no" or "auto"; restored at the end.
ORIGINAL_NIGHT_MODE=$(adb shell cmd uimode night | sed -n 's/.*: //p' | tr -d '\r')
adb shell run-as $PKG sh -c "'mkdir -p files shared_prefs; rm -f files/e2e-vane_settings.xml; \
    [ -f shared_prefs/vane_settings.xml ] && mv shared_prefs/vane_settings.xml files/e2e-vane_settings.xml; true'"
trap cleanup EXIT
night_mode no
adb logcat -c
pass "appearance starts at System default; the device starts in light mode (was: $ORIGINAL_NIGHT_MODE)"

step "Tabs: Home first, then Favourites, Settings and back"
restart_on_home 1-home
for tab in Home Favourites Settings; do
    ui | grep -qE "text=\"$tab\"" || fail "No $tab tab in the bottom bar (1-home.png)"
done
has_state Home selected || fail "The Home tab isn't selected at launch (1-home.png)"
pass "the app opens on Home, with Home, Favourites and Settings tabs (1-home.png)"

open_tab Favourites || { shot 2-favourites; fail "Couldn't open the Favourites tab (2-favourites.png)"; }
wait_for "$FAVOURITES_TITLE" 10 && wait_for "$ADD_PLACE" 10 || { shot 2-favourites; fail "Favourites tab has no list title or 'Add place' (2-favourites.png)"; }
ui | grep -qE "$HOME_TITLE" && { shot 2-favourites; fail "The current location shows on the Favourites tab (2-favourites.png)"; }
shot 2-favourites
pass "Favourites tab shows the saved places and 'Add place', not the current location (2-favourites.png)"

open_tab Settings || { shot 3-settings; fail "Couldn't open the Settings tab (3-settings.png)"; }
for option in "$SYSTEM_DEFAULT" "$LIGHT" "$DARK"; do
    wait_for "$option" 10 || { shot 3-settings; fail "No $option option on the Settings tab (3-settings.png)"; }
done
ui | grep -qE "$ALERTS_SWITCH" || { shot 3-settings; fail "No alerts switch on the Settings tab (3-settings.png)"; }
expect_option_selected "System default" 3-settings
shot 3-settings
pass "Settings tab shows System default (selected), Light, Dark and the alerts switch (3-settings.png)"

adb shell input keyevent BACK
wait_for_state Home selected || { shot 4-back; fail "Back from Settings didn't return to Home (4-back.png)"; }
wait_for "$HOME_TITLE" 10 || { shot 4-back; fail "Back from Settings didn't show Home (4-back.png)"; }
shot 4-back
pass "Back from a tab returns to Home (4-back.png)"
expect_background 4-back light
pass "with System default and the device in light mode, the app is light (4-back.png)"

step "Dark: survives a restart"
open_tab Settings || fail "Couldn't open the Settings tab"
tap_node "$DARK" || fail "Couldn't tap Dark"
expect_option_selected Dark 5-dark-picked
sleep 1
shot 5-dark-picked
expect_background 5-dark-picked dark
pass "picking Dark turns the app dark at once (5-dark-picked.png)"
restart_on_home 6-dark-restarted
expect_background 6-dark-restarted dark
pass "after a restart, Home is still dark (6-dark-restarted.png)"
open_tab Settings || fail "Couldn't open the Settings tab after the restart"
expect_option_selected Dark 7-dark-settings
shot 7-dark-settings
pass "after a restart, Settings still shows Dark (7-dark-settings.png)"

step "Light: survives a restart, even with the device in dark mode"
night_mode yes
sleep 2   # the activity restarts for the new night mode
wait_for "$LIGHT" 10 || { shot 8-light-picked; fail "Settings didn't come back after the night mode change (8-light-picked.png)"; }
tap_node "$LIGHT" || fail "Couldn't tap Light"
expect_option_selected Light 8-light-picked
restart_on_home 8-light-restarted
expect_background 8-light-restarted light
pass "with Light picked and the device in dark mode, Home is light after a restart (8-light-restarted.png)"

step "System default: follows the device"
open_tab Settings || fail "Couldn't open the Settings tab"
tap_node "$SYSTEM_DEFAULT" || fail "Couldn't tap System default"
expect_option_selected "System default" 9-system-dark
restart_on_home 9-system-dark
expect_background 9-system-dark dark
pass "System default with the device in dark mode: Home is dark (9-system-dark.png)"
night_mode no
restart_on_home 10-system-light
expect_background 10-system-light light
pass "System default with the device in light mode: Home is light (10-system-light.png)"

check_no_crash
echo "Done. Artifacts in $OUT"
cat "$OUT/report.md"
