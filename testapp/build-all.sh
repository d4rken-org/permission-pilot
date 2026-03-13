#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$SCRIPT_DIR"

OUTPUT_DIR="$SCRIPT_DIR/output"
MANIFEST_DIR="$SCRIPT_DIR/manifests"
MAIN_MANIFEST="$SCRIPT_DIR/src/main/AndroidManifest.xml"

rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

# Discover manifest versions dynamically: manifests/v{N}.xml, sorted numerically
VERSIONS=()
for f in "$MANIFEST_DIR"/v*.xml; do
    basename="$(basename "$f" .xml)"
    num="${basename#v}"
    VERSIONS+=("$num")
done
IFS=$'\n' VERSIONS=($(sort -n <<<"${VERSIONS[*]}")); unset IFS

if [ ${#VERSIONS[@]} -eq 0 ]; then
    echo "No manifests found in $MANIFEST_DIR"
    exit 1
fi

echo "Found ${#VERSIONS[@]} versions: ${VERSIONS[*]}"

for VERSION in "${VERSIONS[@]}"; do
    echo "=== Building v${VERSION} (versionCode=${VERSION}) ==="

    cp "$MANIFEST_DIR/v${VERSION}.xml" "$MAIN_MANIFEST"

    "$PROJECT_ROOT/gradlew" -p "$SCRIPT_DIR" assembleDebug -PversionCode="${VERSION}" --quiet

    APK_SRC="$SCRIPT_DIR/build/outputs/apk/debug/testapp-debug.apk"
    APK_DST="$OUTPUT_DIR/testapp-v${VERSION}.apk"
    cp "$APK_SRC" "$APK_DST"

    echo "  -> $APK_DST"
done

echo ""
echo "=== Done. APKs in $OUTPUT_DIR ==="
ls -lh "$OUTPUT_DIR"
