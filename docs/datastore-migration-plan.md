# Migrate FlowPreference → DataStoreValue

## Context

Replace `FlowPreference<T>` (SharedPreferences) with `DataStoreValue<T>` (AndroidX DataStore), porting the pattern from the butler project. Gains: atomic updates, no listener lifecycle management, built-in corruption handling, cross-project consistency.

## Reference Implementation

Butler project at `butler/main/app-common/src/main/java/eu/darken/butler/common/datastore/`:
- `DataStoreValue.kt` — core class with `flow`, `value()`, `value(v)`, `update()`, `valueBlocking`
- `DataStoreValueMoshi.kt` — butler uses kotlinx.serialization; **we need Moshi** instead

Key difference: butler uses `kotlinx.serialization`; this project uses Moshi. We'll port `DataStoreValue.kt` directly and write Moshi-specific reader/writer extensions.

---

## Phase 1: Infrastructure (additive, zero behavioral change)

### 1.1 Add DataStore dependency
**File:** `app/build.gradle.kts` (line ~180, dependencies block)
- Add `implementation("androidx.datastore:datastore-preferences:1.1.4")`

### 1.2 Create DataStoreValue
**New file:** `app/src/main/java/eu/darken/myperm/common/datastore/DataStoreValue.kt`

Port from butler's `DataStoreValue.kt`. Core API:
- `val flow: Flow<T>` — `dataStore.data.catch { IOException → emit defaults }.map { reader }.distinctUntilChanged()`
- `suspend fun value(): T` — `flow.first()`
- `suspend fun value(v: T)` — `update { v }`
- `suspend fun update((T) -> T?): Updated<T>` — atomic via `dataStore.updateData`
- `var valueBlocking: T` — `runBlocking` escape hatch
- `basicKey()`, `basicReader()`, `basicWriter()` helpers
- `DataStore<Preferences>.createValue()` factory (2 overloads: basic types + custom reader/writer)

**Note:** `basicKey()` must handle nullable defaults — for `T?` where `defaultValue` is `null`, the caller must pass an explicit `Preferences.Key<T>` rather than relying on runtime type inference.

### 1.3 Create Moshi extensions
**New file:** `app/src/main/java/eu/darken/myperm/common/datastore/DataStoreValueMoshi.kt`

Port serialization logic from existing `FlowPreferenceMoshiExtension.kt` (not butler's kotlinx version):
- `moshiReader<T>(moshi, defaultValue, fallbackToDefault)` — JSON deserialization with error handling
- `moshiWriter<T>(moshi)` — JSON serialization
- `DataStore<Preferences>.createValue(key, defaultValue, moshi)` — convenience factory with `fallbackToDefault` parameter (default: `true`)

### 1.4 Create test mock
**New file:** `app/src/testShared/java/testhelpers/datastore/MockDataStoreValue.kt`

Backed with `MutableStateFlow<T>`, same pattern as existing `MockFlowPreference`. Needed for ViewModel/consumer tests that inject Settings classes.

### 1.5 Unit tests
**New files:**
- `app/src/test/java/eu/darken/myperm/common/datastore/DataStoreValueTest.kt`
- `app/src/test/java/eu/darken/myperm/common/datastore/DataStoreValueMoshiTest.kt`

Port test patterns from butler's tests, adapted for Moshi.

---

## Phase 2: Migration (swap all 4 settings classes, update consumers, delete old code)

### 2.1 Migrate GeneralSettings
**File:** `app/src/main/java/eu/darken/myperm/settings/core/GeneralSettings.kt`

- Replace SharedPreferences with: `private val Context.dataStore by preferencesDataStore(name = "settings_core", produceMigrations = { listOf(SharedPreferencesMigration(it, "settings_core")) }, corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() })`
- Replace all `createFlowPreference` → `dataStore.createValue` (9 fields)
- Remove public `val preferences: SharedPreferences`

### 2.2 Migrate DebugSettings
**File:** `app/src/main/java/eu/darken/myperm/common/debug/autoreport/DebugSettings.kt`

- `preferencesDataStore(name = "debug_settings")` with `SharedPreferencesMigration` + `ReplaceFileCorruptionHandler`
- 1 field: `isAutoReportingEnabled`

### 2.3 Migrate BillingCache (gplay)
**File:** `app/src/gplay/java/eu/darken/myperm/common/upgrade/core/BillingCache.kt`

- `preferencesDataStore(name = "settings_gplay")` with `SharedPreferencesMigration` + `ReplaceFileCorruptionHandler`
- 1 field: `lastProStateAt`

### 2.4 Migrate FossCache (foss)
**File:** `app/src/foss/java/eu/darken/myperm/common/upgrade/core/FossCache.kt`

- `preferencesDataStore(name = "settings_foss")` with `SharedPreferencesMigration` + `ReplaceFileCorruptionHandler`
- 1 field: `upgrade` (Moshi-serialized `FossUpgrade?`) — use `fallbackToDefault = true` (changed from current `false` for resilience; default is `null` = "not upgraded", so corrupt JSON safely falls back)

### 2.5 Update consumer call sites

**`.flow` reads — no change needed (9 sites)**

| File | Usage |
|------|-------|
| `AppsViewModel.kt:43-44` | `appsFilterOptions.flow`, `appsSortOptions.flow` |
| `AppDetailsViewModel.kt:113` | `appDetailsFilterOptions.flow` |
| `PermissionsViewModel.kt:42-43` | `permissionsFilterOptions.flow`, `permissionsSortOptions.flow` |
| `PermissionDetailsViewModel.kt:98` | `permissionDetailsFilterOptions.flow` |
| `UpgradeControlFoss.kt:21` | `fossCache.upgrade.flow` |

**Sync reads → `valueBlocking` or `value()` (suspend)**

| File | Current | New |
|------|---------|-----|
| `MainActivity.kt:53` | `.value` | `.valueBlocking` |
| `MainActivityVM.kt:39` | `.value` (inside `onEach` coroutine) | `.value()` (suspend) |
| `App.kt:64` | `.value` (crash handler) | `.valueBlocking` |
| `IPCFunnel.kt:36` | `.value` (lazy init) | `.valueBlocking` |
| `UpgradeRepoGplay.kt:38` | `.value` (property getter) | `.valueBlocking` |

**Sync writes → suspend or blocking**

| File | Current | New |
|------|---------|-----|
| `OnboardingViewModel.kt:25` | `.value = true` (inside `launch`) | `.value(true)` (suspend) |
| `App.kt:66` | `.value = 1` (crash handler) | `try { .valueBlocking = 1 } catch (e: Exception) { log(...) }` |
| `UpgradeRepoGplay.kt:39` | `.update { value }` (property setter) | `.valueBlocking = value` |
| `UpgradeControlFoss.kt:38,46,53` | `.value = FossUpgrade(...)` (dialog callbacks, NOT suspend) | `.valueBlocking = FossUpgrade(...)` |

**`.update { }` → wrap in `launch { }`**

| File | Line(s) |
|------|---------|
| `MainActivityVM.kt` | 52 |
| `AppsViewModel.kt` | 136, 140 |
| `AppDetailsViewModel.kt` | 232 |
| `PermissionsViewModel.kt` | 189, 193 |
| `PermissionDetailsViewModel.kt` | 181 |

All in ViewModels extending `ViewModel3`/`ViewModel2` with `launch` available.

### 2.6 Delete old infrastructure

Remove:
- `common/preferences/FlowPreference.kt`
- `common/preferences/FlowPreferenceExtension.kt`
- `common/preferences/FlowPreferenceMoshiExtension.kt`
- `common/preferences/SharedPreferenceExtensions.kt`
- `testShared/.../preferences/MockFlowPreference.kt`
- `testShared/.../preferences/MockSharedPreferences.kt`
- `test/.../FlowPreferenceTest.kt`
- `test/.../FlowPreferenceMoshiTest.kt`
- `test/.../MockSharedPreferencesTest.kt`

---

## Edge Cases

1. **SharedPreferences key names** transfer directly — both systems use the same key strings
2. **Migration is one-shot** — `SharedPreferencesMigration` deletes SP file after. Verify in tests.
3. **Crash handler** (`App.kt:64-66`) — both read and write wrapped in try-catch; failed write during crash is acceptable, must not block default handler
4. **Startup latency** (`MainActivity.kt:53`) — `valueBlocking` with splash screen coverage. ~12 keys, sub-10ms including migration. Same pattern as butler.
5. **Thread safety** — `DataStoreValue.update` is atomic via `dataStore.updateData` (improvement over FlowPreference's non-thread-safe read-modify-write)
6. **Corruption handling** — `ReplaceFileCorruptionHandler { emptyPreferences() }` on each DataStore; per-value IOException caught in `flow`
7. **UpgradeControlFoss dialog callbacks** — main thread, non-suspend. Use `valueBlocking` for writes.

---

## Verification

1. `./gradlew testFossDebugUnitTest testGplayDebugUnitTest` — all unit tests pass
2. `./gradlew assembleFossDebug assembleGplayDebug` — both flavors build
3. **Automated migration tests**: pre-populate SP files (`settings_core`, `settings_gplay`, `settings_foss`, `debug_settings`) → assert DataStore values after first read. Include: malformed JSON keys, missing keys, empty SP file, verify SP file deleted post-migration.
4. Install over existing APK → verify settings migrated (manual)
5. Fresh install → verify defaults work (manual)
6. Filter/sort preferences survive app restart (manual)
