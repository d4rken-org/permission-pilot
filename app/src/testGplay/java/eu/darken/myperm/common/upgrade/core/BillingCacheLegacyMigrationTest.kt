package eu.darken.myperm.common.upgrade.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import testhelper.BaseTest
import testhelpers.TestApplication

/**
 * Installs that predate the DataStore move keep their upgrade state in the `settings_gplay`
 * SharedPreferences file. PP retains a [androidx.datastore.preferences.SharedPreferencesMigration]
 * for exactly that reason: dropping it would lose the "this install once confirmed a real Pro
 * purchase" evidence, which is what the grace window and the debug-log header are built on.
 *
 * The legacy prefs are seeded BEFORE the cache is constructed — the migration only runs on the
 * DataStore's first read, so a store created first would never see them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApplication::class)
class BillingCacheLegacyMigrationTest : BaseTest() {

    // One test method on purpose: DataStore forbids two active instances on the same file, and
    // BillingCache is a @Singleton in production.
    @Test
    fun `legacy SharedPreferences upgrade state survives the DataStore migration`() = runTest {
        // Real time on purpose: snapshot()/stampLastProState() are bounded by cacheTimeoutMs, and
        // the real DataStore does its I/O off the test scheduler -- under virtual time the bound
        // would fire instantly while nothing else is scheduled.
        withContext(Dispatchers.IO) {
            resetBillingCacheDataStore()
            val context: Context = ApplicationProvider.getApplicationContext()
            context.getSharedPreferences("settings_gplay", Context.MODE_PRIVATE)
                .edit()
                .putLong("gplay.cache.lastProAt", LEGACY_PRO_AT)
                .putString("gplay.cache.lastProSku", OurSku.Iap.PRO_UPGRADE.id)
                .commit()

            val cache = BillingCache(context)

            cache.lastProStateAt.value() shouldBe LEGACY_PRO_AT
            cache.lastProStateSku.value() shouldBe OurSku.Iap.PRO_UPGRADE.id
            // The third key is new in this schema: legacy stores have no episode, so it must read as
            // "no unconfirmed episode" rather than as a stale grace start.
            cache.proUnconfirmedSince.value() shouldBe 0L

            cache.snapshot() shouldBe BillingCache.Snapshot(
                lastProStateAt = LEGACY_PRO_AT,
                lastProStateSku = OurSku.Iap.PRO_UPGRADE.id,
                proUnconfirmedSince = 0L,
            )

            // The migrated values are ordinary DataStore entries afterwards: the atomic stamp
            // transaction has to keep operating on them consistently.
            cache.stampLastProState(OurSku.Sub.PRO_UPGRADE.id, LEGACY_PRO_AT + 1_000L)

            cache.snapshot() shouldBe BillingCache.Snapshot(
                lastProStateAt = LEGACY_PRO_AT + 1_000L,
                lastProStateSku = OurSku.Sub.PRO_UPGRADE.id,
                proUnconfirmedSince = 0L,
            )
        }
    }

    companion object {
        private const val LEGACY_PRO_AT = 1_709_553_600_000L
    }
}
