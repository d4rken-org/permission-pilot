# Testing Guidelines

## Test Locations

- **Unit Tests**: `app/src/test/` — runs on the **JUnit Platform** (`useJUnitPlatform()` in `app/build.gradle.kts`). **Jupiter** is the primary engine for new tests; **Vintage** is kept for JUnit 4 compatibility; **Kotest** is used for assertions. Base class: `app/src/test/java/testhelper/BaseTest.kt` (imports `org.junit.jupiter.api.AfterAll`).
- **Instrumentation Tests**: `app/src/androidTest/` — stays on **JUnit 4**. Base class: `app/src/testShared/java/testhelpers/BaseTestInstrumentation.kt` (imports `org.junit.AfterClass`).
- **Shared test source**: `app/src/testShared/` is wired into both `test` and `androidTest` source sets (see `sourceSets { … }` in `app/build.gradle.kts`) — place helpers there only when both unit and instrumentation tests need them.
- **Screenshot Tests**: `app/src/screenshotTest/kotlin/` — see the "Screenshot Tests" section below.
- **Test Flavors**: Separate test configurations for FOSS and Google Play variants.

## What to Test

- Write tests for data transformations, filters, and serialized data
- No new UI tests required by default — but existing screenshot tests under `app/src/screenshotTest/` should be updated and validated when the screens they cover change

## Testing Patterns

```kotlin
class ExampleTest : BaseTest() {
    @Test
    fun `descriptive test name with backticks`() {
        // Arrange
        val input = createTestData()

        // Act
        val result = functionUnderTest(input)

        // Assert
        result shouldBe expected
    }
}
```

## Running Tests

```bash
# Run all unit tests
./gradlew test

# Run unit tests for specific variant
./gradlew testFossDebugUnitTest
./gradlew testGplayDebugUnitTest

# Run instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

## Screenshot Tests

Compose preview-based screenshot tests (enabled by `android.experimental.enableScreenshotTest = true` in `app/build.gradle.kts`). These are the mechanism behind the **fastlane Play Store screenshot pipeline** — not a separate system.

- **Test entrypoints**: `app/src/screenshotTest/kotlin/eu/darken/myperm/screenshots/` (`PlayStoreScreenshots.kt`, `PlayStoreLocales.kt`)
- **Screen content under test**: `app/src/debug/java/eu/darken/myperm/screenshots/ScreenshotContent.kt` — edit this file when updating what a screenshot shows
- **Driven by**: `fastlane/generate_screenshots.sh`, which runs `./gradlew updateGplayDebugScreenshotTest` and patches `PlayStoreLocales.kt` per batch

See `.claude/rules/build-commands.md` for the fastlane wrapper commands (`generate_screenshots.sh`, `copy_screenshots.sh`).
