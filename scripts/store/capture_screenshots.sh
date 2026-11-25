#!/bin/sh
# Takes the Play store phone screenshots on an emulator and writes them to
# fastlane/metadata/android/en-GB/images/phoneScreenshots/, replacing what is there.
#
# Usage: scripts/store/capture_screenshots.sh [emulator-serial]   (after ./gradlew :app:assembleDebug)
#
# 1. Backs up the app's database and settings (debug build, via run-as) and restores them at the
#    end, with the emulator's dark theme setting and status bar.
# 2. Starts from no favourites, the default theme and the 24-hour clock, puts the emulator in London, and adds
#    Paris, Tokyo and Nairobi as favourites through the Add place search.
# 3. Sets the screen to 1080x2160 while it runs: Play rejects screenshots whose long side is more
#    than twice the short side, and most phone screens (1080x2400) are taller than that.
# 4. Uses the emulator's demo mode for a clean status bar (09:41, full battery, full Wi-Fi, no
#    mobile signal and no notification icons).
# 5. Takes six screenshots: Home, a place's details, Favourites and Settings in light theme, then
#    Home and Favourites in dark theme. Before each one it waits for the screen's content and
#    checks there is no error message, permission prompt or loading spinner.
# 6. Only if every screenshot passes, replaces the screenshots folder and writes
#    images/capture-report.md, which scripts/e2e/store_listing.sh checks.
# Working files land in build/store-screenshots-<timestamp>/.
set -eu

SERIAL="${1:-emulator-5554}"
. "$(dirname "$0")/../e2e/lib.sh"
OUT="$ROOT/build/store-screenshots-$(date +%Y%m%d-%H%M%S)"
IMAGES="$ROOT/fastlane/metadata/android/en-GB/images"
DEST="$IMAGES/phoneScreenshots"

HOME_TITLE='text="Your Current Location"'
FAVOURITES_TITLE='text="Favourite Locations"'
DETAILS_TITLE='text="Today.s Hourly Forecast"'
CONDITIONS_TITLE='text="Today.s Weather Conditions"'
TEMPERATURE='text="-?[0-9]+°"'
ADD_BUTTON='content-desc="Add place"'
ALERTS_SWITCH='text="Rain and snow alerts"'
APPEARANCE='text="Appearance"'
CITIES="Paris Tokyo Nairobi"
# Anything that must never be in a store screenshot.
BAD_SCREEN='There was an error loading forecasts|Couldn.t find your current location|Location is turned off|Vane needs your location|Error getting your current Location|Can.t get the summary|Service not available|class="android.widget.ProgressBar"'

# London, sent again while waiting: the emulator only reports fixes it is sent during a request.
geo_fix() { adb emu geo fix -0.1276 51.5072 >/dev/null 2>&1 || true; }
wait_for_geo() {
    i=0
    until ui | grep -qE "$1"; do
        i=$((i + 1)); [ $i -gt "${2:-30}" ] && return 1
        geo_fix
        sleep 1
    done
}
demo_mode() {
    adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null
}
# Waits for every regex given, checks the screen is clean, then takes the screenshot $1.
capture() {
    name="$1"; what="$2"; shift 2
    for marker in "$@"; do
        wait_for_geo "$marker" 45 || { shot "failed-$name"; fail "$name: never showed $marker (failed-$name.png)"; }
    done
    sleep 2   # let icons and animations settle
    if ui | grep -qE "$BAD_SCREEN"; then
        shot "failed-$name"
        fail "$name: the screen shows an error, a prompt or a spinner (failed-$name.png)"
    fi
    adb exec-out screencap -p > "$OUT/shots/$name"
    echo "- PASS: $name $what" >> "$OUT/capture-report.md"
}
count_temperatures() { ui | grep -cE "$TEMPERATURE" || true; }

cleanup() {
    demo_mode exit || true
    adb shell wm size reset >/dev/null 2>&1 || true
    if [ "$CLOCK_BEFORE" = null ]; then
        adb shell settings delete system time_12_24 >/dev/null 2>&1 || true
    else
        adb shell settings put system time_12_24 "$CLOCK_BEFORE" >/dev/null 2>&1 || true
    fi
    adb shell cmd uimode night "$NIGHT_BEFORE" >/dev/null 2>&1 || true
    adb shell am force-stop $PKG
    adb shell run-as $PKG sh -c "'rm -rf databases shared_prefs && cp -r files/store-backup/databases files/store-backup/shared_prefs . && rm -rf files/store-backup'" \
        || echo "WARNING: couldn't restore the app's data from files/store-backup"
}

[ -f "$APK" ] || { echo "Build the APK first: ./gradlew :app:assembleDebug"; exit 1; }
mkdir -p "$OUT/shots"
echo "# Store screenshots — $(date)" > "$OUT/capture-report.md"
echo "Device: $SERIAL ($(adb shell getprop ro.product.model | tr -d '\r'), API $(adb shell getprop ro.build.version.sdk | tr -d '\r')), captured at 1080x2160" >> "$OUT/capture-report.md"
# report.md is what lib.sh's fail() writes to.
ln -s capture-report.md "$OUT/report.md"

step "Install, back up the app's data and start clean"
wake_device
network_on
wait_for_network
adb install -r "$APK" >/dev/null
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb shell cmd location set-location-enabled true
NIGHT_BEFORE=$(adb shell cmd uimode night | sed 's/.*: //' | tr -d '\r')
CLOCK_BEFORE=$(adb shell settings get system time_12_24 | tr -d '\r')
adb shell am force-stop $PKG
adb shell run-as $PKG sh -c "'rm -rf files/store-backup && mkdir -p files/store-backup databases shared_prefs && cp -r databases shared_prefs files/store-backup/'"
trap cleanup EXIT
adb shell run-as $PKG sh -c "'rm -rf databases shared_prefs'"
adb shell cmd uimode night no >/dev/null
# The listing is British English, so times show as 22:00, not 10:00 PM.
adb shell settings put system time_12_24 24

step "Add favourites: $CITIES"
geo_fix
start_vane -S
wait_for_geo "$HOME_TITLE" || { shot failed-start; fail "Home screen never opened (failed-start.png)"; }
for city in $CITIES; do
    open_tab Favourites || { shot "failed-add-$city"; fail "Couldn't open the Favourites tab (failed-add-$city.png)"; }
    tap_node "$ADD_BUTTON" || { shot "failed-add-$city"; fail "Couldn't tap Add place (failed-add-$city.png)"; }
    sleep 2
    adb shell input text "$city"
    # A search result reads "<address line>, <country code>", e.g. "Paris, France, FR".
    result="text=\"$city[^\"]*, [A-Z][A-Z]\""
    wait_for "$result" 30 || { shot "failed-add-$city"; fail "No search result for $city (failed-add-$city.png)"; }
    tap_node "$result"
    sleep 2
    adb shell input keyevent BACK   # closes the keyboard
    # A second Back only if the first one didn't leave the search screen; pressing it too early
    # would go on from Favourites to Home.
    wait_for "$FAVOURITES_TITLE" 5 || adb shell input keyevent BACK
    wait_for "$FAVOURITES_TITLE" || { shot "failed-add-$city"; fail "Back didn't return to Favourites after adding $city (failed-add-$city.png)"; }
done

step "Screen size 1080x2160 and a clean status bar"
adb shell wm size 1080x2160
sleep 2
adb shell settings put global sysui_demo_allowed 1
demo_mode enter
demo_mode clock -e hhmm 0941
demo_mode battery -e level 100 -e plugged false
demo_mode network -e wifi show -e level 4 -e fully true
demo_mode network -e mobile hide
demo_mode notifications -e visible false

for theme in light dark; do
    if [ $theme = dark ]; then adb shell cmd uimode night yes >/dev/null; else adb shell cmd uimode night no >/dev/null; fi
    step "Screenshots in $theme theme"
    start_vane -S
    if [ $theme = light ]; then
        capture 1_home.png "Home, London forecast with hourly forecast and conditions (light)" \
            "$HOME_TITLE" "$TEMPERATURE" "$DETAILS_TITLE"

        open_tab Favourites || fail "Couldn't open the Favourites tab"
        wait_for 'text="Paris"' 30 || { shot failed-details; fail "Paris isn't on the Favourites tab (failed-details.png)"; }
        tap_node 'text="Paris"' || fail "Couldn't tap Paris"
        capture 2_details.png "Paris details with hourly forecast and conditions (light)" \
            "$DETAILS_TITLE" "$CONDITIONS_TITLE" "$TEMPERATURE"
        adb shell input keyevent BACK
        wait_for "$FAVOURITES_TITLE" || fail "Back didn't return to the Favourites tab"

        capture 3_favourites.png "Favourites with Paris, Tokyo and Nairobi (light)" \
            "$FAVOURITES_TITLE" 'text="Paris"' 'text="Tokyo"' 'text="Nairobi"'
        [ "$(count_temperatures)" -ge 3 ] || fail "3_favourites.png: not every favourite shows a temperature"

        open_tab Settings || fail "Couldn't open the Settings tab"
        capture 4_settings.png "Settings with appearance and rain and snow alerts (light)" \
            "$APPEARANCE" "$ALERTS_SWITCH"
    else
        capture 5_home_dark.png "Home, London forecast (dark)" \
            "$HOME_TITLE" "$TEMPERATURE" "$DETAILS_TITLE"
        open_tab Favourites || fail "Couldn't open the Favourites tab"
        capture 6_favourites_dark.png "Favourites with Paris, Tokyo and Nairobi (dark)" \
            "$FAVOURITES_TITLE" 'text="Paris"' 'text="Tokyo"' 'text="Nairobi"'
        [ "$(count_temperatures)" -ge 3 ] || fail "6_favourites_dark.png: not every favourite shows a temperature"
    fi
done

step "Replace the store screenshots"
rm -rf "$DEST"
mkdir -p "$DEST"
cp "$OUT"/shots/*.png "$DEST/"
cp "$OUT/capture-report.md" "$IMAGES/capture-report.md"
echo "Done. Screenshots in $DEST; working files in $OUT"
grep -E '^(Device|- )' "$OUT/capture-report.md"
