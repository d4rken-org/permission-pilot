# Code Style

## General Principles

- Package by feature, not by layer
- Prefer adding to existing files unless creating new logical components
- Write minimalistic and concise code
- Don't add code comments for obvious code
- Prefer flow-based solutions
- Prefer reactive programming

## Kotlin Conventions

- Add trailing commas for multi-line parameter lists and collections
- When using `if` that is not single-line, always use brackets

## Coroutine Dispatchers

Use `DispatcherProvider` interface for testability instead of hardcoded dispatchers. Inject via Hilt and access `Default`, `IO`, `Main` dispatchers.

## ViewModel Creation Pattern

```kotlin
@HiltViewModel
class MyFeatureViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val myRepo: MyRepo,
) : ViewModel4(dispatcherProvider = dispatcherProvider)
```

## Screen Creation Pattern

```kotlin
@Composable
fun MyFeatureScreenHost(vm: MyFeatureViewModel = hiltViewModel()) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)
    val state by vm.state.collectAsState()
    MyFeatureScreen(state = state, ...)
}
```

## Logging

Use `logTag()` to create tags and `log()` with lambda messages:

```kotlin
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.debug.logging.Logging.Priority.*

companion object {
    private val TAG = logTag("Apps", "Repo")
}

log(TAG) { "Processing $item" }           // DEBUG (default)
log(TAG, WARN) { "Unexpected state" }     // WARN
```

## UI Patterns

- Jetpack Compose is the sole UI framework — no XML layouts remain
- Material 3 theming and design system
- Single Activity architecture with Compose-based navigation

## Error Handling

- ViewModels implement `ErrorEventSource2`, exposing `errorEvents: SingleEventFlow<Throwable>`
- Compose screens wire errors via `ErrorEventHandler(vm)` composable

## Data & State

- Reactive programming with Kotlin Flow and StateFlow
- DataStore with Kotlinx Serialization for settings
- Room for database operations
- Coil for image loading
