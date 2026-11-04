# Shared helpers for the E2E scripts. Source it after setting SERIAL and OUT.
# shellcheck shell=sh

PKG=com.theseuntaylor.vane
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# Set APK to test another build, e.g. one from master.
APK="${APK:-$ROOT/app/build/outputs/apk/debug/app-debug.apk}"
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"

adb() { "$ADB" -s "$SERIAL" "$@"; }
step() { echo "==> $*"; }
pass() { echo "- PASS: $*" >> "$OUT/report.md"; }
fail() { echo "- FAIL: $*" | tee -a "$OUT/report.md"; echo "Artifacts in $OUT"; exit 1; }
shot() { adb exec-out screencap -p > "$OUT/$1.png"; }

# The screen's accessibility tree, one node per line. Compose text shows up as text="…".
ui() {
    adb shell uiautomator dump /sdcard/vane-ui.xml >/dev/null 2>&1
    adb shell cat /sdcard/vane-ui.xml | tr '>' '\n'
}
# Waits up to $2 seconds (default 20) for a node matching the regex $1.
wait_for() {
    i=0
    until ui | grep -qE "$1"; do
        i=$((i + 1)); [ $i -gt "${2:-20}" ] && return 1
        sleep 1
    done
}
# Taps the centre of the first node matching the regex $1.
tap_node() {
    bounds=$(ui | grep -E "$1" | head -1 \
        | sed -n 's/.*bounds="\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]".*/\1 \2 \3 \4/p')
    [ -n "$bounds" ] || return 1
    # shellcheck disable=SC2086
    set -- $bounds
    adb shell input tap $((($1 + $3) / 2)) $((($2 + $4) / 2))
}
# The package of the window that has focus, e.g. com.theseuntaylor.vane.
focused_package() {
    adb shell dumpsys window | grep -m1 mCurrentFocus | sed -n 's/.* \([a-zA-Z0-9_.]*\)\/.*/\1/p'
}
# Starts Vane in a fresh, cleared task (FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK), so no
# screen left over from an earlier run is on top. Pass -S to also restart the process.
start_vane() { adb shell am start -W -f 0x10008000 "$@" -n $PKG/.MainActivity >/dev/null; }
# The top activity of the task that has Vane's MainActivity at its base.
vane_task_top() {
    adb shell am stack list | grep "taskId=.*$PKG/$PKG.MainActivity" | sed -n 's/.*topActivity=ComponentInfo{\([^}]*\)}.*/\1/p'
}
wake_device() {
    adb shell input keyevent KEYCODE_WAKEUP
    adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
    adb shell svc power stayon true >/dev/null 2>&1 || true
}
network_on() {
    adb shell cmd connectivity airplane-mode disable
    adb shell svc wifi enable >/dev/null 2>&1 || true
    adb shell svc data enable >/dev/null 2>&1 || true
}
network_off() {
    adb shell cmd connectivity airplane-mode enable
    # Android can keep Wi-Fi on in airplane mode, so switch the radios off explicitly too.
    adb shell svc wifi disable >/dev/null 2>&1 || true
    adb shell svc data disable >/dev/null 2>&1 || true
}
wait_for_network() {
    i=0
    until adb shell ping -c1 -W2 1.1.1.1 >/dev/null 2>&1; do
        i=$((i + 1)); [ $i -gt 30 ] && fail "Network never came up"
        sleep 1
    done
}
start_report() {
    [ -f "$APK" ] || { echo "Build the APK first: ./gradlew :app:assembleDebug"; exit 1; }
    mkdir -p "$OUT"
    echo "# $1 — $(date)" > "$OUT/report.md"
    echo "Device: $SERIAL ($(adb shell getprop ro.product.model | tr -d '\r'), API $(adb shell getprop ro.build.version.sdk | tr -d '\r'))" >> "$OUT/report.md"
}
check_no_crash() {
    adb logcat -d > "$OUT/logcat.txt"
    grep -E "FATAL EXCEPTION" -A3 "$OUT/logcat.txt" | grep -q "$PKG" && fail "Vane crashed (see logcat.txt)"
    pass "no Vane crash in logcat"
}
