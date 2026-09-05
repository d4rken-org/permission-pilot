# Build Commands

## Building the Project

```bash
# Build debug version (both flavors)
./gradlew assembleDebug

# Build specific flavor and type
./gradlew assembleFossDebug
./gradlew assembleGplayRelease

# Build all variants (FOSS and Google Play flavors)
./gradlew assemble

# Build app bundles for Play Store
./gradlew bundleGplayRelease
```

## Testing

```bash
# Run all unit tests
./gradlew test

# Run unit tests for specific variant
./gradlew testFossDebugUnitTest
./gradlew testGplayDebugUnitTest

# Run instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest
./gradlew connectedFossDebugAndroidTest
```

## Code Quality

```bash
# Run lint vital checks (used in CI)
./gradlew lintVitalFossBeta lintVitalFossRelease lintVitalGplayBeta lintVitalGplayRelease

# Run lint for specific variant
./gradlew lintFossDebug
```

Verify that the gplay flavor is obfuscated and the foss flavor is not:

```bash
./gradlew assembleFossRelease assembleGplayRelease bundleGplayRelease
bash tools/r8/check-obfuscation.sh
```

## Screenshots

Screenshot generation and Play Store upload is a multi-step procedure — invoke the `/screenshots` skill.

## Release

```bash
./gradlew assembleFossRelease assembleGplayRelease
```

Version bumping and tagging is a separate procedure — invoke the `/release` skill.
