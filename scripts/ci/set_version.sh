#!/bin/sh
# Writes versionCode and versionName into a Gradle build file.
# Used by .github/workflows/release.yml.
#
# Usage: scripts/ci/set_version.sh <versionCode> <versionName> [build-file]
set -eu

code="$1"
name="$2"
file="${3:-app/build.gradle}"

case "$code" in '' | *[!0-9]*) echo "versionCode must be a whole number: '$code'" >&2; exit 1 ;; esac
case "$name" in '' | *[!0-9A-Za-z.-]*) echo "versionName has unexpected characters: '$name'" >&2; exit 1 ;; esac

sed -i.bak -E \
    -e "s/^([[:space:]]*versionCode[[:space:]]*=[[:space:]]*)[0-9]+[[:space:]]*$/\1$code/" \
    -e "s/^([[:space:]]*versionName[[:space:]]*=[[:space:]]*)\".*\"[[:space:]]*$/\1\"$name\"/" \
    "$file"
rm -f "$file.bak"

grep -Eq "^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*$code$" "$file" \
    || { echo "could not set versionCode in $file" >&2; exit 1; }
grep -Eq "^[[:space:]]*versionName[[:space:]]*=[[:space:]]*\"$name\"$" "$file" \
    || { echo "could not set versionName in $file" >&2; exit 1; }
