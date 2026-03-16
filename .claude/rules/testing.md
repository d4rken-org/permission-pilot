# Testing Guidelines

## Test Locations

- **Unit Tests**: `app/src/test/` with shared utilities in `app/src/testShared/`
- **Instrumentation Tests**: `app/src/androidTest/`
- **Test Flavors**: Separate test configurations for FOSS and Google Play variants

## What to Test

- Write tests for data transformations, filters, and serialized data
- No UI tests required unless specifically requested

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
