#!/bin/sh
# End-to-end check: the home screen widget shows weather with the app closed and the device offline.
#
# Usage: scripts/e2e/widget_offline.sh [emulator-serial]
#
# 1. Installs the debug APK, grants location and opens the app once so it caches a forecast.
# 2. Makes sure a Vane widget is on the home screen (adds one through the Pixel launcher if needed).
# 3. Closes the app the way a user does (process killed, not force-stopped), turns on airplane
#    mode and cold-boots the emulator, so the widget must render from a cold process with no network.
#    Force-stop is avoided on purpose: Android blocks all widget updates for a force-stopped app
#    until the user opens it again, for every widget provider.
# 4. Writes screenshots and a report to build/e2e/widget-offline-<timestamp>/.
#
# Use an emulator: the script toggles airplane mode and restarts the emulator.
set -eu

SERIAL="${1:-emulator-5554}"
PKG=com.theseuntaylor.vane
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
OUT="$ROOT/build/e2e/widget-offline-$(date +%Y%m%d-%H%M%S)"
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"

adb() { "$ADB" -s "$SERIAL" "$@"; }
step() { echo "==> $*"; }
fail() { echo "FAIL: $*" | tee -a "$OUT/report.md"; echo "Artifacts in $OUT"; exit 1; }
shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
# Cold-boots the emulator. `adb reboot` can wedge some emulator images, so restart the AVD instead.
restart_emulator() {
    avd=$(adb emu avd name | head -1 | tr -d '\r')
    adb emu kill >/dev/null
    while "$ADB" devices | grep -q "^$SERIAL"; do sleep 1; done
    "${ANDROID_HOME:-$HOME/Library/Android/sdk}/emulator/emulator" -avd "$avd" -no-snapshot-load \
        > "$OUT/emulator.log" 2>&1 &
    wait_for_boot
}
wait_for_boot() {
    adb wait-for-device
    until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 3; done
    adb shell input keyevent KEYCODE_WAKEUP
    adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
}
# Bound widget instances list "provider=ProviderId{...}"; the provider catalogue uses "provider ProviderId".
widget_bound() { adb shell dumpsys appwidget | grep -q "provider=ProviderId{.*cmp:ComponentInfo{$PKG/$PKG.feature.widget.ui.VaneWeatherWidgetReceiver}"; }
# Taps a point given as a fraction of the screen, so it works across screen sizes.
tap_frac() {
    size=$(adb shell wm size | sed -n 's/.*: \([0-9]*x[0-9]*\).*/\1/p' | tail -1)
    w=${size%x*}; h=${size#*x}
    adb shell input tap $((w * $1 / 1000)) $((h * $2 / 1000))
}

[ -f "$APK" ] || fail "Build the APK first: ./gradlew :app:assembleDebug"
mkdir -p "$OUT"
echo "# Widget offline E2E — $(date)" > "$OUT/report.md"
echo "Device: $SERIAL ($(adb shell getprop ro.product.model | tr -d '\r'), API $(adb shell getprop ro.build.version.sdk | tr -d '\r'))" >> "$OUT/report.md"

step "Online: install, grant location, open the app"
adb shell cmd connectivity airplane-mode disable
adb shell svc wifi enable >/dev/null 2>&1 || true
adb shell svc data enable >/dev/null 2>&1 || true
i=0
until adb shell ping -c1 -W2 1.1.1.1 >/dev/null 2>&1; do
    i=$((i + 1)); [ $i -gt 30 ] && fail "Network never came up"
    sleep 1
done
# The app reads the last known location once, so give the emulator a fix before it opens.
adb emu geo fix -0.1276 51.5072 >/dev/null
adb install -r "$APK" >/dev/null
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb shell run-as $PKG rm -f shared_prefs/vane_widget.xml
adb shell am start -W -n $PKG/.MainActivity >/dev/null

i=0
until adb shell run-as $PKG cat shared_prefs/vane_widget.xml 2>/dev/null | grep -q snapshot; do
    i=$((i + 1)); [ $i -gt 45 ] && fail "App never cached a forecast for the widget"
    # Reopen every 15 s in case the first launch found no location fix.
    [ $((i % 15)) -eq 0 ] && adb shell am start -S -W -n $PKG/.MainActivity >/dev/null
    # The emulator only reports GPS fixes it is sent while a request is active.
    adb emu geo fix -0.1276 51.5072 >/dev/null
    sleep 1
done
adb shell run-as $PKG cat shared_prefs/vane_widget.xml > "$OUT/snapshot.xml"
echo "- PASS: app cached a widget snapshot after loading the home screen" >> "$OUT/report.md"

step "Ensure a Vane widget is on the home screen"
adb shell input keyevent HOME
sleep 1
if ! widget_bound; then
    # Pixel launcher: long-press empty space > Widgets > search "Vane" > expand > preview > Add.
    size=$(adb shell wm size | sed -n 's/.*: \([0-9]*x[0-9]*\).*/\1/p' | tail -1)
    w=${size%x*}; h=${size#*x}
    adb shell input swipe $((w / 2)) $((h * 433 / 1000)) $((w / 2)) $((h * 433 / 1000)) 1200; sleep 1.5
    tap_frac 632 250; sleep 2      # Widgets
    tap_frac 500 206; sleep 1      # Search field
    adb shell input text Vane; sleep 2
    adb shell input keyevent 111; sleep 1
    tap_frac 500 300; sleep 2      # Expand "Vane"
    tap_frac 500 441; sleep 1.5    # Preview
    tap_frac 500 568; sleep 3      # Add
    adb shell input keyevent HOME; sleep 1
fi
widget_bound || fail "No Vane widget on the home screen. Add one by hand and re-run."
shot 1-online
echo "- PASS: Vane widget is bound to the launcher (1-online.png)" >> "$OUT/report.md"

step "Offline + app closed: kill the app, airplane mode, cold-boot"
adb shell input keyevent HOME; sleep 1
adb shell am kill $PKG
adb shell cmd connectivity airplane-mode enable
# Android can keep Wi-Fi on in airplane mode, so switch the radios off explicitly too.
adb shell svc wifi disable >/dev/null 2>&1 || true
adb shell svc data disable >/dev/null 2>&1 || true
# Settings are written to disk asynchronously; flush before the abrupt emulator kill.
sleep 5
adb shell sync
restart_emulator
sleep 20
adb shell input keyevent HOME; sleep 2

[ "$(adb shell settings get global airplane_mode_on | tr -d '\r')" = 1 ] || fail "Airplane mode is off after reboot"
adb shell ping -c1 -W2 1.1.1.1 >/dev/null 2>&1 && fail "Device still has network"
echo "- PASS: device rebooted in airplane mode with no network" >> "$OUT/report.md"

adb shell dumpsys activity activities | grep -q "$PKG/.MainActivity" && fail "MainActivity is running; the app should be closed"
echo "- PASS: no Vane activity is running" >> "$OUT/report.md"
adb shell dumpsys package $PKG | grep -q "stopped=true" && fail "Vane is in the force-stopped state"


widget_bound || fail "Widget is no longer bound after reboot"
adb logcat -d > "$OUT/logcat.txt"
grep -E "FATAL EXCEPTION" -A3 "$OUT/logcat.txt" | grep -q "$PKG" && fail "Vane crashed after reboot (see logcat.txt)"
echo "- PASS: no Vane crash after reboot" >> "$OUT/report.md"
shot 2-offline
echo "- CHECK: 2-offline.png must show the same location and temperatures as 1-online.png" >> "$OUT/report.md"

adb shell cmd connectivity airplane-mode disable
adb shell svc wifi enable >/dev/null 2>&1 || true
adb shell svc data enable >/dev/null 2>&1 || true
echo "Done. Artifacts in $OUT"
cat "$OUT/report.md"
