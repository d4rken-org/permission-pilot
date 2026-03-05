# Upgrade Screen: Dialog to Full-Screen Compose

## Context

The current upgrade flow shows a bare `MaterialAlertDialog` from within the repository layer (`UpgradeRepoGplay.launchBillingFlow` / `UpgradeControlFoss.launchBillingFlow`). This mixes UI concerns into the data layer and provides a poor user experience.

**Goal**: Replace the dialog with a dedicated full-screen Compose upgrade screen per flavor (GPlay and FOSS), following the bluemusic reference pattern. Add subscription + trial support alongside the existing one-time IAP for GPlay.

## Scope Summary

- **New files**: ~10 (screens, viewmodels, navigation entries, events, utilities)
- **Modified files**: ~14 (billing infra, repos, interface, entry points, strings)
- **Deleted files**: 1 (`AvailableSku.kt`)

---

## Phase 1: Billing Infrastructure Refactor (GPlay only)

### 1.1 Refactor Sku model
**File**: `app/src/gplay/.../data/Sku.kt`

Replace `data class Sku(val id: String)` with interface hierarchy:
- `Sku` interface with `id: String`, `type: Type`
- `Sku.Iap` sub-interface (type = IAP)
- `Sku.Subscription` sub-interface (type = SUBSCRIPTION) with `offers: Collection<Offer>`
- `Sku.Subscription.Offer` with `basePlanId`, `offerId`, `matches(SubscriptionOfferDetails)` method
- `Sku.Type` enum: `IAP`, `SUBSCRIPTION`

**New file**: `app/src/gplay/.../data/SkuDetails.kt` — `data class SkuDetails(val sku: Sku, val details: ProductDetails)`

**Delete**: `app/src/gplay/.../data/AvailableSku.kt` (only used by old `MyPermSku` enum)

### 1.2 Refactor MyPermSku
**File**: `app/src/gplay/.../MyPermSku.kt`

Change from enum to interface with concrete objects:
- `MyPermSku.Iap.PRO_UPGRADE` — keeps existing ID `${APPLICATION_ID}.iap.upgrade.pro`
- `MyPermSku.Sub.PRO_UPGRADE` — new placeholder ID `${APPLICATION_ID}.sub.upgrade.pro`
  - `BASE_OFFER` (basePlanId: `upgrade-pro-baseplan`)
  - `TRIAL_OFFER` (basePlanId: `upgrade-pro-baseplan`, offerId: `upgrade-pro-baseplan-trial`)
- `MyPermSku.PRO_SKUS` = setOf(Sub.PRO_UPGRADE, Iap.PRO_UPGRADE)

### 1.3 Extend BillingClientConnection
**File**: `app/src/gplay/.../client/BillingClientConnection.kt`

- Replace single `purchasesLocal` with separate `queryCacheIaps` + `queryCacheSubs` caches
- `purchases` flow combines `purchaseEvents`, iap cache, sub cache — **dedupe by `purchaseToken`** (not `orderId!!` which is nullable for pending purchases)
- Filter purchases to `PurchaseState.PURCHASED` before entitlement and acknowledgment
- Add `refreshPurchases()` — queries INAPP + SUBS in parallel via **`supervisorScope { async {} }`** (partial success: keep INAPP results even if SUBS query fails)
- Replace `querySku(sku)` with `querySkus(vararg skus: Sku): Collection<SkuDetails>` — uses `sku.type` for product type
- Update `launchBillingFlow(activity, sku, offer?)` — handles subscription offer tokens, **must run on main thread** via `withContext(dispatcherProvider.Main.immediate)`
- If offer token not found for a subscription, disable that CTA in the UI (don't crash)

### 1.4 Update BillingClientConnectionProvider
**File**: `app/src/gplay/.../client/BillingClientConnectionProvider.kt`

- `purchasePublisher` becomes `purchaseEvents: MutableStateFlow<Pair<BillingResult, Collection<Purchase>?>?>`
- Initial purchase query uses `refreshPurchases()` (both INAPP + SUBS)
- Pass `purchaseEvents` to `BillingClientConnection`

### 1.5 Update BillingDataRepo
**File**: `app/src/gplay/.../data/BillingDataRepo.kt`

- Add `querySkus(vararg skus: Sku)` delegating to connection
- Update `startIapFlow` → `launchBillingFlow(activity, sku, offer?)` delegating to connection
- Add `suspend fun refresh()` calling `connection.refreshPurchases()`
- Keep existing `billingData` flow + acknowledgment logic

### 1.6 Update PurchasedSku / BillingData
**Files**: `app/src/gplay/.../data/PurchasedSku.kt`, `BillingData.kt`

- `PurchasedSku.sku` type changes from `data class Sku` to `interface Sku`
- Product ID → SKU mapping resolves against `MyPermSku.PRO_SKUS`

---

## Phase 2: UpgradeRepo Interface

### 2.1 Update UpgradeRepo interface
**File**: `app/src/main/.../upgrade/UpgradeRepo.kt`

- Remove `fun launchBillingFlow(activity: Activity)`
- Add `suspend fun refresh()`

### 2.2 Update UpgradeRepoGplay
**File**: `app/src/gplay/.../UpgradeRepoGplay.kt`

- Remove the `MaterialAlertDialog` code entirely
- Add public methods for VM consumption: `querySkus()`, `launchBillingFlow(activity, sku, offer)`, `refresh()`
- `Info` exposes `upgrades: Collection<PurchasedSku>` for VM to check availability

### 2.3 Update UpgradeControlFoss
**File**: `app/src/foss/.../UpgradeControlFoss.kt`

- Remove `MaterialAlertDialog` code
- Add `launchGithubSponsorsUpgrade()` — sets `FossUpgrade` + opens `https://github.com/sponsors/d4rken`
- Add `override suspend fun refresh()` with refreshTrigger pattern
- Add `GITHUB_SPONSORS` to `FossUpgrade.Reason` enum with `@Json(name = "foss.upgrade.reason.github_sponsors")` (keep existing values for backward compat)

---

## Phase 3: Navigation

### 3.1 Add Upgrade destination
**File**: `app/src/main/.../navigation/Nav.kt`

Add inside `sealed interface Main`:
```kotlin
@Serializable
data object Upgrade : Main
```

---

## Phase 4: GPlay Upgrade Screen

### 4.1 UpgradeEvents
**New file**: `app/src/gplay/java/eu/darken/myperm/upgrade/ui/UpgradeEvents.kt`
- `sealed class UpgradeEvents` with `data object RestoreFailed`

### 4.2 UpgradeViewModel (GPlay)
**New file**: `app/src/gplay/java/eu/darken/myperm/upgrade/ui/UpgradeViewModel.kt`
- Extends `ViewModel4(dispatcherProvider)` — uses `navUp()`, `navTo()` pattern
- Injects `UpgradeRepoGplay` directly (concrete, not interface)
- Auto-navigates up on upgrade transition: track initial `isPro` state on screen open, only `navUp()` on `false → true` transition (not if already pro when screen opens)
- State: `data class State(iapState: Iap, subState: Sub, trialState: Trial)` with availability + formattedPrice
- `state` flow: combines IAP query + Sub query + upgradeInfo (5s timeout per query, same as bluemusic)
- Methods: `onGoIap(activity)`, `onGoSubscription(activity)`, `onGoSubscriptionTrial(activity)`, `restorePurchase()`

### 4.3 UpgradeScreen (GPlay)
**New file**: `app/src/gplay/java/eu/darken/myperm/upgrade/ui/UpgradeScreen.kt`
- `UpgradeScreenHost()`: hiltViewModel, ErrorEventHandler, NavigationEventHandler, RestoreFailedDialog
- `UpgradeScreen()`: Scroll-based animations (toolbar alpha, content fade), app icon (via `PermPilotIcon`), colored title, preamble card, why/how sections, purchase buttons (trial > subscription > IAP) with AnimatedVisibility, restore purchase
- `RestoreFailedDialog()`: AlertDialog with troubleshooting tips
- Preview composable

### 4.4 UpgradeNavigation (GPlay)
**New file**: `app/src/gplay/java/eu/darken/myperm/upgrade/ui/UpgradeNavigation.kt`
- `NavigationEntry` + `@Binds @IntoSet` Hilt module (same pattern as `GeneralSettingsNavigation`)

---

## Phase 5: FOSS Upgrade Screen

### 5.1 UpgradeViewModel (FOSS)
**New file**: `app/src/foss/java/eu/darken/myperm/upgrade/ui/UpgradeViewModel.kt`
- Extends `ViewModel4`, injects `UpgradeControlFoss`
- `openSponsor()`: calls `launchGithubSponsorsUpgrade()`, waits for isPro, calls `navUp()`

### 5.2 UpgradeScreen (FOSS)
**New file**: `app/src/foss/java/eu/darken/myperm/upgrade/ui/UpgradeScreen.kt`
- Simpler layout: CenterAlignedTopAppBar, app icon (96dp), title, preamble card (primaryContainer), how/why sections, single "Sponsor development" button with hint text
- Preview composable

### 5.3 UpgradeNavigation (FOSS)
**New file**: `app/src/foss/java/eu/darken/myperm/upgrade/ui/UpgradeNavigation.kt`
- Same NavigationEntry + Hilt pattern

---

## Phase 6: Update Entry Points

### 6.1 MainActivityVM + MainScreen + MainActivity
**Files**: `MainActivityVM.kt`, `MainScreen.kt`, `MainActivity.kt`
- Change `upgradeNag: SingleEventFlow<(Activity) -> Unit>` → `SingleEventFlow<Unit>`
- Instead of emitting a lambda that calls launchBillingFlow, emit Unit
- MainScreen: collect `upgradeNag` in `LaunchedEffect(Unit) { flow.collect { navCtrl.goTo(Nav.Main.Upgrade) } }` (not `collectAsState` which would deduplicate repeated emissions)
- Remove Activity dependency from the nag flow

### 6.2 GeneralSettingsViewModel + Screen
**Files**: `GeneralSettingsViewModel.kt`, `GeneralSettingsScreen.kt`
- VM: Change `onUpgrade(activity: Activity)` → `onUpgrade()` which calls `navTo(Nav.Main.Upgrade)`
- Screen: Add `NavigationEventHandler(vm)`, remove `Activity` dependency from onUpgrade callback

### 6.3 OverviewViewModel
**File**: `OverviewViewModel.kt`
- Change `onUpgrade()` from emitting `NavEvent.Finish` to `navTo(Nav.Main.Upgrade)`

---

## Phase 7: String Resources

### 7.1 GPlay strings
**File**: `app/src/gplay/res/values/strings.xml`

Add: upgrade_screen_title, upgrade_screen_preamble, upgrade_screen_why_title, upgrade_screen_why_body, upgrade_screen_how_title, upgrade_screen_how_body, upgrade_screen_subscription_trial_action, upgrade_screen_subscription_action, upgrade_screen_subscription_action_hint, upgrade_screen_iap_action, upgrade_screen_iap_action_hint, upgrade_screen_restore_purchase_action, upgrade_screen_restore_purchase_message, upgrade_screen_restore_troubleshooting_msg, upgrade_screen_restore_sync_patience_hint, upgrade_screen_restore_multiaccount_hint

### 7.2 FOSS strings
**File**: `app/src/foss/res/values/strings.xml`

Add: upgrade_screen_title, upgrade_screen_preamble, upgrade_screen_how_title, upgrade_screen_how_body, upgrade_screen_why_title, upgrade_screen_why_body, upgrade_screen_sponsor_action, upgrade_screen_sponsor_action_hint

### 7.3 Main strings cleanup
**File**: `app/src/main/res/values/strings.xml`

Existing `upgrade_myperm_label`, `upgrade_myperm_description`, `upgrade_buy_pro_action`, `upgrade_donate_action` can be removed (they were for the old dialog). Keep `upgrade_required_subtitle` (used by pro-gate UI).

---

## Phase 8: App Icon Composable

**New file**: `app/src/main/.../compose/PermPilotIcon.kt`

Simple composable using `Image(painterResource(R.mipmap.ic_launcher))` with configurable size. Used by both flavor screens.

---

## Implementation Order

1. Sku + SkuDetails + delete AvailableSku (Phase 1.1)
2. MyPermSku refactor (Phase 1.2)
3. BillingClientConnection (Phase 1.3)
4. BillingClientConnectionProvider (Phase 1.4)
5. BillingDataRepo + PurchasedSku/BillingData (Phase 1.5-1.6)
6. **BUILD CHECK** (gplay debug)
7. UpgradeRepo interface + UpgradeRepoGplay + UpgradeControlFoss (Phase 2)
8. Nav.kt + PermPilotIcon (Phase 3 + 8)
9. String resources (Phase 7)
10. GPlay screen + VM + nav (Phase 4)
11. FOSS screen + VM + nav (Phase 5)
12. Entry point updates (Phase 6)
13. **BUILD CHECK** (both flavors)

---

## Verification

1. `./gradlew assembleGplayDebug` — GPlay variant builds
2. `./gradlew assembleFossDebug` — FOSS variant builds
3. Manual test (GPlay): tap pro-gated setting → navigates to upgrade screen → shows IAP/sub/trial buttons → tap purchase → Google billing flow launches → on success, auto-navigates back
4. Manual test (FOSS): tap pro-gated setting → navigates to upgrade screen → shows sponsor button → tap → opens GitHub Sponsors + grants pro → auto-navigates back
5. Manual test: launch count nag (every 8 launches) → navigates to upgrade screen instead of showing dialog
6. Verify existing `isPro` checks still work (theme settings gate, overview badge)
7. Edge cases: cancel purchase flow (screen stays open), already-owned IAP (button hidden), Play unavailable (error shown gracefully), restore purchase for non-purchased user (RestoreFailedDialog shown), subscription offer missing from Play Console (button disabled, not crashed)
