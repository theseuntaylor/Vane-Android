#!/bin/sh
# End-to-end check: the location permission flow never leaves a blank screen or traps the user in Settings.
#
# Usage: scripts/e2e/location_permission.sh [emulator-serial]
#
# 1. Denies the permission prompt once: the app shows "Allow location".
# 2. Denies it again (Android then stops asking): the app shows "Open settings" and stays in front.
# 3. Reopens the app: it still shows "Open settings" and does not jump to Settings by itself.
# 4. Taps "Open settings", grants only approximate location, comes back: the home screen shows a forecast.
# 5. Writes screenshots and a report to build/e2e/location-permission-<timestamp>/.
set -eu

SERIAL="${1:-emulator-5554}"
. "$(dirname "$0")/lib.sh"
OUT="$ROOT/build/e2e/location-permission-$(date +%Y%m%d-%H%M%S)"

DENY='resource-id="com.android.permissioncontroller:id/permission_deny(_and_dont_ask_again)?_button"'
ALLOW_LOCATION='text="Allow location"'
OPEN_SETTINGS='text="Open settings"'
HOME_TITLE='text="Your Current Location"'
TEMPERATURE='text="-?[0-9]+°"'

deny_prompt() {
    wait_for "$DENY" 15 || { shot "$1"; fail "The system permission prompt never appeared ($1.png)"; }
    tap_node "$DENY"
}

start_report "Location permission E2E"

step "Fresh install with no location permission"
wake_device
adb install -r "$APK" >/dev/null
adb shell am force-stop $PKG
for permission in ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION; do
    adb shell pm revoke $PKG android.permission.$permission 2>/dev/null || true
    adb shell pm clear-permission-flags $PKG android.permission.$permission user-set user-fixed
done
adb logcat -c
adb shell am start -W -n $PKG/.MainActivity >/dev/null

step "Deny once"
deny_prompt 1-first-prompt
wait_for "$ALLOW_LOCATION" || { shot 1-denied-once; fail "No 'Allow location' button after the first denial (1-denied-once.png)"; }
shot 1-denied-once
pass "after one denial the app explains why and offers 'Allow location' (1-denied-once.png)"

step "Deny again"
tap_node "$ALLOW_LOCATION"
deny_prompt 2-second-prompt
wait_for "$OPEN_SETTINGS" || { shot 2-denied-twice; fail "No 'Open settings' button after the second denial (2-denied-twice.png)"; }
sleep 3
[ "$(focused_package)" = "$PKG" ] || fail "Something else took focus after the second denial: $(focused_package)"
shot 2-denied-twice
pass "after two denials the app offers 'Open settings' and stays in front (2-denied-twice.png)"

step "Reopen the app"
adb shell am start -S -W -n $PKG/.MainActivity >/dev/null
wait_for "$OPEN_SETTINGS" || { shot 3-reopened; fail "No 'Open settings' button after reopening (3-reopened.png)"; }
sleep 5
[ "$(focused_package)" = "$PKG" ] || fail "The app opened $(focused_package) by itself after reopening"
shot 3-reopened
pass "reopening shows 'Open settings' and does not open Settings by itself (3-reopened.png)"

step "Open settings, grant approximate location only, come back"
tap_node "$OPEN_SETTINGS"
sleep 3
[ "$(focused_package)" != "$PKG" ] || fail "Tapping 'Open settings' did not leave the app"
shot 4-settings
pass "tapping 'Open settings' opens $(focused_package) (4-settings.png)"
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb shell input keyevent BACK
wait_for "$HOME_TITLE" || { shot 5-approximate; fail "Home screen didn't open with approximate location (5-approximate.png)"; }
# The emulator only reports GPS fixes it is sent while a request is active.
i=0
until ui | grep -qE "$TEMPERATURE"; do
    i=$((i + 1)); [ $i -gt 45 ] && { shot 5-approximate; fail "No forecast with approximate location (5-approximate.png)"; }
    adb emu geo fix -0.1276 51.5072 >/dev/null
    sleep 1
done
[ "$(adb shell dumpsys package $PKG | grep -c 'ACCESS_FINE_LOCATION: granted=true')" = 0 ] \
    || fail "Precise location was granted; this step must run with approximate only"
shot 5-approximate
pass "with approximate location only, the home screen shows a forecast on return (5-approximate.png)"
check_no_crash

adb shell svc power stayon false >/dev/null 2>&1 || true
echo "Done. Artifacts in $OUT"
cat "$OUT/report.md"
