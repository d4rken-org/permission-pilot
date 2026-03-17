package eu.darken.myperm.main.ui.overview

import eu.darken.myperm.main.ui.overview.OverviewViewModel.SummaryCategory

internal object OverviewPreviewData {

    fun loadedState() = OverviewViewModel.State(
        deviceInfo = OverviewViewModel.DeviceInfo(
            deviceName = "lynx (Pixel 7a)",
            androidVersion = "Android 15 [36]",
            patchLevel = "2026-01-05",
        ),
        summaryInfo = OverviewViewModel.SummaryInfo(
            counts = mapOf(
                SummaryCategory.ACTIVE_PROFILE to PkgCount(9, 339),
                SummaryCategory.OTHER_PROFILES to PkgCount(0, 1),
                SummaryCategory.CLONES to PkgCount(0, 0),
                SummaryCategory.GOOGLE_PLAY to PkgCount(6, 0),
                SummaryCategory.OEM_STORE to PkgCount(1, 0),
                SummaryCategory.SIDELOADED to PkgCount(5, 0),
                SummaryCategory.CAMERA to PkgCount(4, 2),
                SummaryCategory.LOCATION to PkgCount(7, 5),
                SummaryCategory.MICROPHONE to PkgCount(3, 1),
                SummaryCategory.CONTACTS to PkgCount(5, 3),
                SummaryCategory.INSTALLERS to PkgCount(0, 1),
                SummaryCategory.OVERLAYERS to PkgCount(0, 5),
                SummaryCategory.NO_INTERNET to PkgCount(2, 1),
                SummaryCategory.SHARED_IDS to PkgCount(0, 39),
                SummaryCategory.BATTERY_OPT to PkgCount(3, 12),
                SummaryCategory.OLD_API to PkgCount(1, 8),
            ),
        ),
        versionDesc = "v1.2.3 (42) ~ abc1234/foss/debug",
        isLoading = false,
    )

    fun loadingState() = OverviewViewModel.State(
        versionDesc = "v1.2.3 (42) ~ abc1234/foss/debug",
        isLoading = true,
    )
}
