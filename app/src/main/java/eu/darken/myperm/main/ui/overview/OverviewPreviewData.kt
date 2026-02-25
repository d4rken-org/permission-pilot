package eu.darken.myperm.main.ui.overview

internal object OverviewPreviewData {

    fun loadedState() = OverviewViewModel.State(
        deviceInfo = OverviewViewModel.DeviceInfo(
            deviceName = "oriole (Pixel 6)",
            androidVersion = "Android 14 (Upside Down Cake) [34]",
            patchLevel = "2024-01-05",
        ),
        summaryInfo = OverviewViewModel.SummaryInfo(
            activeProfileUser = 87,
            activeProfileSystem = 142,
            otherProfileUser = 3,
            otherProfileSystem = 0,
            sideloaded = 5,
            installerAppsUser = 2,
            installerAppsSystem = 1,
            systemAlertWindowUser = 4,
            systemAlertWindowSystem = 3,
            noInternetUser = 12,
            noInternetSystem = 68,
            clonesUser = 1,
            clonesSystem = 0,
            sharedIdsUser = 0,
            sharedIdsSystem = 14,
        ),
        versionDesc = "v1.2.3 (42) ~ abc1234/foss/debug",
        isLoading = false,
    )

    fun loadingState() = OverviewViewModel.State(
        versionDesc = "v1.2.3 (42) ~ abc1234/foss/debug",
        isLoading = true,
    )
}
