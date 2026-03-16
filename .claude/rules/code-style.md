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
class MyFeatureVM @Inject constructor(
    private val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val myRepo: MyRepo,
) : ViewModel3(dispatcherProvider = dispatcherProvider)
```

## Fragment Creation Pattern

```kotlin
@AndroidEntryPoint
class MyFeatureFragment : Fragment3(R.layout.my_feature_fragment) {
    override val vm: MyFeatureVM by viewModels()
    override val ui: MyFeatureFragmentBinding by viewBinding()
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

- XML layouts with ViewBinding for UI components
- Material 3 theming and design system
- Single Activity architecture with Fragment-based navigation
- Compose is used for new screens alongside existing XML/ViewBinding

## Error Handling

- Use the established error handling patterns with `ErrorEventSource`
- ViewModels expose `errorEvents: SingleLiveEvent<Throwable>`

## Data & State

- Reactive programming with Kotlin Flow and StateFlow
- SharedPreferences with Moshi JSON serialization for settings
- Room for database operations
- Coil for image loading
