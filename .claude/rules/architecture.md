# Architecture

## Module Structure

- **app/**: Main Android application with FOSS and Google Play flavors
- **buildSrc/**: Gradle build configuration, version management (`Versions.kt`, `ProjectConfig.kt`)

## Core Architecture Patterns

- **MVVM**: ViewModels with LiveData/StateFlow for UI state management
- **Dependency Injection**: Hilt/Dagger for dependency management
- **Coroutines**: Extensive use of Kotlin coroutines for async operations
- **Repository Pattern**: Data layer abstraction with sealed State classes
- **Single Activity**: Navigation Component with multiple fragments

## Base UI Classes (`app/src/main/java/eu/darken/myperm/common/uix/`)

- `ViewModel3`: Full MVVM support with nav events + error events (most feature VMs extend this)
- `ViewModel2`: Coroutine scope with error handlers
- `ViewModel1`: Basic logging
- `Fragment3`: MVVM integration, observes navEvents/errorEvents from ViewModel
- `Fragment2`: Lifecycle logging
- `Activity2`: Base activity with logging

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

- Single Activity (`MainActivity`) with `NavHostFragment`
- AndroidX Navigation with Safe Args (KSP-generated)
- Navigation graphs: `res/navigation/main_navigation.xml`, `res/navigation/bottom_navigation.xml`
- `NavEventSource` interface: ViewModels expose `navEvents: SingleLiveEvent<NavDirections>`
- `ErrorEventSource` interface: ViewModels expose `errorEvents: SingleLiveEvent<Throwable>`

## Settings System

- `GeneralSettings` singleton uses SharedPreferences with Moshi JSON serialization
- Flow-based preference reading via `createFlowPreference()`
- Located in `settings/core/GeneralSettings.kt`

## Data Flow

The app follows unidirectional data flow:

1. `AppRepo` queries PackageManager for installed apps
2. `PermissionRepo` aggregates permission data from apps
3. ViewModels combine repository flows with filter/sort options
4. UI observes ViewModel state via LiveData
5. User actions trigger ViewModel methods which update repository or navigate

## Project Structure

```
app/src/main/java/eu/darken/myperm/
├── main/           # MainActivity, main navigation hub
├── permissions/    # Permissions feature
│   ├── core/       # PermissionRepo, data models
│   └── ui/         # List and details fragments
├── apps/           # Apps feature
│   ├── core/       # AppRepo, PackageManager interactions
│   └── ui/         # List and details fragments
├── settings/       # Settings feature
│   ├── core/       # GeneralSettings
│   └── ui/         # Settings fragments
└── common/         # Shared utilities
    ├── uix/        # Base UI classes
    ├── coroutine/  # DispatcherProvider, AppScope
    ├── dagger/     # Hilt DI modules
    ├── navigation/ # Nav extensions
    ├── lists/      # ModularAdapter pattern for RecyclerView
    └── preferences/# FlowPreference utilities
```

## Key Dependencies

- **Hilt**: Dependency injection framework
- **AndroidX Navigation**: Fragment navigation with SafeArgs
- **Moshi**: JSON serialization for settings and data
- **Coil**: Image loading for app icons
- **Material Design**: UI components
