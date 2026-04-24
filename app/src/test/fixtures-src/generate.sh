#!/usr/bin/env bash
# Generate binary AndroidManifest.xml fixtures for the streaming parser integration tests.
#
# Each source XML in ./manifest/*.xml is linked into a minimal APK via aapt2, and the
# binary AndroidManifest.xml entry is extracted into app/src/test/resources/manifest/.
#
# Prerequisites:
#   - Android SDK (path read from ../../../../local.properties' sdk.dir)
#   - aapt2 (picked from the Gradle cache — any version works for these minimal manifests)
#
# Re-run this script if a source XML changes. The generated blobs are checked into git.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
SRC_DIR="$SCRIPT_DIR/manifest"
OUT_DIR="$PROJECT_ROOT/app/src/test/resources/manifest"

# Resolve SDK path.
SDK_DIR="$(grep '^sdk.dir=' "$PROJECT_ROOT/local.properties" | cut -d'=' -f2-)"
if [[ -z "$SDK_DIR" ]] || [[ ! -d "$SDK_DIR" ]]; then
    echo "ERROR: sdk.dir not set or invalid in local.properties" >&2
    exit 1
fi

# Pick an android.jar — use android-36 to match compileSdk.
ANDROID_JAR="$SDK_DIR/platforms/android-36/android.jar"
if [[ ! -f "$ANDROID_JAR" ]]; then
    # Fall back to the highest available platform.
    ANDROID_JAR="$(ls -d "$SDK_DIR"/platforms/android-* 2>/dev/null | sort -V | tail -1)/android.jar"
fi
if [[ ! -f "$ANDROID_JAR" ]]; then
    echo "ERROR: android.jar not found under $SDK_DIR/platforms" >&2
    exit 1
fi

# Pick aapt2 from the Gradle transforms cache (most recent).
AAPT2="$(find "$HOME/.gradle/caches" -type f -name aapt2 -executable 2>/dev/null \
    | awk -F/ '{ print }' | sort -V | tail -1)"
if [[ -z "$AAPT2" ]] || [[ ! -x "$AAPT2" ]]; then
    echo "ERROR: aapt2 binary not found in Gradle cache. Run any Gradle build first to populate the cache." >&2
    exit 1
fi

mkdir -p "$OUT_DIR"

WORK_DIR="$(mktemp -d)"
trap "rm -rf '$WORK_DIR'" EXIT

echo "Using:"
echo "  aapt2:       $AAPT2"
echo "  android.jar: $ANDROID_JAR"
echo "  out:         $OUT_DIR"
echo

for src in "$SRC_DIR"/*.xml; do
    name="$(basename "$src" .xml)"
    apk="$WORK_DIR/$name.apk"
    echo "==> $name"
    "$AAPT2" link \
        --manifest "$src" \
        -I "$ANDROID_JAR" \
        -o "$apk" \
        --auto-add-overlay
    # Extract AndroidManifest.xml from the APK (unzip -p streams to stdout).
    unzip -p "$apk" AndroidManifest.xml > "$OUT_DIR/$name.manifest.bin"
    printf "    wrote %s (%d bytes)\n" "$OUT_DIR/$name.manifest.bin" "$(stat -c%s "$OUT_DIR/$name.manifest.bin")"
done

echo
echo "All fixtures generated."
