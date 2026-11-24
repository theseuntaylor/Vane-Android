#!/bin/sh
# Prints "<versionCode> <versionName>" for a release tag. Used by .github/workflows/release.yml.
#
# Usage: scripts/ci/resolve_version.sh <tag> <tagged build file> <master build file>
#
# versionName is the tag without its leading "v".
# Manual bump: when the tagged build file already has that versionName, its versionCode is used
# as it is. It must be above master's, unless the tagged commit and master agree (the bump was
# merged to master before tagging).
# Otherwise versionCode is master's plus one.
set -eu

tag="$1"
tagged="$2"
master="$3"

code_in() { sed -n -E 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*([0-9]+)[[:space:]]*$/\1/p' "$1" | head -1; }
name_in() { sed -n -E 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"(.*)"[[:space:]]*$/\1/p' "$1" | head -1; }

name="${tag#v}"
master_code=$(code_in "$master")
tagged_code=$(code_in "$tagged")
[ -n "$master_code" ] || { echo "no versionCode in $master" >&2; exit 1; }
[ -n "$tagged_code" ] || { echo "no versionCode in $tagged" >&2; exit 1; }

if [ "$(name_in "$tagged")" = "$name" ]; then
    if [ "$tagged_code" -lt "$master_code" ] \
        || { [ "$tagged_code" -eq "$master_code" ] && [ "$(name_in "$master")" != "$name" ]; }; then
        echo "versionCode $tagged_code for $name is not above master's $master_code" >&2
        exit 1
    fi
    echo "$tagged_code $name"
else
    echo "$((master_code + 1)) $name"
fi
