# Plan: Upgrade UX + Pro-Gated Export

## Context

The app's "pro" upgrade is purely a donation — it unlocks no features. The nag dialog fires every 8th launch (`MainActivityVM.kt:55`) regardless of user engagement, with generic copy. This plan adds a pro-gated export feature as a concrete incentive and improves the upgrade prompt timing and messaging to better convert engaged users.

---

## Part 1: Smarter Nag Dialog

### Current behavior
- `MainActivityVM.kt:55` — fires when `launchCount % 8 == 0`
- No consideration of user engagement or install age
- Generic message: "no ads, doesn't collect your data"

### 1.1 Add engagement preferences to `GeneralSettings.kt`

Add two new preferences after `launchCount` (line 31):

```kotlin
val firstLaunchAt = preferences.createFlowPreference("core.stats.first_launch_at", 0L)
val detailViewCount = preferences.createFlowPreference("core.stats.detail_views", 0)
```

### 1.2 Update nag logic in `MainActivityVM.kt`

**In `increaseLaunchCount()` (line 66):** Set `firstLaunchAt` if still 0:
```kotlin
if (generalSettings.firstLaunchAt.value == 0L) {
    generalSettings.firstLaunchAt.update { System.currentTimeMillis() }
}
```

**In `init` block (line 54-57):** Replace `launchCount % 8` with:
```
Show nag when ALL true:
  1. NOT isPro (already checked at line 52)
  2. firstLaunchAt != 0 AND >= 3 days since firstLaunchAt
     (firstLaunchAt == 0 means not yet initialized — skip nag.
      This handles race: init runs before increaseLaunchCount sets it.
      For existing users updating, firstLaunchAt gets set on next launch,
      so they'll be eligible after 3 days from update.)
  3. detailViewCount >= 5
  4. launchCount % 10 == 0
```

### 1.3 Increment `detailViewCount` in list ViewModel click handlers

Increment when the user explicitly navigates to a detail screen (not in detail VM's `init(route)`, which can re-fire on recomposition).

**`AppsViewModel.kt`** — in `onAppClicked()` (line 124):
```kotlin
fun onAppClicked(item: AppItem) {
    log(TAG) { "Navigating to ${item.id}" }
    generalSettings.detailViewCount.update { it + 1 }
    navTo(Nav.Details.AppDetails(...))
}
```

**`PermissionsViewModel.kt`** — in `onPermissionClicked()` (line 179):
```kotlin
fun onPermissionClicked(item: PermItem) {
    log(TAG) { "Navigating to ${item.id}" }
    generalSettings.detailViewCount.update { it + 1 }
    navTo(Nav.Details.PermissionDetails(...))
}
```

### 1.4 Update upgrade dialog strings in `strings.xml`

Update `upgrade_myperm_label` (line 18) and `upgrade_myperm_description` (line 19):
- Title: `"Support Permission Pilot"`
- Body: `"Permission Pilot is actively maintained, ad-free, and respects your privacy. Upgrade to Pro to support continued development!"`

Note: Localized overrides in other locales will continue using their existing translations until updated separately.

### Files modified (Part 1)
- `app/src/main/java/eu/darken/myperm/settings/core/GeneralSettings.kt`
- `app/src/main/java/eu/darken/myperm/main/ui/MainActivityVM.kt`
- `app/src/main/java/eu/darken/myperm/apps/ui/list/AppsViewModel.kt`
- `app/src/main/java/eu/darken/myperm/permissions/ui/list/PermissionsViewModel.kt`
- `app/src/main/res/values/strings.xml`

---

## Part 2: Export Feature (Pro-Gated)

### 2.1 Create `ExportManager`

**New file:** `app/src/main/java/eu/darken/myperm/common/export/ExportManager.kt`

- `@Singleton`, Hilt-injected
- Dependencies: `@ApplicationContext Context`, `AppRepo`, `PermissionRepo`, `DispatcherProvider`
- `suspend fun exportApps(): File` — writes CSV to `cacheDir/exports/` with timestamped filename
- `suspend fun exportPermissions(): File` — writes CSV to `cacheDir/exports/` with timestamped filename
- Exports ALL data (not filtered view) — guard in VMs ensures repo is Ready before calling
- Before writing, prune any existing files in `exports/` directory (prevents stale file accumulation)
- Write with `BufferedWriter` on `Dispatchers.IO`, explicit UTF-8 charset
- Sort apps by package name, permissions by ID for deterministic output
- CSV escaping: wrap fields in quotes, double-escape internal quotes, handle newlines/CR, prefix formula-injection chars (`=`,`+`,`-`,`@`,`\t`) with `'`
- Null fields written as empty string

**Apps CSV columns:** Package Name, App Label, System App, Install Source, Installed At (ISO-8601), Updated At (ISO-8601), Permissions Requested, Permissions Granted, Internet Access (DIRECT/INDIRECT/NONE/UNKNOWN)

**Permissions CSV columns:** Permission ID, Permission Label, Type, Protection Level (or empty), Apps Requesting, Apps Granted

### 2.2 Move FileProvider to main manifest

Currently FileProvider is only in `app/src/foss/AndroidManifest.xml` (lines 5-13). Move it to `app/src/main/AndroidManifest.xml` so both flavors can share files via content URIs.

- **`app/src/main/AndroidManifest.xml`** — add FileProvider block inside `<application>`
- **`app/src/foss/AndroidManifest.xml`** — remove FileProvider block (keep RecorderActivity/RecorderService entries)
- **`app/src/main/res/xml/file_provider_paths.xml`** — add `<cache-path name="exports" path="exports" />`
- **Verify:** debug log sharing (existing `debug_logs` cache-path) still works after changes

### 2.3 Add export to overflow menus (Compose)

No XML menu files exist. Menus are inline Compose `DropdownMenu` in:
- **`AppsScreen.kt`** lines 157-171 — add Export `DropdownMenuItem` before Refresh
- **`PermissionsScreen.kt`** lines ~190-219 — add Export `DropdownMenuItem` before Expand All

Use `Icons.Filled.FileDownload` (from `androidx.compose.material.icons.filled`) — no custom drawable needed.

Add new callback parameters:
- `AppsScreen`: add `onExport: () -> Unit` parameter
- `PermissionsScreen`: add `onExport: () -> Unit` parameter

Update all preview composables with `onExport = {}` to prevent compilation errors.

### 2.4 Wire up Apps export flow

**`AppsViewModel.kt`:**
- Inject `ExportManager` and `UpgradeRepo`
- Add `val exportEvents = SingleEventFlow<ExportEvent>()`
- Add sealed class:
  ```kotlin
  sealed class ExportEvent {
      data object ShowProGate : ExportEvent()
      data class ShareExport(val file: File) : ExportEvent()
  }
  ```
- Add `private var isExporting = false` guard flag
- Add `fun onExport()`:
  - Guard: if `isExporting`, return
  - Guard: if repo state not Ready, emit snackbar/error ("Data still loading") and return
  - Check `upgradeRepo.isPro()` → if not pro, emit `ShowProGate`; if pro, set `isExporting = true`, run export in `launch`, emit `ShareExport(file)`, set `isExporting = false`
  - Wrap in try/catch/finally, emit error on failure via `errorEvents`, reset `isExporting` in finally
- Add `fun launchUpgrade(activity: Activity)`:
  ```kotlin
  upgradeRepo.launchBillingFlow(activity)
  ```

**`AppsScreenHost` (in `AppsScreen.kt`):**
- Collect `vm.exportEvents` in a `LaunchedEffect`
- `ShowProGate` → set `showProGateDialog = true` state
- `ShareExport` → create share intent via FileProvider with `FLAG_GRANT_READ_URI_PERMISSION`, MIME `text/csv`, wrapped in `Intent.createChooser()`. Get Activity via `LocalContext.current`. Wrap `startActivity` in try/catch for `ActivityNotFoundException`.
- Show pro gate `AlertDialog` when `showProGateDialog` is true:
  - Title: string resource `export_pro_required_title`
  - Body: string resource `export_pro_required_description`
  - Positive button: "Upgrade" → calls `vm.launchUpgrade(activity)`, dismiss dialog
  - Negative button: "Cancel" → dismiss dialog
- Pass `onExport = { vm.onExport() }` to `AppsScreen`

### 2.5 Wire up Permissions export flow

Same pattern as Apps, applied to:
- **`PermissionsViewModel.kt`** — inject `ExportManager`, `UpgradeRepo`, add `exportEvents`, `onExport()`, `launchUpgrade()`, `isExporting` guard
- **`PermissionsScreenHost` (in `PermissionsScreen.kt`)** — collect `exportEvents`, show pro gate dialog, handle share intent with try/catch
- Pass `onExport = { vm.onExport() }` to `PermissionsScreen`

### 2.6 New strings

Add to `app/src/main/res/values/strings.xml`:
```xml
<string name="general_export_action">Export</string>
<string name="export_pro_required_title">Pro Feature</string>
<string name="export_pro_required_description">Exporting data is a Pro feature. Upgrade to support development and unlock exports.</string>
```

### Files modified (Part 2)
- `app/src/main/java/eu/darken/myperm/common/export/ExportManager.kt` (new)
- `app/src/main/AndroidManifest.xml`
- `app/src/foss/AndroidManifest.xml`
- `app/src/main/res/xml/file_provider_paths.xml`
- `app/src/main/java/eu/darken/myperm/apps/ui/list/AppsViewModel.kt`
- `app/src/main/java/eu/darken/myperm/apps/ui/list/AppsScreen.kt`
- `app/src/main/java/eu/darken/myperm/permissions/ui/list/PermissionsViewModel.kt`
- `app/src/main/java/eu/darken/myperm/permissions/ui/list/PermissionsScreen.kt`
- `app/src/main/res/values/strings.xml`

### FOSS flavor handling

Export is gated via `UpgradeRepo.isPro()` on both flavors. FOSS users who "donate" (any of the 3 dialog options) get `isPro = true` via honor system, which unlocks export. No flavor-specific code needed.

---

## Implementation Order

1. Add `firstLaunchAt` and `detailViewCount` preferences to `GeneralSettings`
2. Update `MainActivityVM` nag logic + `firstLaunchAt` tracking
3. Add `detailViewCount` increment in `AppsViewModel.onAppClicked()` and `PermissionsViewModel.onPermissionClicked()`
4. Update upgrade dialog strings
5. Move FileProvider to main manifest + update paths xml
6. Create `ExportManager`
7. Add export menu items + callbacks to `AppsScreen` and `PermissionsScreen` (including preview updates)
8. Wire up `AppsViewModel` + `AppsScreenHost` (export + pro gate + upgrade flow)
9. Wire up `PermissionsViewModel` + `PermissionsScreenHost` (same pattern)

---

## Verification

1. **Build:** `./gradlew assembleFossDebug` and `./gradlew assembleGplayDebug`
2. **Unit tests:** `./gradlew test` — add tests for CSV escaping (formula injection, quotes, newlines, null fields)
3. **Manual testing on device:**
   - Non-pro user: tap Export → see pro gate dialog → tap Upgrade → billing/donation flow launches
   - Pro user: tap Export → CSV generated → share sheet opens
   - Nag dialog: verify it doesn't appear in first 3 days or with < 5 detail views
   - Nag dialog: verify it appears on 10th launch after meeting engagement criteria
   - Verify debug log sharing still works (FileProvider regression check)
4. **Verify CSV output:** check proper escaping, correct column data, deterministic sort order, file opens in spreadsheet app
