package eu.darken.myperm.watcher.ui.dashboard

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.watcher.core.WatcherEventType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import testhelper.BaseTest

class WatcherFilterOptionsTest : BaseTest() {

    private fun item(
        eventType: WatcherEventType = WatcherEventType.UPDATE,
        isSeen: Boolean = true,
        hasAddedPermissions: Boolean = false,
        hasLostPermissions: Boolean = false,
        gainedCount: Int = 0,
        lostCount: Int = 0,
    ) = WatcherReportItem(
        id = 1,
        packageName = Pkg.Name("com.example.app"),
        appLabel = "Test App",
        versionName = "1.0",
        previousVersionName = null,
        eventType = eventType,
        detectedAt = System.currentTimeMillis(),
        isSeen = isSeen,
        hasAddedPermissions = hasAddedPermissions,
        hasLostPermissions = hasLostPermissions,
        gainedCount = gainedCount,
        lostCount = lostCount,
    )

    @Test
    fun `empty filters match everything`() {
        val options = WatcherFilterOptions()
        options.matches(item()) shouldBe true
        options.matches(item(eventType = WatcherEventType.INSTALL)) shouldBe true
        options.matches(item(gainedCount = 5)) shouldBe true
    }

    @Test
    fun `HAS_GAINED_PERMISSIONS matches item with gainedCount greater than zero`() {
        val filter = WatcherFilterOptions.Filter.HAS_GAINED_PERMISSIONS
        filter.matches(item(gainedCount = 2)) shouldBe true
        filter.matches(item(gainedCount = 1)) shouldBe true
    }

    @Test
    fun `HAS_GAINED_PERMISSIONS does not match item with gainedCount zero`() {
        val filter = WatcherFilterOptions.Filter.HAS_GAINED_PERMISSIONS
        filter.matches(item(gainedCount = 0)) shouldBe false
    }

    @Test
    fun `filters within same group are OR-ed`() {
        val options = WatcherFilterOptions(
            filters = setOf(
                WatcherFilterOptions.Filter.HAS_GAINED_PERMISSIONS,
                WatcherFilterOptions.Filter.HAS_LOST_PERMISSIONS,
            )
        )

        options.matches(item(gainedCount = 1, hasLostPermissions = false)) shouldBe true
        options.matches(item(gainedCount = 0, hasLostPermissions = true)) shouldBe true
        options.matches(item(gainedCount = 0, hasLostPermissions = false)) shouldBe false
    }

    @Test
    fun `filters across groups are AND-ed`() {
        val options = WatcherFilterOptions(
            filters = setOf(
                WatcherFilterOptions.Filter.INSTALL,
                WatcherFilterOptions.Filter.HAS_GAINED_PERMISSIONS,
            )
        )

        options.matches(item(eventType = WatcherEventType.INSTALL, gainedCount = 1)) shouldBe true
        options.matches(item(eventType = WatcherEventType.INSTALL, gainedCount = 0)) shouldBe false
        options.matches(item(eventType = WatcherEventType.UPDATE, gainedCount = 1)) shouldBe false
    }
}
