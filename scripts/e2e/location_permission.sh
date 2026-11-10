#!/bin/sh
# End-to-end check: the location permission flow never leaves a blank screen or traps the user in Settings.
#
# Usage: scripts/e2e/location_permission.sh [emulator-serial]
#
# 1. Denies the permission prompt once: the app shows "Allow location".
# 2. Denies it again (Android then stops asking): the app shows "Open settings" and stays in front.
# 3. Reopens the app: it still shows "Open settings" and does not jump to Settings by itself.
# 4. Taps "Open settings", grants only approximate location, presses HOME and opens Vane from the
#    launcher: Vane's home screen shows a forecast, not the settings page it opened.
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
start_vane

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
start_vane -S
wait_for "$OPEN_SETTINGS" || { shot 3-reopened; fail "No 'Open settings' button after reopening (3-reopened.png)"; }
sleep 5
[ "$(focused_package)" = "$PKG" ] || fail "The app opened $(focused_package) by itself after reopening"
shot 3-reopened
pass "reopening shows 'Open settings' and does not open Settings by itself (3-reopened.png)"

step "Open settings, grant approximate location only, leave with HOME, open Vane from the launcher"
tap_node "$OPEN_SETTINGS"
sleep 3
[ "$(focused_package)" != "$PKG" ] || fail "Tapping 'Open settings' did not leave the app"
shot 4-settings
pass "tapping 'Open settings' opens $(focused_package) (4-settings.png)"
# Settings must open in its own task. On top of Vane's task, returning to Vane from Recents shows Settings.
case "$(vane_task_top)" in
    "$PKG"/*) pass "Settings opened in its own task; Vane's task still ends at $(vane_task_top)" ;;
    *) fail "Settings opened inside Vane's task (top activity $(vane_task_top))" ;;
esac
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
# Leave Settings the way many users do: HOME, then the Vane icon. If Settings had opened inside
# Vane's task, the icon would bring back the settings page instead of Vane.
adb shell input keyevent HOME
sleep 1
adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 3
[ "$(focused_package)" = "$PKG" ] || { shot 5-relaunched; fail "Opening Vane from the launcher showed $(focused_package) (5-relaunched.png)"; }
wait_for "$HOME_TITLE" || { shot 5-relaunched; fail "Home screen didn't open with approximate location (5-relaunched.png)"; }
pass "opening Vane from the launcher after Settings shows Vane's home screen, not Settings"
# The emulator only reports GPS fixes it is sent while a request is active.
i=0
until ui | grep -qE "$TEMPERATURE"; do
    i=$((i + 1)); [ $i -gt 45 ] && { shot 5-relaunched; fail "No forecast with approximate location (5-relaunched.png)"; }
    adb emu geo fix -0.1276 51.5072 >/dev/null
    sleep 1
done
[ "$(adb shell dumpsys package $PKG | grep -c 'ACCESS_FINE_LOCATION: granted=true')" = 0 ] \
    || fail "Precise location was granted; this step must run with approximate only"
shot 5-relaunched
pass "with approximate location only, the home screen shows a forecast (5-relaunched.png)"
check_no_crash

adb shell svc power stayon false >/dev/null 2>&1 || true
echo "Done. Artifacts in $OUT"
cat "$OUT/report.md"
