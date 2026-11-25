#!/bin/sh
# End-to-end check: tapping a favourite on the home screen widget opens that place's details.
#
# Usage: scripts/e2e/widget_favourite_tap.sh [emulator-serial]   (after ./gradlew :app:assembleDebug)
#
# Needs at least one saved favourite (add one in the app, or run add_favourite.sh's steps by hand).
#
# 1. Installs the build and opens the app online, so it saves the favourites for the widget with
#    their coordinates (rows saved by older builds have none).
# 2. Makes sure a Vane widget is on the home screen (adds a 3 × 3 one through the Pixel launcher's
#    widget picker if needed) and that it lists the first favourite.
# 3. App closed: taps the favourite's row on the widget. Vane must open that place's details, and
#    Back must land on the Favourites tab.
# 4. App open on the Settings tab: goes home and taps the row again. The details must open, not
#    the Settings tab the app was left on. Then taps the second favourite, if any: it must open
#    that place, not the first. Rotates the details screen: one Back must still reach Favourites.
#    Restores the rotation settings at the end.
# 5. Writes screenshots and a report to build/e2e/widget-favourite-tap-<timestamp>/.
#
# Ways this could fail, which the steps above check: every row opens the same place (Android
# merges intents that differ only in extras); the tap opens Home instead of the details; Back
# leaves the app or lands on Home; a running app ignores the tap and stays where it was; the
# details open twice, or again after a rotation; a crash on any of these (logcat).
set -eu

SERIAL="${1:-emulator-5554}"
. "$(dirname "$0")/lib.sh"
OUT="$ROOT/build/e2e/widget-favourite-tap-$(date +%Y%m%d-%H%M%S)"

HOME_TITLE='text="Your Current Location"'
FAVOURITES_TITLE='text="Favourite Locations"'
HOURLY='text="Today.s Hourly Forecast"'
WIDGET_CELL='content-desc="Vane Weather widget[^"]*"'

widget_bound() { adb shell dumpsys appwidget | grep -q "provider=ProviderId{.*cmp:ComponentInfo{$PKG/$PKG.feature.widget.ui.VaneWeatherWidgetReceiver}"; }
# The widget's saved favourites, as the JSON the app stored (shared preferences escape the quotes).
widget_favourites() { adb shell run-as $PKG cat shared_prefs/vane_widget.xml 2>/dev/null | grep '"favourites"' | sed 's/&quot;/"/g'; }
add_widget() {
    adb shell input keyevent HOME; sleep 1
    adb shell input swipe 540 1000 540 1000 1500; sleep 2
    tap_node 'text="Widgets"' || return 1; sleep 2
    tap_node 'text="Search"' || return 1; sleep 1
    adb shell input text Vane; sleep 2
    adb shell input keyevent 111; sleep 1
    tap_node 'text="Vane"' last || return 1; sleep 2
    tap_node "$WIDGET_CELL" || return 1; sleep 2
    tap_node 'text="Add"' || return 1; sleep 3
    adb shell input keyevent HOME; sleep 2
}
# Taps the widget row for $1 on the home screen and waits for its details screen.
tap_widget_row() {
    adb shell input keyevent HOME; sleep 2
    wait_for "text=\"$1\"" 20 || { shot "$2"; fail "The widget doesn't list $1 ($2.png)"; }
    tap_node "text=\"$1\""
    wait_for "$HOURLY" 30 || { shot "$2"; fail "Tapping $1 on the widget didn't open its details ($2.png)"; }
    ui | grep -qE "text=\"$1\"" || { shot "$2"; fail "The details screen isn't for $1 ($2.png)"; }
    [ "$(focused_package)" = "$PKG" ] || { shot "$2"; fail "Vane isn't in front after the tap ($2.png)"; }
}

start_report "Widget favourite tap E2E"

step "Install and open the app online"
wake_device
network_on
wait_for_network
adb install -r "$APK" >/dev/null
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb logcat -c
adb emu geo fix -0.1276 51.5072 >/dev/null
start_vane -S
i=0
until widget_favourites | grep -q '"latitude"'; do
    i=$((i + 1)); [ $i -gt 45 ] && fail "The app never saved the widget's favourites with coordinates (is there a saved favourite?)"
    sleep 1
done
widget_favourites > "$OUT/widget-favourites.json"
# Names in the order the widget lists them.
NAMES=$(grep -oE '"name":"[^"]*"' "$OUT/widget-favourites.json" | sed 's/"name":"\(.*\)"/\1/')
FIRST=$(echo "$NAMES" | sed -n 1p)
SECOND=$(echo "$NAMES" | sed -n 2p)
[ -n "$FIRST" ] || fail "Couldn't read the first widget favourite (widget-favourites.json)"
pass "the app saved the widget's favourites with coordinates; first is $FIRST (widget-favourites.json)"

step "Make sure a Vane widget is on the home screen"
widget_bound || add_widget || true
widget_bound || fail "No Vane widget on the home screen. Add a 3 × 3 one by hand and re-run."
adb shell input keyevent HOME; sleep 2
wait_for "text=\"$FIRST\"" 30 || { shot 1-widget; fail "The widget doesn't list $FIRST; make it at least 3 × 3 (1-widget.png)"; }
shot 1-widget
pass "the widget lists $FIRST (1-widget.png)"

step "App closed: tap $FIRST on the widget"
# Kill the process the way Android does in the background. Not force-stop: that cancels the
# app's pending intents, so the widget's taps would do nothing until the app is opened again.
adb shell input keyevent HOME; sleep 1
adb shell am kill $PKG
tap_widget_row "$FIRST" 2-details-cold
shot 2-details-cold
pass "with the app closed, tapping $FIRST opened its details (2-details-cold.png)"
adb shell input keyevent BACK
wait_for "$FAVOURITES_TITLE" 10 || { shot 2-back; fail "Back from the details didn't land on the Favourites tab (2-back.png)"; }
has_state Favourites selected || { shot 2-back; fail "Back landed on the favourites list, but the Favourites tab isn't selected (2-back.png)"; }
shot 2-back
pass "Back from the details lands on the Favourites tab (2-back.png)"

step "App open on Settings: tap $FIRST again"
open_tab Settings || { shot 3-settings; fail "Couldn't open the Settings tab (3-settings.png)"; }
tap_widget_row "$FIRST" 3-details-warm
shot 3-details-warm
pass "with the app open on Settings, tapping $FIRST opened its details (3-details-warm.png)"

if [ -n "$SECOND" ]; then
    step "Tap $SECOND: each row opens its own place"
    tap_widget_row "$SECOND" 3-details-second
    ui | grep -qE "text=\"$FIRST\"" && { shot 3-details-second; fail "Tapping $SECOND opened $FIRST (3-details-second.png)"; }
    shot 3-details-second
    pass "tapping $SECOND opened $SECOND, not $FIRST (3-details-second.png)"
else
    echo "- SKIP: only one favourite, so rows opening the same place isn't checked" >> "$OUT/report.md"
fi

step "Rotate: the details open once"
AUTO_ROTATE=$(adb shell settings get system accelerometer_rotation | tr -d '\r')
USER_ROTATION=$(adb shell settings get system user_rotation | tr -d '\r')
trap 'adb shell settings put system user_rotation "$USER_ROTATION"; adb shell settings put system accelerometer_rotation "$AUTO_ROTATE"' EXIT
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1; sleep 3
adb shell settings put system user_rotation 0; sleep 3
adb shell input keyevent BACK
wait_for "$FAVOURITES_TITLE" 10 || { shot 4-rotated-back; fail "After a rotation, one Back didn't return to Favourites; the details opened twice (4-rotated-back.png)"; }
shot 4-rotated-back
pass "after a rotation, one Back returns to Favourites (4-rotated-back.png)"
check_no_crash

adb shell svc power stayon false >/dev/null 2>&1 || true
echo "Done. Artifacts in $OUT"
cat "$OUT/report.md"
