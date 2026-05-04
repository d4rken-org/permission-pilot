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

## Screenshots

For the full regen + Play Store upload workflow (smoke-only tracking, on-demand full regen, manual upload), see `.claude/rules/screenshots.md`.

```bash
# Generate all localized screenshots (39 locales x 6 screens, batched)
./fastlane/generate_screenshots.sh

# Smoke test (6 locales for fast iteration)
./fastlane/generate_screenshots.sh --smoke

# Custom batch size
./fastlane/generate_screenshots.sh --batch-size 4

# Copy generated PNGs to fastlane metadata directories
./fastlane/copy_screenshots.sh

# Clean existing screenshots before copying
./fastlane/copy_screenshots.sh --clean

# Direct Gradle task (single batch, all locales — may OOM)
./gradlew updateGplayDebugScreenshotTest
```

## Release

```bash
./gradlew assembleFossRelease assembleGplayRelease
```

## Context Management

When running gradle builds or tests, use the Task tool with a sub-agent to keep verbose output isolated from the main conversation context. The sub-agent should report back only:
- Success or failure
- Compilation errors with file paths and line numbers
- Warning counts

Run gradle directly in the main context only when the user explicitly requests full output.
