package eu.darken.myperm.apps.ui.details

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.permissions.core.Permission
import java.time.Instant

internal object AppDetailsPreviewData {

    private fun permItem(
        permName: String,
        label: String? = null,
        status: UsesPermission.Status = UsesPermission.Status.GRANTED,
        isRuntime: Boolean = false,
        isSpecialAccess: Boolean = false,
        isDeclaredByApp: Boolean = false,
    ) = AppDetailsViewModel.PermItem(
        permId = Permission.Id(permName),
        permLabel = label,
        usesPermission = UsesPermission.WithState(Permission.Id(permName), flags = null, overrideStatus = status),
        status = status,
        type = "declared",
        isRuntime = isRuntime,
        isSpecialAccess = isSpecialAccess,
        isDeclaredByApp = isDeclaredByApp,
    )

    fun loadedState() = AppDetailsViewModel.State(
        label = "Chrome",
        packageName = "com.google.chrome",
        pkg = Pkg.Container(Pkg.Id("com.google.chrome")),
        isSystemApp = false,
        versionName = "120.0.6099.43",
        versionCode = 612009943,
        grantedCount = 8,
        totalPermCount = 12,
        installedAt = Instant.ofEpochMilli(1700000000000),
        updatedAt = Instant.ofEpochMilli(1705000000000),
        apiTargetDesc = "Target: Android 14 (Upside Down Cake) [34]",
        apiMinimumDesc = "Min: Android 10 (Q) [29]",
        apiCompileDesc = "Compile: Android 14 (Upside Down Cake) [34]",
        installerLabel = "Google Play Store",
        installerSourceLabel = "Installed by:",
        canOpen = true,
        installerPkgNames = listOf("com.android.vending"),
        installerAppName = "Google Play Store",
        permissions = listOf(
            permItem("android.permission.CAMERA", "Camera", UsesPermission.Status.GRANTED, isRuntime = true),
            permItem("android.permission.ACCESS_FINE_LOCATION", "Fine location", UsesPermission.Status.DENIED, isRuntime = true),
            permItem("android.permission.INTERNET", "Internet", UsesPermission.Status.GRANTED),
            permItem("android.permission.READ_CONTACTS", "Read contacts", UsesPermission.Status.GRANTED, isRuntime = true),
            permItem("android.permission.SYSTEM_ALERT_WINDOW", "Draw over other apps", UsesPermission.Status.DENIED, isSpecialAccess = true),
            permItem("com.google.chrome.DYNAMIC_RECEIVER", null, UsesPermission.Status.UNKNOWN, isDeclaredByApp = true),
        ),
        twins = listOf(
            AppDetailsViewModel.TwinItem(Pkg.Id("com.google.chrome"), "Chrome (Work)"),
        ),
        siblings = listOf(
            AppDetailsViewModel.SiblingItem(Pkg.Id("com.google.android.webview"), "Android System WebView"),
        ),
        isLoading = false,
    )

    fun loadingState() = AppDetailsViewModel.State(
        label = "Chrome",
        isLoading = true,
    )

    fun systemAppState() = AppDetailsViewModel.State(
        label = "System UI",
        packageName = "com.android.systemui",
        pkg = Pkg.Container(Pkg.Id("com.android.systemui")),
        isSystemApp = true,
        versionName = "14",
        versionCode = 34,
        grantedCount = 42,
        totalPermCount = 42,
        apiTargetDesc = "Target: Android 14 (Upside Down Cake) [34]",
        canOpen = false,
        permissions = listOf(
            permItem("android.permission.INTERNET", "Internet", UsesPermission.Status.GRANTED),
            permItem("android.permission.READ_PHONE_STATE", "Read phone state", UsesPermission.Status.GRANTED, isRuntime = true),
        ),
        isLoading = false,
    )

    fun emptyFilterState() = AppDetailsViewModel.State(
        label = "Chrome",
        packageName = "com.google.chrome",
        pkg = Pkg.Container(Pkg.Id("com.google.chrome")),
        grantedCount = 8,
        totalPermCount = 12,
        permissions = emptyList(),
        isLoading = false,
    )
}
