package eu.darken.myperm.main.ui.overview

internal object OverviewPreviewData {

    fun loadedState() = OverviewViewModel.State(
        deviceInfo = OverviewViewModel.DeviceInfo(
            deviceName = "lynx (Pixel 7a)",
            androidVersion = "Android 15 [36]",
            patchLevel = "2026-01-05",
        ),
        summaryInfo = OverviewViewModel.SummaryInfo(
            activeProfileUser = 9,
            activeProfileSystem = 339,
            otherProfileUser = 0,
            otherProfileSystem = 1,
            sideloaded = 5,
            installerAppsUser = 0,
            installerAppsSystem = 1,
            systemAlertWindowUser = 0,
            systemAlertWindowSystem = 5,
            noInternetUser = 2,
            noInternetSystem = 1,
            clonesUser = 0,
            clonesSystem = 0,
            sharedIdsUser = 0,
            sharedIdsSystem = 39,
        ),
        versionDesc = "v1.2.3 (42) ~ abc1234/foss/debug",
        isLoading = false,
    )

    fun loadingState() = OverviewViewModel.State(
        versionDesc = "v1.2.3 (42) ~ abc1234/foss/debug",
        isLoading = true,
    )
}
