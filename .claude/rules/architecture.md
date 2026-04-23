# Architecture

## Module Structure

- **app/**: Main Android application with FOSS and Google Play flavors
- **buildSrc/**: Gradle build configuration, version management (`Versions.kt`, `ProjectConfig.kt`)

## Core Architecture Patterns

- **MVVM**: ViewModels with StateFlow for UI state management
- **Dependency Injection**: Hilt/Dagger for dependency management
- **Coroutines**: Extensive use of Kotlin coroutines for async operations
- **Repository Pattern**: Data layer abstraction with sealed State classes
- **Single Activity**: Navigation3 with Compose screens

## Base UI Classes (`app/src/main/java/eu/darken/myperm/common/uix/`)

- `ViewModel4`: Primary base class — implements `NavigationEventSource` + `ErrorEventSource2` via `SingleEventFlow` (all
  feature VMs extend this)
- `ViewModel3`: Legacy MVVM base with `SingleLiveEvent` (still exists, not used by new code)
- `ViewModel2`: Coroutine scope with error handlers
- `ViewModel1`: Basic logging
- `Activity2`: Base activity with logging
- `Service2`: Base service with logging

## Repository Pattern

Repositories expose `Flow<State>` with sealed state classes:
- `PermissionRepo` (`permissions/core/`): Permission data management
- `AppRepo` (`apps/core/`): Installed app data management

State pattern used throughout:
```kotlin
sealed class State {
    class Loading : State()
    data class Ready(val data: List<T>) : State()
}
```

## Navigation System

- Single Activity (`MainActivity`) with Navigation3 (Compose-based, no fragments)
- Custom `NavigationController` + `NavigationEntry` (not AndroidX Navigation fragments)
- `NavigationEventSource` interface: ViewModels expose `navEvents: SingleEventFlow<NavEvent>`
- `NavEvent` sealed interface: `GoTo(destination, popUpTo, inclusive)`, `Up`, and `Finish`
- `ErrorEventSource2` interface: ViewModels expose `errorEvents: SingleEventFlow<Throwable>`

## Settings System

- `GeneralSettings` singleton uses DataStore Preferences with Kotlinx Serialization
- Flow-based preference reading via `createValue()` with `kotlinxReader`/`kotlinxWriter` helpers
- SharedPreferences retained only for migration via `SharedPreferencesMigration`
- Located in `settings/core/GeneralSettings.kt`

## Data Flow

The app follows unidirectional data flow:

1. `AppRepo` queries PackageManager for installed apps
2. `PermissionRepo` aggregates permission data from apps
3. ViewModels combine repository flows with filter/sort options
4. Compose UI collects ViewModel state via StateFlow
5. User actions trigger ViewModel methods which update repository or navigate

## Project Structure

Representative structure (not exhaustive):

```
app/src/main/java/eu/darken/myperm/
├── main/
│   └── ui/         # MainActivity + overview & onboarding sub-features
├── permissions/    # Permissions feature
│   ├── core/       # PermissionRepo, data models
│   └── ui/         # List and details Compose screens
├── apps/           # Apps feature
│   ├── core/       # AppRepo, PackageManager interactions
│   └── ui/         # List and details Compose screens
├── watcher/        # Permission change monitoring
│   ├── core/       # WatcherManager, SnapshotDiffer, PermissionDiff
│   └── ui/         # Dashboard and report detail screens
├── export/         # Export permission/app data (CSV, Markdown, …)
│   ├── core/       # ExportEngine, ExportConfig, ExportFormat, formatters
│   └── ui/         # Export Compose screen
├── settings/       # Settings feature
│   ├── core/       # GeneralSettings
│   └── ui/         # Settings Compose screens
└── common/         # Shared utilities
    ├── uix/        # Base UI classes (ViewModel4, Activity2, Service2)
    ├── compose/    # Shared Compose components
    ├── coroutine/  # DispatcherProvider, AppScope
    ├── dagger/     # Hilt DI modules
    ├── datastore/  # DataStore helpers (createValue, kotlinxReader/Writer)
    ├── navigation/ # Navigation3 extensions, NavigationEventSource
    ├── room/       # Room database, DAOs, entities
    └── serialization/ # Kotlinx Serialization utilities
```

`common/` also holds utility submodules for notifications, theming, upgrade flow, background work, support/feedback, debug/logging, and misc helpers (collections, flow, livedata, error, coil, etc.).

## Key Dependencies

- **Jetpack Compose**: UI framework (Material 3)
- **Hilt**: Dependency injection framework
- **Navigation3**: Compose-based navigation
- **Kotlinx Serialization**: JSON serialization for settings and data
- **DataStore**: Preferences storage
- **Room**: Database for watcher snapshots
- **Coil**: Image loading for app icons
