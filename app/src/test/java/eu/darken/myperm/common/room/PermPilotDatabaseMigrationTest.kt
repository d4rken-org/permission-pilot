package eu.darken.myperm.common.room

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import testhelpers.TestApplication

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApplication::class)
class PermPilotDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PermPilotDatabase::class.java,
    )

    @Test
    fun `migrate 1 to 2 adds scannerVersion with default zero`() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO manifest_hints
                (pkgName, versionCode, lastUpdateTime, hasActionMainQuery, packageQueryCount, intentQueryCount, providerQueryCount, scannedAt)
                VALUES ('com.example.app', 42, 1000, 1, 3, 2, 1, 12345)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true)

        db.query("SELECT scannerVersion, hasActionMainQuery FROM manifest_hints WHERE pkgName = 'com.example.app'")
            .use { cursor ->
                cursor.moveToFirst() shouldBe true
                // Pre-migration rows must land below the current scanner version so they get re-scanned.
                cursor.getInt(0) shouldBe 0
                cursor.getInt(1) shouldBe 1
            }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
