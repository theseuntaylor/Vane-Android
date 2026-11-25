#!/bin/sh
# End-to-end check: the units chosen in Settings change what Home shows, and survive a restart.
#
# Usage: scripts/e2e/units.sh [emulator-serial]   (after ./gradlew :app:assembleDebug)
#
# 1. Home shows the current temperature in °C (the default).
# 2. Settings: picks °F, mph, in and 12-hour.
# 3. Home: the temperature is the °C value converted (±1° for rounding), times read "h:mm AM/PM",
#    and the wind speed is in mph.
# 4. Restarts the app: Home still shows °F, and Settings still has °F, mph, in and 12-hour selected.
# The app's saved settings are restored at the end.
# Writes screenshots and a report to build/e2e/units-<timestamp>/.
set -eu

SERIAL="${1:-emulator-5554}"
. "$(dirname "$0")/lib.sh"
OUT="$ROOT/build/e2e/units-$(date +%Y%m%d-%H%M%S)"

HOME_TITLE='text="Your Current Location"'
TEMPERATURE='text="-?[0-9]+°"'
TWELVE_HOUR_TIME='text="[0-9]{1,2}:[0-9]{2} (AM|PM|am|pm)"'
TWENTY_FOUR_HOUR_TIME='text="[0-9]{2}:[0-9]{2}"'

# The first temperature on Home is the current location's, e.g. 9 for "9°".
current_temperature() { ui | grep -oE "$TEMPERATURE" | head -1 | grep -oE -- '-?[0-9]+'; }
# Waits for the current location's forecast, sending GPS fixes while the app asks for one.
wait_for_forecast() {
    i=0
    until ui | grep -qE "$TEMPERATURE"; do
        i=$((i + 1)); [ $i -gt 45 ] && { shot "$1"; fail "Home never showed a forecast ($1.png)"; }
        tap_node 'resource-id="android:id/aerr_wait"' || true
        adb emu geo fix -0.1276 51.5072 >/dev/null
        sleep 1
    done
}
# Scrolls the current screen down until a node matching $1 shows, up to 6 swipes.
scroll_to() {
    for _ in 1 2 3 4 5 6; do
        ui | grep -qE "$1" && return 0
        adb shell input swipe 540 1700 540 900 300
        sleep 1
    done
    ui | grep -qE "$1"
}
scroll_to_top() { for _ in 1 2 3; do adb shell input swipe 540 700 540 1900 200; done; sleep 1; }
pick() {
    scroll_to "text=\"$1\"" || { shot pick; fail "No '$1' option in Settings (pick.png)"; }
    tap_node "text=\"$1\"" || fail "Couldn't tap '$1'"
    wait_for_state "$1" checked 5 || { shot pick; fail "'$1' didn't become selected (pick.png)"; }
}

cleanup() {
    adb shell am force-stop $PKG
    adb shell run-as $PKG sh -c "'rm -f shared_prefs/vane_settings.xml; \
        [ -f files/e2e-vane_settings.xml ] && mv files/e2e-vane_settings.xml shared_prefs/vane_settings.xml; true'" \
        || echo "WARNING: couldn't restore the settings"
    adb shell svc power stayon false >/dev/null 2>&1 || true
}

start_report "Units E2E"

step "Install, back up the settings"
wake_device
network_on
wait_for_network
adb install -r "$APK" >/dev/null
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb shell cmd location set-location-enabled true
adb shell am force-stop $PKG
adb shell run-as $PKG sh -c "'mkdir -p files shared_prefs; rm -f files/e2e-vane_settings.xml; \
    [ -f shared_prefs/vane_settings.xml ] && mv shared_prefs/vane_settings.xml files/e2e-vane_settings.xml; true'"
trap cleanup EXIT
adb logcat -c
pass "settings start at their defaults (°C, km/h, mm, system clock)"

step "Home in °C"
start_vane -S
wait_for "$HOME_TITLE" 30 || { shot 1-celsius; fail "Home never opened (1-celsius.png)"; }
wait_for_forecast 1-celsius
celsius=$(current_temperature)
shot 1-celsius
pass "Home shows ${celsius}° by default (1-celsius.png)"

step "Settings: °F, mph, in, 12-hour"
open_tab Settings || fail "Couldn't open the Settings tab"
scroll_to 'text="Units"' || { shot 2-settings; fail "No Units section in Settings (2-settings.png)"; }
for option in "°F" "mph" "in" "12-hour"; do pick "$option"; done
shot 2-settings
pass "picked °F, mph, in and 12-hour in Settings (2-settings.png)"

step "Home in the new units"
open_tab Home || fail "Couldn't open the Home tab"
wait_for_forecast 3-fahrenheit
fahrenheit=$(current_temperature)
expected=$(python3 -c "print(round($celsius * 9 / 5 + 32))")
diff=$((fahrenheit - expected)); [ $diff -lt 0 ] && diff=$((-diff))
# The °C shown is already rounded, so allow 1° either way.
[ $diff -le 1 ] || { shot 3-fahrenheit; fail "Home shows ${fahrenheit}°, expected about ${expected}°F for ${celsius}°C (3-fahrenheit.png)"; }
ui | grep -qE "$TWELVE_HOUR_TIME" || { shot 3-fahrenheit; fail "No 12-hour time on Home (3-fahrenheit.png)"; }
ui | grep -E 'text="[^"]*"' | grep -qE "$TWENTY_FOUR_HOUR_TIME" && { shot 3-fahrenheit; fail "A 24-hour time is still on Home (3-fahrenheit.png)"; }
shot 3-fahrenheit
pass "Home shows ${fahrenheit}° for ${celsius}°C, and times like '$(ui | grep -oE "$TWELVE_HOUR_TIME" | head -1 | cut -d'"' -f2)' (3-fahrenheit.png)"
scroll_to 'text="mph"' || { shot 4-wind; fail "The wind speed isn't in mph (4-wind.png)"; }
shot 4-wind
pass "the wind speed is in mph (4-wind.png)"

step "Restart: the units are kept"
start_vane -S
wait_for "$HOME_TITLE" 30 || fail "Home never opened after the restart"
wait_for_forecast 5-restarted
after_restart=$(current_temperature)
diff=$((after_restart - fahrenheit)); [ $diff -lt 0 ] && diff=$((-diff))
[ $diff -le 1 ] || { shot 5-restarted; fail "After a restart Home shows ${after_restart}°, not about ${fahrenheit}°F (5-restarted.png)"; }
shot 5-restarted
open_tab Settings || fail "Couldn't open the Settings tab"
for option in "°F" "mph" "in" "12-hour"; do
    scroll_to "text=\"$option\"" && has_state "$option" checked || { shot 6-settings-restarted; fail "'$option' isn't selected after a restart (6-settings-restarted.png)"; }
done
shot 6-settings-restarted
pass "after a restart Home still shows °F and Settings keeps °F, mph, in and 12-hour (5-restarted.png, 6-settings-restarted.png)"

check_no_crash
echo "Done. Artifacts in $OUT"
cat "$OUT/report.md"
