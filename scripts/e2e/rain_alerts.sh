#!/bin/sh
# End-to-end check: rain and snow alerts, against a canned 15-minute forecast.
#
# Usage: scripts/e2e/rain_alerts.sh [emulator-serial]   (after ./gradlew :app:assembleDebug)
#
# Debug builds fetch the alert forecast from the URL in files/e2e-alerts-base-url, if present.
# This script serves canned forecasts from this computer (scripts/e2e/nowcast_server.py, reached
# through adb reverse) and turns the home screen's "Rain and snow alerts" switch on and off; each
# switch-on runs the check at once.
# 1. Declining the notification prompt (Android 13+) leaves alerts off and says how to allow them.
# 2. Dry next 90 minutes: no notification.
# 3. Already raining: no notification.
# 4. Snow starting in about an hour: one notification, "Snow from about <time>".
# 5. The same snow on the next check: no second notification.
# 6. Each request asked for the saved location and skipped the HTTP cache.
# The app's alert settings and notification permission are restored at the end.
# Writes screenshots, logcat, the server's request log and a report to build/e2e/rain-alerts-<timestamp>/.
set -eu

SERIAL="${1:-emulator-5554}"
. "$(dirname "$0")/lib.sh"
OUT="$ROOT/build/e2e/rain-alerts-$(date +%Y%m%d-%H%M%S)"
PORT=8765

TEMPERATURE='text="-?[0-9]+°"'
SWITCH='checkable="true"'
SWITCH_ON='checkable="true" checked="true"'
BLOCKED='text="Notifications are off for Vane[^"]*"'
DENY_BUTTON='resource-id="com.android.permissioncontroller:id/permission_deny_button"'
NOTIFICATION_PERMISSION=android.permission.POST_NOTIFICATIONS

SERVER_PID=""
HAD_NOTIFICATION_PERMISSION=false

alert_log() { adb logcat -d -s RainAlerts:I | grep -c "$1" || true; }
# Waits up to 30 seconds for the RainAlerts log line $1 to appear more than $2 times.
wait_for_log() {
    i=0
    until [ "$(alert_log "$1")" -gt "$2" ]; do
        i=$((i + 1)); [ $i -gt 30 ] && return 1
        sleep 1
    done
}
vane_notifications() { adb shell dumpsys notification --noredact | grep -c "NotificationRecord.*pkg=$PKG" || true; }
# Android sometimes shows "<app> isn't responding" over everything on a busy emulator.
dismiss_system_dialogs() { tap_node 'resource-id="android:id/aerr_wait"' || true; }
# A uiautomator dump can come back empty while the screen animates, so try a few times.
tap_switch() {
    for _ in 1 2 3 4 5; do
        tap_node "$SWITCH" && return 0
        dismiss_system_dialogs
        sleep 1
    done
    shot switch-missing
    fail "Couldn't tap the alerts switch (switch-missing.png)"
}
set_scenario() { echo "$1" > "$OUT/server/scenario"; }
# Switch-on runs a check; switching off first makes sure the next tap is a switch-on.
switch_on_again() {
    if ui | grep -qE "$SWITCH_ON"; then tap_switch; sleep 1; fi
    tap_switch
    wait_for "$SWITCH_ON" 10 || { shot switch; fail "The alerts switch didn't turn on (switch.png)"; }
}

cleanup() {
    [ -n "$SERVER_PID" ] && kill "$SERVER_PID" 2>/dev/null || true
    adb reverse --remove tcp:$PORT >/dev/null 2>&1 || true
    # Switch off through the app so the background check is cancelled, then restore the settings.
    ui | grep -qE "$SWITCH_ON" && tap_node "$SWITCH" && sleep 1
    adb shell am force-stop $PKG
    adb shell run-as $PKG sh -c "'rm -f files/e2e-alerts-base-url shared_prefs/vane_alerts.xml; \
        [ -f files/e2e-vane_alerts.xml ] && mv files/e2e-vane_alerts.xml shared_prefs/vane_alerts.xml; true'" \
        || echo "WARNING: couldn't restore the alert settings"
    if [ "$HAD_NOTIFICATION_PERMISSION" = true ]; then
        adb shell pm grant $PKG $NOTIFICATION_PERMISSION 2>/dev/null || true
    else
        adb shell pm revoke $PKG $NOTIFICATION_PERMISSION 2>/dev/null || true
    fi
    adb shell cmd statusbar collapse >/dev/null 2>&1 || true
    adb shell svc power stayon false >/dev/null 2>&1 || true
}

start_report "Rain and snow alerts E2E"
API=$(adb shell getprop ro.build.version.sdk | tr -d '\r')

step "Install, back up the alert settings, start the canned forecast server"
wake_device
network_on
wait_for_network
adb install -r "$APK" >/dev/null
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb shell cmd location set-location-enabled true
adb shell dumpsys package $PKG | grep -q "$NOTIFICATION_PERMISSION: granted=true" && HAD_NOTIFICATION_PERMISSION=true
# Revoking a permission kills the app, so do it before the app starts. Clearing the flags lets
# Android show the prompt again even if it was declined before.
if [ "$API" -ge 33 ]; then
    adb shell pm revoke $PKG $NOTIFICATION_PERMISSION
    adb shell pm clear-permission-flags $PKG $NOTIFICATION_PERMISSION user-set user-fixed
fi
adb shell am force-stop $PKG
adb shell run-as $PKG sh -c "'mkdir -p files shared_prefs; rm -f files/e2e-vane_alerts.xml; \
    [ -f shared_prefs/vane_alerts.xml ] && mv shared_prefs/vane_alerts.xml files/e2e-vane_alerts.xml; \
    echo http://localhost:$PORT/ > files/e2e-alerts-base-url'"
trap cleanup EXIT
mkdir -p "$OUT/server"
set_scenario dry
python3 "$ROOT/scripts/e2e/nowcast_server.py" $PORT "$OUT/server" &
SERVER_PID=$!
adb reverse tcp:$PORT tcp:$PORT >/dev/null
adb logcat -c
pass "alerts start off; the alert forecast comes from the canned server (adb reverse tcp:$PORT)"

step "Home screen saves the location the alerts use"
start_vane -S
i=0
until ui | grep -qE "$TEMPERATURE"; do
    i=$((i + 1)); [ $i -gt 45 ] && { shot 1-home; fail "Home screen never showed a forecast (1-home.png)"; }
    dismiss_system_dialogs
    # The emulator only reports GPS fixes it is sent while a request is active.
    adb emu geo fix -0.1276 51.5072 >/dev/null
    sleep 1
done
adb shell run-as $PKG cat shared_prefs/vane_widget.xml | grep -q 'name="snapshot"' \
    || fail "The home screen didn't save its location"
wait_for "$SWITCH" || { shot 1-home; fail "No alerts switch on the home screen (1-home.png)"; }
shot 1-home
pass "home screen shows a forecast and the alerts switch, off (1-home.png)"

if [ "$API" -ge 33 ]; then
    step "Declining notifications leaves alerts off"
    tap_switch
    wait_for "$DENY_BUTTON" 10 || { shot 2-prompt; fail "No notification permission prompt (2-prompt.png)"; }
    shot 2-prompt
    tap_node "$DENY_BUTTON"
    wait_for "$BLOCKED" 10 || { shot 3-declined; fail "No explanation after declining notifications (3-declined.png)"; }
    ui | grep -qE "$SWITCH_ON" && { shot 3-declined; fail "Alerts turned on without notifications (3-declined.png)"; }
    shot 3-declined
    pass "declining the prompt keeps the switch off and explains how to allow notifications (2-prompt.png, 3-declined.png)"
else
    echo "- SKIP: API $API has no notification permission prompt" >> "$OUT/report.md"
fi
adb shell pm grant $PKG $NOTIFICATION_PERMISSION 2>/dev/null || true
before_notifications=$(vane_notifications)

step "Dry: no alert"
set_scenario dry
switch_on_again
wait_for_log "No rain or snow" 0 || fail "The check never ran for the dry forecast (see logcat.txt)"
[ "$(vane_notifications)" -eq "$before_notifications" ] || fail "An alert was posted for a dry forecast"
pass "a dry forecast posts nothing"

step "Already raining: no alert"
set_scenario raining-now
switch_on_again
wait_for_log "Already raining" 0 || fail "The check never ran for the raining forecast (see logcat.txt)"
[ "$(vane_notifications)" -eq "$before_notifications" ] || fail "An alert was posted while it was already raining"
pass "rain that has already started posts nothing"

step "Snow in about an hour: one alert"
set_scenario snow-in-60
switch_on_again
wait_for_log "Posted SNOW alert" 0 || fail "No snow alert was posted (see logcat.txt)"
adb shell dumpsys notification --noredact > "$OUT/notifications.txt"
grep -q "android.title=String (Snow from about" "$OUT/notifications.txt" \
    || fail "The notification doesn't say 'Snow from about …' (see notifications.txt)"
adb shell cmd statusbar expand-notifications
sleep 2
shot 4-snow-alert
adb shell cmd statusbar collapse
pass "snow starting in about an hour posts 'Snow from about …' (4-snow-alert.png)"

step "The same snow again: no second alert"
switch_on_again
wait_for_log "Already alerted about this SNOW" 0 || fail "The second check didn't run or alerted again (see logcat.txt)"
[ "$(alert_log "Posted SNOW alert")" -eq 1 ] || fail "The same snow was alerted twice"
pass "the next check of the same snow posts nothing new"

step "Requests: saved location, no HTTP cache"
cp "$OUT/server/requests.log" "$OUT/requests.log"
[ "$(grep -c . "$OUT/requests.log")" -ge 4 ] || fail "Fewer requests than checks (see requests.log)"
grep -v "latitude=.*longitude=.*minutely_15=" "$OUT/requests.log" && fail "A request had no location (see requests.log)"
grep -v "cache-control=no-cache" "$OUT/requests.log" && fail "A request could be served from the cache (see requests.log)"
pass "every check asked for the saved location and skipped the cache (requests.log)"

check_no_crash
echo "Done. Artifacts in $OUT"
cat "$OUT/report.md"
