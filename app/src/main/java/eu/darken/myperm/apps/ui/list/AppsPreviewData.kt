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
            appItem("org.mozilla.firefox", "Firefox", grantedCount = 22, totalCount = 32, hasInstaller = true, declaredCount = 2),
            appItem("com.duckduckgo.mobile.android", "DuckDuckGo", grantedCount = 15, totalCount = 22, hasInstaller = true, declaredCount = 1),
            appItem("com.google.android.apps.maps", "Google Maps", grantedCount = 18, totalCount = 24, hasInstaller = true),
            appItem("com.spotify.music", "Spotify", grantedCount = 6, totalCount = 11, hasInstaller = true),
            appItem("org.thoughtcrime.securesms", "Signal", grantedCount = 14, totalCount = 19, hasInstaller = true, declaredCount = 1),
            appItem("com.whatsapp", "WhatsApp", grantedCount = 20, totalCount = 26, hasInstaller = true, declaredCount = 3),
            appItem("com.android.systemui", "System UI", isSystemApp = true, grantedCount = 42, totalCount = 42, declaredCount = 3),
            appItem("com.google.android.gms", "Google Play Services", isSystemApp = true, grantedCount = 67, totalCount = 67, declaredCount = 12),
        ),
        itemCount = 9,
    )

    fun emptyReadyState() = AppsViewModel.State.Ready(
        items = emptyList(),
        itemCount = 0,
    )
}
