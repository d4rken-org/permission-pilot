package eu.darken.myperm.apps.ui.list

import android.os.Process
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.InstallerInfo

internal object AppsPreviewData {

    private fun appItem(
        pkgName: String,
        label: String?,
        isSystemApp: Boolean = false,
        grantedCount: Int = 0,
        totalCount: Int = 0,
        declaredCount: Int = 0,
        tagIconRes: List<Int> = emptyList(),
        hasInstaller: Boolean = false,
    ) = AppsViewModel.AppItem(
        id = Pkg.Id(pkgName),
        pkg = Pkg.Container(Pkg.Id(pkgName)),
        label = label,
        packageName = pkgName,
        isSystemApp = isSystemApp,
        permissionCount = totalCount,
        grantedCount = grantedCount,
        totalCount = totalCount,
        declaredCount = declaredCount,
        tagIconRes = tagIconRes,
        installerInfo = if (hasInstaller) InstallerInfo(installingPkg = Pkg.Container(Pkg.Id("com.android.vending"))) else null,
        userHandle = Process.myUserHandle(),
    )

    fun readyState() = AppsViewModel.State.Ready(
        items = listOf(
            appItem("com.google.chrome", "Chrome", grantedCount = 8, totalCount = 12, hasInstaller = true),
            appItem("org.mozilla.firefox", "Firefox", grantedCount = 5, totalCount = 9, hasInstaller = true),
            appItem("com.android.systemui", "System UI", isSystemApp = true, grantedCount = 42, totalCount = 42, declaredCount = 3),
        ),
        itemCount = 3,
    )

    fun emptyReadyState() = AppsViewModel.State.Ready(
        items = emptyList(),
        itemCount = 0,
    )
}
