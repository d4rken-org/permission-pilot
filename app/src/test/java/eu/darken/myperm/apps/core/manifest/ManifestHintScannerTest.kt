package eu.darken.myperm.apps.core.manifest

import eu.darken.myperm.apps.core.Pkg
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import testhelper.BaseTest

class ManifestHintScannerTest : BaseTest() {

    private val scanner = ManifestHintScanner()

    @Test
    fun `empty queries produces no flags and zero counts`() {
        val flags = scanner.evaluate(QueriesInfo())

        flags.hasActionMainQuery shouldBe false
        flags.packageQueryCount shouldBe 0
        flags.intentQueryCount shouldBe 0
        flags.providerQueryCount shouldBe 0
    }

    @Test
    fun `ACTION_MAIN in intent query is detected`() {
        val info = QueriesInfo(
            intentQueries = listOf(
                QueriesInfo.IntentQuery(actions = listOf("android.intent.action.MAIN")),
            ),
        )

        val flags = scanner.evaluate(info)

        flags.hasActionMainQuery shouldBe true
        flags.intentQueryCount shouldBe 1
    }

    @Test
    fun `ACTION_MAIN detection is case sensitive`() {
        val info = QueriesInfo(
            intentQueries = listOf(
                QueriesInfo.IntentQuery(actions = listOf("android.intent.action.main")),
            ),
        )

        val flags = scanner.evaluate(info)

        flags.hasActionMainQuery shouldBe false
    }

    @Test
    fun `ACTION_MAIN among multiple actions is detected`() {
        val info = QueriesInfo(
            intentQueries = listOf(
                QueriesInfo.IntentQuery(
                    actions = listOf("android.intent.action.VIEW", "android.intent.action.MAIN"),
                ),
            ),
        )

        val flags = scanner.evaluate(info)

        flags.hasActionMainQuery shouldBe true
    }

    @Test
    fun `ACTION_MAIN in second intent query is detected`() {
        val info = QueriesInfo(
            intentQueries = listOf(
                QueriesInfo.IntentQuery(actions = listOf("android.intent.action.VIEW")),
                QueriesInfo.IntentQuery(actions = listOf("android.intent.action.MAIN")),
            ),
        )

        val flags = scanner.evaluate(info)

        flags.hasActionMainQuery shouldBe true
        flags.intentQueryCount shouldBe 2
    }

    @Test
    fun `ACTION_MAIN with LAUNCHER category is detected`() {
        val info = QueriesInfo(
            intentQueries = listOf(
                QueriesInfo.IntentQuery(
                    actions = listOf("android.intent.action.MAIN"),
                    categories = listOf("android.intent.category.LAUNCHER"),
                ),
            ),
        )

        scanner.evaluate(info).hasActionMainQuery shouldBe true
    }

    @Test
    fun `ACTION_MAIN with non-launcher category is not flagged`() {
        // Discord ships MAIN + APP_CONTACTS — that only matches contacts apps, not "most apps".
        val info = QueriesInfo(
            intentQueries = listOf(
                QueriesInfo.IntentQuery(
                    actions = listOf("android.intent.action.MAIN"),
                    categories = listOf("android.intent.category.APP_CONTACTS"),
                ),
            ),
        )

        scanner.evaluate(info).hasActionMainQuery shouldBe false
    }

    @Test
    fun `ACTION_MAIN with LAUNCHER plus extra category is not flagged`() {
        val info = QueriesInfo(
            intentQueries = listOf(
                QueriesInfo.IntentQuery(
                    actions = listOf("android.intent.action.MAIN"),
                    categories = listOf(
                        "android.intent.category.LAUNCHER",
                        "android.intent.category.DEFAULT",
                    ),
                ),
            ),
        )

        scanner.evaluate(info).hasActionMainQuery shouldBe false
    }

    @Test
    fun `ACTION_MAIN with data spec is not flagged`() {
        val info = QueriesInfo(
            intentQueries = listOf(
                QueriesInfo.IntentQuery(
                    actions = listOf("android.intent.action.MAIN"),
                    dataSpecs = listOf("mimeType=image/jpeg"),
                ),
            ),
        )

        scanner.evaluate(info).hasActionMainQuery shouldBe false
    }

    @Test
    fun `ACTION_MAIN with host-only data spec is still flagged`() {
        // The platform only builds the query's data URI when a scheme or mimeType is present —
        // a host alone is dropped and the query still matches data-less launcher filters.
        val info = QueriesInfo(
            intentQueries = listOf(
                QueriesInfo.IntentQuery(
                    actions = listOf("android.intent.action.MAIN"),
                    dataSpecs = listOf("host=example.com"),
                ),
            ),
        )

        scanner.evaluate(info).hasActionMainQuery shouldBe true
    }

    @Test
    fun `wildcard action is detected`() {
        val info = QueriesInfo(
            intentQueries = listOf(
                QueriesInfo.IntentQuery(actions = listOf("*")),
            ),
        )

        scanner.evaluate(info).hasActionMainQuery shouldBe true
    }

    @Test
    fun `wildcard action with data spec is not flagged`() {
        val info = QueriesInfo(
            intentQueries = listOf(
                QueriesInfo.IntentQuery(
                    actions = listOf("*"),
                    dataSpecs = listOf("scheme=https"),
                ),
            ),
        )

        scanner.evaluate(info).hasActionMainQuery shouldBe false
    }

    @Test
    fun `non-ACTION_MAIN intents do not trigger flag`() {
        val info = QueriesInfo(
            intentQueries = listOf(
                QueriesInfo.IntentQuery(actions = listOf("android.intent.action.VIEW")),
                QueriesInfo.IntentQuery(actions = listOf("android.intent.action.SEND")),
            ),
        )

        val flags = scanner.evaluate(info)

        flags.hasActionMainQuery shouldBe false
    }

    @Test
    fun `counts are computed correctly for all query types`() {
        val info = QueriesInfo(
            packageQueries = listOf("a", "b", "c"),
            intentQueries = listOf(QueriesInfo.IntentQuery(), QueriesInfo.IntentQuery()),
            providerQueries = listOf("p"),
        )

        val flags = scanner.evaluate(info)

        flags.packageQueryCount shouldBe 3
        flags.intentQueryCount shouldBe 2
        flags.providerQueryCount shouldBe 1
    }

    @Test
    fun `hasExcessiveQueries at threshold is false`() {
        val entity = makeEntity(totalPackages = 10, totalIntents = 0, totalProviders = 0)

        ManifestHintScanner.hasExcessiveQueries(entity) shouldBe false
    }

    @Test
    fun `hasExcessiveQueries above threshold is true`() {
        val entity = makeEntity(totalPackages = 11, totalIntents = 0, totalProviders = 0)

        ManifestHintScanner.hasExcessiveQueries(entity) shouldBe true
    }

    @Test
    fun `hasExcessiveQueries only counts package queries`() {
        val entity = makeEntity(totalPackages = 4, totalIntents = 4, totalProviders = 4)

        ManifestHintScanner.hasExcessiveQueries(entity) shouldBe false
    }

    @Test
    fun `hasExcessiveQueries ignores intent and provider counts`() {
        val entity = makeEntity(totalPackages = 5, totalIntents = 50, totalProviders = 50)

        ManifestHintScanner.hasExcessiveQueries(entity) shouldBe false
    }

    @Test
    fun `hasFlaggedIssues returns true for ACTION_MAIN only`() {
        val entity = makeEntity(totalPackages = 0, totalIntents = 0, totalProviders = 0, actionMain = true)

        ManifestHintScanner.hasFlaggedIssues(entity) shouldBe true
    }

    @Test
    fun `hasFlaggedIssues returns true for excessive only`() {
        val entity = makeEntity(totalPackages = 15, totalIntents = 0, totalProviders = 0)

        ManifestHintScanner.hasFlaggedIssues(entity) shouldBe true
    }

    @Test
    fun `hasFlaggedIssues returns false when clean`() {
        val entity = makeEntity(totalPackages = 2, totalIntents = 1, totalProviders = 0)

        ManifestHintScanner.hasFlaggedIssues(entity) shouldBe false
    }

    private fun makeEntity(
        totalPackages: Int = 0,
        totalIntents: Int = 0,
        totalProviders: Int = 0,
        actionMain: Boolean = false,
    ) = ManifestHintEntity(
        pkgName = Pkg.Name("com.test"),
        versionCode = 1,
        lastUpdateTime = 1000,
        hasActionMainQuery = actionMain,
        packageQueryCount = totalPackages,
        intentQueryCount = totalIntents,
        providerQueryCount = totalProviders,
        scannedAt = System.currentTimeMillis(),
    )
}
