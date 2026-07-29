package eu.darken.myperm.common.upgrade.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.serialization.SerializationModule
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import testhelper.BaseTest
import testhelpers.TestApplication
import java.time.Instant

/**
 * The FOSS entitlement of every existing supporter is a record written by an older Permission Pilot
 * version: `{"upgradedAt":"<iso>","reason":"foss.upgrade.reason.donated"}`, living in the
 * pre-DataStore `settings_foss` SharedPreferences. Reading it goes through the REAL [FossCache]
 * here (not a raw Json round-trip), because both halves have to hold: the retained
 * SharedPreferences migration and the retained serialization schema. Adopting canonical's
 * `upgradeType` schema would decode these records as null and silently strip those supporters'
 * entitlement.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApplication::class)
class FossCacheLegacyRecordTest : BaseTest() {

    // One test method on purpose: DataStore forbids two active instances on the same file, and
    // FossCache is a @Singleton in production.
    @Test
    fun `a legacy supporter record still grants the entitlement`() = runTest {
        val context: Context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("settings_foss", Context.MODE_PRIVATE)
            .edit()
            .putString("foss.upgrade", LEGACY_RECORD)
            .commit()

        val cache = FossCache(context, SerializationModule().json())

        cache.upgrade.value().apply {
            this shouldNotBe null
            this!!.upgradedAt shouldBe LEGACY_UPGRADED_AT
            reason shouldBe FossUpgrade.Reason.DONATED
        }

        val repo = UpgradeRepoFoss(
            // backgroundScope: the repo's shareIn keeps a collector alive for the scope's lifetime.
            appScope = backgroundScope,
            fossCache = cache,
            webpageTool = mockk<WebpageTool>(relaxed = true),
        )
        repo.upgradeInfo.first().apply {
            isPro shouldBe true
            upgradedAt shouldBe LEGACY_UPGRADED_AT
            isSettled shouldBe true
        }
    }

    companion object {
        private val LEGACY_UPGRADED_AT: Instant = Instant.parse("2024-03-04T12:00:00Z")
        private const val LEGACY_RECORD =
            """{"upgradedAt":"2024-03-04T12:00:00Z","reason":"foss.upgrade.reason.donated"}"""
    }
}
