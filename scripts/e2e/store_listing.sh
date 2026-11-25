#!/bin/sh
# End-to-end check of the Play store listing in fastlane/metadata/android, before it is uploaded by
# .github/workflows/store-listing.yml.
#
# Usage: scripts/e2e/store_listing.sh
#
# Ways the listing can go wrong, each checked below:
#   1. A text file is missing or empty, so the upload clears that field on Play.
#   2. A text is longer than Play allows (title 30, short description 80, full description 4000
#      characters), so Play rejects the whole upload.
#   3. A text keeps a leftover from drafting (TODO, TBD, a placeholder in angle brackets).
#   4. There are fewer than 2 or more than 8 phone screenshots; Play rejects both.
#   5. A screenshot is not a PNG, is smaller than 320 px or larger than 3840 px on a side, or its
#      long side is more than twice its short side; Play rejects it.
#   6. A screenshot is landscape, or the screenshots are not all the same size, so the listing
#      looks uneven.
#   7. There is more than one locale folder, so an upload would add or change a translation by
#      accident.
#   8. A screenshot was taken on the wrong screen or mid-load. The capture script
#      (scripts/store/capture_screenshots.sh) checks the screen before each shot and records what
#      it saw in capture-report.md next to the screenshots; this check fails if that report is
#      missing, has a failure, or names a different set of files from the folder.
#
# Writes a report to build/e2e/store-listing-<timestamp>.md.
set -eu

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
META="$ROOT/fastlane/metadata/android"
OUT="$ROOT/build/e2e"
REPORT="$OUT/store-listing-$(date +%Y%m%d-%H%M%S).md"
mkdir -p "$OUT"

failed=0
pass() { echo "- PASS: $*" >> "$REPORT"; }
fail() { echo "- FAIL: $*" | tee -a "$REPORT"; failed=1; }

echo "# Store listing check — $(date)" > "$REPORT"

# 7. Exactly one locale.
locales=$(find "$META" -mindepth 1 -maxdepth 1 -type d | wc -l | tr -d ' ')
if [ "$locales" != 1 ]; then
    fail "expected one locale folder in $META, found $locales"
    exit 1
fi
LOCALE_DIR=$(find "$META" -mindepth 1 -maxdepth 1 -type d)
echo "Locale: $(basename "$LOCALE_DIR")" >> "$REPORT"
pass "one locale folder ($(basename "$LOCALE_DIR"))"

# 1-3. Text fields. Play counts characters, not bytes; fastlane strips the trailing newline.
check_text() {
    file="$LOCALE_DIR/$1"
    if [ ! -s "$file" ]; then fail "$1 is missing or empty"; return; fi
    chars=$(python3 -I -c 'import sys; print(len(open(sys.argv[1], encoding="utf-8").read().rstrip("\n")))' "$file")
    if [ "$chars" -gt "$2" ]; then fail "$1 has $chars characters; Play allows $2"; return; fi
    if grep -qiE 'TODO|TBD|lorem|<[a-z ]+>' "$file"; then fail "$1 still has a placeholder"; return; fi
    pass "$1: $chars of $2 characters"
}
check_text title.txt 30
check_text short_description.txt 80
check_text full_description.txt 4000

# 4-6. Phone screenshots.
SHOTS="$LOCALE_DIR/images/phoneScreenshots"
count=$(find "$SHOTS" -maxdepth 1 -type f 2>/dev/null | wc -l | tr -d ' ')
if [ "$count" -lt 2 ] || [ "$count" -gt 8 ]; then
    fail "$count phone screenshots; Play needs 2 to 8"
else
    pass "$count phone screenshots"
fi

sizes=$(python3 -I - "$SHOTS" <<'EOF'
import os, struct, sys
folder = sys.argv[1]
for name in sorted(os.listdir(folder)):
    with open(os.path.join(folder, name), "rb") as f:
        head = f.read(24)
    if head[:8] != b"\x89PNG\r\n\x1a\n":
        print(f"{name} notpng 0 0")
        continue
    w, h = struct.unpack(">II", head[16:24])
    print(f"{name} png {w} {h}")
EOF
)
first=""
echo "$sizes" | while read -r name kind w h; do
    [ -n "$name" ] || continue
    if [ "$kind" != png ]; then echo "- FAIL: $name is not a PNG"; continue; fi
    short=$w; long=$h; [ "$w" -gt "$h" ] && { short=$h; long=$w; }
    [ "$short" -ge 320 ] && [ "$long" -le 3840 ] || echo "- FAIL: $name is ${w}x${h}; each side must be 320 to 3840 px"
    [ "$long" -le $((short * 2)) ] || echo "- FAIL: $name is ${w}x${h}; the long side may be at most twice the short side"
    [ "$w" -lt "$h" ] || echo "- FAIL: $name is landscape"
    [ -z "$first" ] && first="${w}x${h}"
    [ "${w}x${h}" = "$first" ] || echo "- FAIL: $name is ${w}x${h}, not $first like the others"
done > "$OUT/.store-listing-images"
if [ -s "$OUT/.store-listing-images" ]; then
    cat "$OUT/.store-listing-images" | tee -a "$REPORT"
    failed=1
else
    pass "every screenshot is a portrait PNG of the same size, within Play's limits ($(echo "$sizes" | head -1 | awk '{print $3"x"$4}'))"
fi
rm -f "$OUT/.store-listing-images"

# 8. The capture report vouches for every screenshot in the folder.
CAPTURE="$SHOTS/../capture-report.md"
if [ ! -f "$CAPTURE" ]; then
    fail "no capture report at images/capture-report.md; take the screenshots with scripts/store/capture_screenshots.sh"
elif grep -q -- '- FAIL' "$CAPTURE"; then
    fail "the capture report has a failure: $(grep -- '- FAIL' "$CAPTURE" | head -1)"
else
    reported=$(sed -n 's/^- PASS: \([^ ]*\.png\) .*/\1/p' "$CAPTURE" | sort)
    present=$(find "$SHOTS" -maxdepth 1 -type f -exec basename {} \; | sort)
    if [ "$reported" = "$present" ]; then
        pass "the capture report checked the screen for every screenshot"
    else
        fail "the capture report and the screenshots folder list different files"
    fi
fi

if [ "$failed" = 0 ]; then echo "PASS — report: $REPORT"; else echo "FAIL — report: $REPORT"; fi
grep -E '^(Locale|- )' "$REPORT"
exit "$failed"
