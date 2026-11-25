#!/bin/sh
# End-to-end check: Home's background follows the weather and time of day where the user is, and
# other tabs keep the plain background.
#
# Usage: scripts/e2e/weather_backdrop.sh [emulator-serial]   (after ./gradlew :app:assembleDebug)
# Needs python3 and network access to api.open-meteo.com from this machine.
#
# 1. Opens Home at London and reads the backdrop the app chose from its test tag
#    ("backdrop-<sky>-<time of day>"). Asks Open-Meteo directly about the coordinates the app
#    requested (the debug build logs each request), works out the expected backdrop with the same
#    rules, and compares.
# 2. Opens Settings: it must be plain ("backdrop-none"), and its top colour must differ from Home's.
# 3. Backs up the database, adds Tokyo as a favourite and opens its details (usually daylight when
#    London is dark, and the reverse), and checks its backdrop the same way. Restores the database.
# 4. Writes screenshots, the Open-Meteo answers and a report to build/e2e/weather-backdrop-<timestamp>/.
#
# Ways this could fail, which the steps above check: the backdrop ignores the weather or the day
# and night (tag differs from Open-Meteo's); the tag is right but nothing is drawn (Home's colour
# equals Settings'); a screen keeps the last place's backdrop after leaving it (Settings isn't
# plain); a crash (logcat). Not checked here: dawn and dusk need a run within 45 minutes of
# sunrise or sunset at one of the places; the report says when that happened.
set -eu

SERIAL="${1:-emulator-5554}"
. "$(dirname "$0")/lib.sh"
OUT="$ROOT/build/e2e/weather-backdrop-$(date +%Y%m%d-%H%M%S)"

TEMPERATURE='text="-?[0-9]+°"'
BACKDROP='resource-id="backdrop-[a-z-]+"'
# A point at the top right of the screen, beside the title, where nothing is drawn but the background.
PROBE_X=0.95
PROBE_Y=0.07

backdrop_tag() { ui | grep -oE "$BACKDROP" | head -1 | sed 's/resource-id="\(.*\)"/\1/'; }
pixel() { python3 "$ROOT/scripts/e2e/png_pixel.py" "$OUT/$1.png" $PROBE_X $PROBE_Y; }
# The latitude and longitude of the app's last single-place forecast request, from the debug log.
last_request_coordinates() {
    adb logcat -d | grep -oE -- '--> GET https://api.open-meteo.com/v1/forecast\?latitude=[-0-9.]+&longitude=[-0-9.]+&' \
        | tail -1 | sed 's/.*latitude=\([-0-9.]*\)&longitude=\([-0-9.]*\)&.*/\1 \2/'
}
# The backdrop Open-Meteo's current data calls for, by the app's rules (core/backdrop/WeatherBackdrop.kt).
expected_tag() {
    curl -sf "https://api.open-meteo.com/v1/forecast?latitude=$1&longitude=$2&current=weather_code,is_day&daily=sunrise,sunset&timezone=auto&forecast_days=1" \
        > "$OUT/open-meteo-$3.json" || return 1
    python3 - "$OUT/open-meteo-$3.json" <<'EOF'
import json, sys
from datetime import datetime
d = json.load(open(sys.argv[1]))
code, is_day, now = d["current"]["weather_code"], d["current"]["is_day"] == 1, d["current"]["time"]
if code in (0, 1): sky = "clear"
elif code == 2: sky = "partly-cloudy"
elif code in (45, 48): sky = "fog"
elif 51 <= code <= 67 or 80 <= code <= 82: sky = "rain"
elif 71 <= code <= 77 or code in (85, 86): sky = "snow"
elif 95 <= code <= 99: sky = "storm"
else: sky = "cloudy"
t = datetime.fromisoformat(now)
near = lambda e: abs((datetime.fromisoformat(e) - t).total_seconds()) <= 45 * 60
if near(d["daily"]["sunrise"][0]): light = "dawn"
elif near(d["daily"]["sunset"][0]): light = "dusk"
else: light = "day" if is_day else "night"
print(f"backdrop-{sky}-{light}")
EOF
}
# Opens Home at longitude $1, latitude $2 and waits for its forecast.
open_home_at() {
    adb emu geo fix "$1" "$2" >/dev/null
    start_vane -S
    # The emulator only reports GPS fixes it is sent while a request is active.
    for _ in 1 2 3; do sleep 1; adb emu geo fix "$1" "$2" >/dev/null; done
    wait_for "$TEMPERATURE" 45
}
# Reads the backdrop on screen and checks it against Open-Meteo for the place the app last asked about.
check_backdrop() {
    name=$1 shot_name=$2
    sleep 2   # the backdrop fades in over 0.6 s
    shot "$shot_name"
    tag=$(backdrop_tag)
    coordinates=$(last_request_coordinates)
    [ -n "$coordinates" ] || fail "No forecast request for $name in the log"
    # shellcheck disable=SC2086
    expected=$(expected_tag $coordinates "$name") || fail "Couldn't ask Open-Meteo about $name"
    [ "$tag" = "$expected" ] || fail "At $name ($coordinates) the app shows $tag, but Open-Meteo's current data means $expected ($shot_name.png, open-meteo-$name.json)"
    pass "at $name ($coordinates) the screen shows $tag, matching Open-Meteo's current weather and time of day ($shot_name.png)"
    case "$tag" in *-dawn|*-dusk) echo "- NOTE: $name was at dawn or dusk during this run" >> "$OUT/report.md" ;; esac
    PIXEL=$(pixel "$shot_name")
}
restore_database() {
    adb shell am force-stop $PKG
    adb shell run-as $PKG sh -c "'rm -rf databases && cp -r files/e2e-databases-backup databases && rm -rf files/e2e-databases-backup'" \
        || echo "WARNING: couldn't restore the database backup in files/e2e-databases-backup"
}

start_report "Weather backdrop E2E"

step "Install and grant location"
wake_device
network_on
wait_for_network
adb install -r "$APK" >/dev/null
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb logcat -c

step "Home at London"
open_home_at -0.1276 51.5072 || { shot 1-london; fail "Home never showed a forecast (1-london.png)"; }
# Not in $(...): fail must stop the whole script, not a subshell.
check_backdrop London 1-london
LONDON_PIXEL=$PIXEL LONDON_TAG=$tag

step "Settings tab is plain"
open_tab Settings || { shot 2-settings; fail "Couldn't open the Settings tab (2-settings.png)"; }
sleep 2
shot 2-settings
SETTINGS_TAG=$(backdrop_tag)
[ "$SETTINGS_TAG" = backdrop-none ] || fail "The Settings tab kept a backdrop ($SETTINGS_TAG) (2-settings.png)"
SETTINGS_PIXEL=$(pixel 2-settings)
[ "$LONDON_PIXEL" != "$SETTINGS_PIXEL" ] || fail "Home at London ($LONDON_TAG) is the same colour as Settings ($SETTINGS_PIXEL): the backdrop isn't drawn (1-london.png, 2-settings.png)"
pass "Settings is plain (rgb $SETTINGS_PIXEL); Home at London was tinted (rgb $LONDON_PIXEL) (2-settings.png)"

step "Details for Tokyo, added as a favourite"
adb shell am force-stop $PKG
adb shell run-as $PKG sh -c "'rm -rf files/e2e-databases-backup && mkdir -p files databases && cp -r databases files/e2e-databases-backup'"
trap restore_database EXIT
adb shell run-as $PKG sh -c "'rm -rf databases'"
start_vane -S
wait_for "$TEMPERATURE" 45 || { shot 3-tokyo; fail "Home never showed a forecast (3-tokyo.png)"; }
open_tab Favourites || fail "Couldn't open the Favourites tab"
tap_node 'content-desc="Add place"' || fail "Couldn't tap 'Add place'"
sleep 2
adb shell input text Tokyo
wait_for 'text="Tokyo[^"]*, JP"' 30 || { shot 3-search; fail "No search result for Tokyo (3-search.png)"; }
tap_node 'text="Tokyo[^"]*, JP"'
sleep 2
adb shell input keyevent BACK
sleep 1
ui | grep -qE 'text="Favourite Locations"' || adb shell input keyevent BACK
wait_for 'text="Tokyo"' 30 || { shot 3-tokyo; fail "Tokyo isn't on the Favourites tab (3-tokyo.png)"; }
tap_node 'text="Tokyo"'
wait_for 'text="Today.s Hourly Forecast"' 30 || { shot 3-tokyo; fail "Tokyo's details didn't open (3-tokyo.png)"; }
check_backdrop Tokyo 3-tokyo
TOKYO_PIXEL=$PIXEL TOKYO_TAG=$tag
[ "$TOKYO_PIXEL" != "$SETTINGS_PIXEL" ] || fail "Home at Tokyo ($TOKYO_TAG) is the same colour as Settings: the backdrop isn't drawn (3-tokyo.png)"
if [ "$TOKYO_TAG" != "$LONDON_TAG" ]; then
    [ "$TOKYO_PIXEL" != "$LONDON_PIXEL" ] || fail "$LONDON_TAG and $TOKYO_TAG look the same (1-london.png, 3-tokyo.png)"
    pass "Tokyo ($TOKYO_TAG, rgb $TOKYO_PIXEL) looks different from London ($LONDON_TAG, rgb $LONDON_PIXEL)"
else
    echo "- SKIP: London and Tokyo had the same backdrop ($TOKYO_TAG), so two different skies weren't compared" >> "$OUT/report.md"
fi
check_no_crash

adb shell svc power stayon false >/dev/null 2>&1 || true
echo "Done. Artifacts in $OUT"
cat "$OUT/report.md"
