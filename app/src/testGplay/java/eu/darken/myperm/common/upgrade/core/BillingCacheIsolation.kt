package eu.darken.myperm.common.upgrade.core

/**
 * Drops the cached DataStore behind the file-level `billingCacheDataStore` delegate.
 *
 * The delegate is a process-wide singleton (that is what makes it correct in production), but
 * Robolectric reuses ONE sandbox classloader for every test class with the same `@Config`, while
 * giving each test method a fresh `filesDir`. Without this reset the second test class to touch
 * BillingCache gets the first class's live DataStore -- still serving the first class's values and
 * still pointing at its deleted temp file. Every test here depends on a pristine FIRST access:
 * that is when the SharedPreferences migration runs and when a corrupt file is detected.
 *
 * Fails loudly rather than silently no-op'ing: a renamed field would otherwise turn the isolation
 * off and leave the tests passing on leaked state.
 */
internal fun resetBillingCacheDataStore() {
    val delegateField = Class.forName("eu.darken.myperm.common.upgrade.core.BillingCacheKt")
        .getDeclaredField("billingCacheDataStore\$delegate")
        .apply { isAccessible = true }
    val delegate = requireNotNull(delegateField.get(null)) { "billingCacheDataStore delegate is null" }
    delegate.javaClass
        .getDeclaredField("INSTANCE")
        .apply { isAccessible = true }
        .set(delegate, null)
}
