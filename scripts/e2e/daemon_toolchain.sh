#!/bin/sh
# End-to-end check: the build picks the daemon JVM from gradle/gradle-daemon-jvm.properties,
# whatever JDK the shell would otherwise use.
#
# Usage: scripts/e2e/daemon_toolchain.sh
#
# Stops running daemons, unsets JAVA_HOME (so the launcher runs on the machine's default `java`),
# builds the debug APK, and checks the daemon ran on the JDK version the criteria file asks for.
# Writes a log to build/e2e/daemon-toolchain-<timestamp>.log.
set -eu

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/build/e2e"
LOG="$OUT/daemon-toolchain-$(date +%Y%m%d-%H%M%S).log"
mkdir -p "$OUT"
cd "$ROOT"

fail() { echo "FAIL: $*" | tee -a "$LOG"; exit 1; }

expected=$(sed -n 's/^toolchainVersion=//p' gradle/gradle-daemon-jvm.properties)
[ -n "$expected" ] || fail "gradle/gradle-daemon-jvm.properties has no toolchainVersion"

unset JAVA_HOME
{
    echo "# Daemon toolchain E2E — $(date)"
    echo "Expected daemon JVM: $expected"
    echo "Shell default java: $(java -version 2>&1 | head -1)"
} > "$LOG"

sh ./gradlew --stop >> "$LOG" 2>&1

# --version prints the daemon JVM Gradle selected from the criteria.
sh ./gradlew --version >> "$LOG" 2>&1 || fail "gradlew --version failed (see log)"
grep -E "^Daemon JVM:" "$LOG" | grep -q "$expected" || fail "Daemon JVM is not $expected (see log)"
echo "- PASS: daemon JVM criteria select JDK $expected" >> "$LOG"

sh ./gradlew :app:assembleDebug -q >> "$LOG" 2>&1 || fail "assembleDebug failed (see log)"
echo "- PASS: :app:assembleDebug succeeded with JAVA_HOME unset" >> "$LOG"

echo "Done. Log: $LOG"
grep -E "^(# |Expected|Shell default|Daemon JVM|- PASS)" "$LOG"
