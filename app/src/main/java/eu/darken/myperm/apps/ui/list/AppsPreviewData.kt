package eu.darken.myperm.apps.ui.list

import androidx.compose.ui.graphics.vector.ImageVector
import eu.darken.myperm.apps.core.Pkg

internal object AppsPreviewData {

    private fun appItem(
        pkgName: String,
        label: String,
        isSystemApp: Boolean = false,
        showPkgName: Boolean = false,
        grantedCount: Int = 0,
        totalCount: Int = 0,
        declaredCount: Int = 0,
        tagIcons: List<ImageVector> = emptyList(),
        installerPkgName: String? = null,
    ) = AppsViewModel.AppItem(
        pkgName = pkgName,
        userHandleId = 0,
        iconModel = Pkg.Container(Pkg.Id(pkgName)),
        label = label,
        isSystemApp = isSystemApp,
        showPkgName = showPkgName,
        permissionCount = totalCount,
        grantedCount = grantedCount,
        totalCount = totalCount,
        declaredCount = declaredCount,
        tagIcons = tagIcons,
        installerPkgName = installerPkgName,
    )

    fun readyState() = AppsViewModel.State.Ready(
        items = listOf(
            appItem("com.google.chrome", "Chrome", grantedCount = 8, totalCount = 12, installerPkgName = "com.android.vending"),
            appItem("org.mozilla.firefox", "Firefox", grantedCount = 22, totalCount = 32, installerPkgName = "com.android.vending", declaredCount = 2),
            appItem("com.duckduckgo.mobile.android", "DuckDuckGo", grantedCount = 15, totalCount = 22, installerPkgName = "com.android.vending", declaredCount = 1),
            appItem("com.google.android.apps.maps", "Google Maps", grantedCount = 18, totalCount = 24, installerPkgName = "com.android.vending"),
            appItem("com.spotify.music", "Spotify", grantedCount = 6, totalCount = 11, installerPkgName = "com.android.vending"),
            appItem("org.thoughtcrime.securesms", "Signal", grantedCount = 14, totalCount = 19, installerPkgName = "com.android.vending", declaredCount = 1),
            appItem("com.whatsapp", "WhatsApp", grantedCount = 20, totalCount = 26, installerPkgName = "com.android.vending", declaredCount = 3),
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
