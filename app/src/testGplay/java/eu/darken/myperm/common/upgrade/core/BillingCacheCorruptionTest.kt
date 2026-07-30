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
import java.io.File

/**
 * PP keeps a [androidx.datastore.core.handlers.ReplaceFileCorruptionHandler] on the billing cache:
 * a truncated or garbled `settings_gplay.preferences_pb` (interrupted write, storage fault) must
 * reset the store to empty instead of throwing on every single read for the rest of the install's
 * life — the upgrade screen and the debug-log header both read this store.
 *
 * The corrupt file is written BEFORE the cache is constructed — the store is only parsed on its
 * first read, so a store created first would never see it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApplication::class)
class BillingCacheCorruptionTest : BaseTest() {

    // One test method on purpose: DataStore forbids two active instances on the same file, and
    // BillingCache is a @Singleton in production.
    @Test
    fun `a corrupted preferences file resets to defaults instead of failing every read`() = runTest {
        // Real time on purpose: snapshot() is bounded by cacheTimeoutMs, and the real DataStore
        // does its I/O off the test scheduler -- under virtual time the bound would fire instantly
        // while nothing else is scheduled.
        withContext(Dispatchers.IO) {
            resetBillingCacheDataStore()
            val context: Context = ApplicationProvider.getApplicationContext()

            val storeFile = File(context.filesDir, "datastore/settings_gplay.preferences_pb")
            storeFile.parentFile!!.mkdirs()
            // Field number 0 is not a legal protobuf tag, so the preferences parser rejects this
            // outright -- the same CorruptionException a half-written file produces.
            storeFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03))

            val cache = BillingCache(context)

            // Not an exception, and not a hang: the never-bought triple. Losing the evidence is the
            // accepted cost of a corrupted file; bricking every read is not.
            cache.snapshot() shouldBe BillingCache.Snapshot(
                lastProStateAt = 0L,
                lastProStateSku = "",
                proUnconfirmedSince = 0L,
            )

            // And the store is usable again afterwards, rather than re-throwing on the next write.
            cache.stampLastProState(OurSku.Iap.PRO_UPGRADE.id, 4_242L)
            cache.snapshot() shouldBe BillingCache.Snapshot(
                lastProStateAt = 4_242L,
                lastProStateSku = OurSku.Iap.PRO_UPGRADE.id,
                proUnconfirmedSince = 0L,
            )
        }
    }
}
