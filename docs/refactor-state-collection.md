# Plan: Standardize state collection, add `asStateFlow` to ViewModel2, remove LiveData

## Context

The codebase has two state collection patterns: `collectAsState()` for `StateFlow` properties and a custom `waitForState()` wrapper around `produceState` for plain `Flow` properties. This refactor adds a `asStateFlow()` convenience extension to `ViewModel2`, converts all 5 feature ViewModels to use it, removes `waitForState`, and removes LiveData helpers from `ViewModel2`.

## Step 1: Update `ViewModel2` — add `asStateFlow`, remove LiveData helpers

**File:** `app/src/main/java/eu/darken/myperm/common/uix/ViewModel2.kt`

Add:
```kotlin
fun <T> Flow<T>.asStateFlow(defaultValue: T? = null): StateFlow<T?> = stateIn(
    vmScope,
    SharingStarted.WhileSubscribed(5000),
    defaultValue,
)
```
New imports: `SharingStarted`, `StateFlow`

Remove:
- `fun <T : Any> DynamicStateFlow<T>.asLiveData2() = flow.asLiveData2()`
- `fun <T> Flow<T>.asLiveData2() = this.asLiveData(context = getVMContext())`
- Import: `androidx.lifecycle.asLiveData`
- Import: `eu.darken.myperm.common.flow.DynamicStateFlow`

## Step 2: Fix `RecorderActivityVM` — inline the removed LiveData helper

**File:** `app/src/foss/java/eu/darken/myperm/common/debug/recording/ui/RecorderActivityVM.kt`

`RecorderActivityVM` is the sole user of `asLiveData2()`. Inline it:
```kotlin
// Before
val state = stater.asLiveData2()

// After
val state = stater.flow.asLiveData(vmScope.coroutineContext)
```
Add import: `androidx.lifecycle.asLiveData`

## Step 3: Convert 5 ViewModels — `Flow<State>` → `StateFlow<State?>` via `asStateFlow()`

Each ViewModel: change type to `StateFlow<State?>`, replace `.onStart { ... }` with `.asStateFlow()`.

**a) `app/src/main/java/eu/darken/myperm/apps/ui/list/AppsViewModel.kt`**
```kotlin
// Before
val state: Flow<State> = combine(...).onStart { emit(State.Loading) }

// After
val state = combine(...).asStateFlow()
```
Remove `Flow` import, add nothing (asStateFlow is inherited from ViewModel2).

**b) `app/src/main/java/eu/darken/myperm/permissions/ui/list/PermissionsViewModel.kt`**
Same pattern as (a).

**c) `app/src/main/java/eu/darken/myperm/main/ui/overview/OverviewViewModel.kt`**
```kotlin
// Before
val state: Flow<State> = combine(...) { ... }

// After
val state = combine(...) { ... }.asStateFlow()
```
No top-level `.onStart` to remove. Inner flows keep their own `.onStart` (needed for `combine` to emit early).
Remove `Flow` import.

**d) `app/src/main/java/eu/darken/myperm/apps/ui/details/AppDetailsViewModel.kt`**
```kotlin
// Before
val state: Flow<State> by lazy {
    combine(...).onStart { emit(State(label = initialLabel ?: pkgId.pkgName)) }
}

// After
val state by lazy {
    combine(...).asStateFlow()
}
```
Keep `by lazy` (required because `init()` sets `pkgId` before first access). Remove `.onStart`.
Remove `Flow` import.

**e) `app/src/main/java/eu/darken/myperm/permissions/ui/details/PermissionDetailsViewModel.kt`**
Same pattern as (d). Remove `.onStart`, keep `by lazy`.

## Step 4: Update 5 Screen hosts — `waitForState` → `collectAsState`

Replace `waitForState(vm.state)` with `vm.state.collectAsState()`. The `state?.let { ... }` pattern stays the same (nullable StateFlow).

**a) `app/src/main/java/eu/darken/myperm/apps/ui/list/AppsScreen.kt`**
```kotlin
// Before
import eu.darken.myperm.common.compose.waitForState
val state by waitForState(vm.state)

// After
import androidx.compose.runtime.collectAsState
val state by vm.state.collectAsState()
```

**b) `app/src/main/java/eu/darken/myperm/permissions/ui/list/PermissionsScreen.kt`** — same

**c) `app/src/main/java/eu/darken/myperm/main/ui/overview/OverviewScreen.kt`** — same

**d) `app/src/main/java/eu/darken/myperm/apps/ui/details/AppDetailsScreen.kt`** — same

**e) `app/src/main/java/eu/darken/myperm/permissions/ui/details/PermissionDetailsScreen.kt`** — same

## Step 5: Delete `waitForState`

**Delete:** `app/src/main/java/eu/darken/myperm/common/compose/ViewModelExtensions.kt`

## Files touched (12)

| File | Action |
|------|--------|
| `common/uix/ViewModel2.kt` | Add `asStateFlow()`, remove `asLiveData2()` |
| `common/compose/ViewModelExtensions.kt` | Delete |
| `foss/.../RecorderActivityVM.kt` | Inline `asLiveData` call |
| `apps/ui/list/AppsViewModel.kt` | `.asStateFlow()` |
| `apps/ui/details/AppDetailsViewModel.kt` | `.asStateFlow()` |
| `permissions/ui/list/PermissionsViewModel.kt` | `.asStateFlow()` |
| `permissions/ui/details/PermissionDetailsViewModel.kt` | `.asStateFlow()` |
| `main/ui/overview/OverviewViewModel.kt` | `.asStateFlow()` |
| `apps/ui/list/AppsScreen.kt` | `collectAsState()` |
| `apps/ui/details/AppDetailsScreen.kt` | `collectAsState()` |
| `permissions/ui/list/PermissionsScreen.kt` | `collectAsState()` |
| `permissions/ui/details/PermissionDetailsScreen.kt` | `collectAsState()` |
| `main/ui/overview/OverviewScreen.kt` | `collectAsState()` |

## Not in scope (follow-up)
- Migrate `RecorderActivity` / `RecorderActivityVM` from ViewModel3 + LiveData to ViewModel4 + Compose
- Delete `ViewModel3`, `SingleLiveEvent`, old `NavEventSource`, old `ErrorEventSource`, `Activity2.observe2`
- Remove dead `ErrorEventSource` check in `ViewModel2.getErrorHandler()`

## Verification

1. `./gradlew assembleDebug` — compilation check (both foss and gplay flavors)
2. Manual: open each screen (Overview, Apps, Permissions, App details, Permission details) — data loads, no blank screens
