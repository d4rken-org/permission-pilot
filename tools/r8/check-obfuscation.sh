#!/usr/bin/env bash
# Asserts that the gplay flavor ships obfuscated and the foss flavor does not.
#
# Requires a prior build of:
#   ./gradlew assembleFossRelease assembleGplayRelease bundleGplayRelease
set -euo pipefail

cd "$(dirname "$0")/../.."

FOSS_CONFIG="app/build/outputs/mapping/fossRelease/configuration.txt"
GPLAY_CONFIG="app/build/outputs/mapping/gplayRelease/configuration.txt"
GPLAY_MAPPING="app/build/outputs/mapping/gplayRelease/mapping.txt"

for f in "$FOSS_CONFIG" "$GPLAY_CONFIG" "$GPLAY_MAPPING"; do
    if [ ! -f "$f" ]; then
        echo "FAIL: missing $f - run the release builds first"
        exit 1
    fi
done

# a) foss keeps -dontobfuscate. configuration.txt reproduces rule-file comments verbatim,
# so the pattern is anchored to the start of a line to not match commentary.
if [ "$(grep -cE '^-dontobfuscate\b' "$FOSS_CONFIG" || true)" -ge 1 ]; then
    echo "OK: fossRelease is configured with -dontobfuscate"
else
    echo "FAIL: fossRelease has no -dontobfuscate rule ($FOSS_CONFIG)"
    exit 1
fi

# b) gplay must not inherit it.
if [ "$(grep -cE '^-dontobfuscate\b' "$GPLAY_CONFIG" || true)" -eq 0 ]; then
    echo "OK: gplayRelease has no -dontobfuscate rule"
else
    echo "FAIL: gplayRelease is configured with -dontobfuscate ($GPLAY_CONFIG)"
    exit 1
fi

# c) app classes were actually renamed. mapping.txt is ~80MB, so it is only ever streamed.
RENAMED=$(grep -E '^eu\.darken\.myperm\..* -> ' "$GPLAY_MAPPING" \
    | grep -v 'R8\$\$REMOVED' \
    | awk -F' -> ' '$1":" != $2' \
    | wc -l || true)
if [ "$RENAMED" -ge 1 ]; then
    echo "OK: gplayRelease renamed $RENAMED app classes"
else
    echo "FAIL: gplayRelease renamed no app classes ($GPLAY_MAPPING)"
    exit 1
fi

# d) the keep rules held: these classes must map onto themselves.
for cls in \
    eu.darken.myperm.apps.ui.list.AppsViewModel \
    eu.darken.myperm.main.ui.MainActivityVM \
    'eu.darken.myperm.apps.core.manifest.ApkManifestReader$MalformedApkException' \
    'eu.darken.myperm.common.debug.recording.core.RecorderModule$RecordingStartFailedException' \
    eu.darken.myperm.common.BuildConfigWrap \
    ; do
    if grep -qF "$cls -> $cls:" "$GPLAY_MAPPING"; then
        echo "OK: kept name $cls"
    else
        echo "FAIL: $cls is not mapped onto itself ($GPLAY_MAPPING)"
        exit 1
    fi
done

# e) Play reads the mapping from this bundle entry; without it the obfuscation score is not counted.
BUNDLE=$(ls app/build/outputs/bundle/gplayRelease/*.aab 2>/dev/null | head -n 1 || true)
if [ -z "$BUNDLE" ]; then
    echo "FAIL: no .aab in app/build/outputs/bundle/gplayRelease/ - run bundleGplayRelease first"
    exit 1
fi
if unzip -l "$BUNDLE" | grep -qF 'BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map'; then
    echo "OK: $BUNDLE carries the obfuscation mapping"
else
    echo "FAIL: $BUNDLE has no BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map"
    exit 1
fi

# f) navigation keys must stay distinct classes - a merge shows up as two left sides sharing
# one right-hand target, which would break destination identity.
NAV_DUPES=$(grep -E '^eu\.darken\.myperm\.common\.navigation\.Nav\$.* -> ' "$GPLAY_MAPPING" \
    | grep -v 'R8\$\$REMOVED' \
    | awk -F' -> ' '{sub(/:$/, "", $2); print $2}' \
    | sort \
    | uniq -d || true)
if [ -z "$NAV_DUPES" ]; then
    echo "OK: navigation keys map to distinct classes"
else
    echo "FAIL: navigation keys share a target class:"
    echo "$NAV_DUPES"
    exit 1
fi

MERGED=$(grep -E '^eu\.darken\.myperm\..* -> ' "$GPLAY_MAPPING" \
    | grep -v 'R8\$\$REMOVED' \
    | awk -F' -> ' '{sub(/:$/, "", $2); print $2}' \
    | sort \
    | uniq -d \
    | wc -l || true)
echo "INFO: $MERGED app-class targets are shared by more than one source class"

echo "All obfuscation checks passed"
