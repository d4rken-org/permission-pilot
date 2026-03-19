package eu.darken.myperm.apps.ui.list

import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg

internal object AppsPreviewData {

    private fun appItem(
        pkgName: String,
        label: String,
        isSystemApp: Boolean = false,
        showPkgName: Boolean = false,
        installerIconPkg: String? = null,
        installerLabel: String = "Play Store",
        updatedAtFormatted: String? = "Updated Mar 2026",
        permChips: List<AppsViewModel.PermChip> = emptyList(),
    ) = AppsViewModel.AppItem(
        pkgName = Pkg.Name(pkgName),
        userHandleId = 0,
        iconModel = Pkg.Container(Pkg.Id(Pkg.Name(pkgName))),
        label = label,
        isSystemApp = isSystemApp,
        showPkgName = showPkgName,
        installerIconPkg = installerIconPkg?.let { Pkg.Name(it) },
        installerLabel = installerLabel,
        updatedAtFormatted = updatedAtFormatted,
        permChips = permChips,
    )

    private val cameraChip = AppsViewModel.PermChip(R.string.permission_group_camera_label)
    private val locationChip = AppsViewModel.PermChip(R.string.permission_group_location_label)
    private val audioChip = AppsViewModel.PermChip(R.string.permission_group_audio_label)
    private val contactsChip = AppsViewModel.PermChip(R.string.permission_group_contacts_label)
    private val callsChip = AppsViewModel.PermChip(R.string.permission_group_calls_label)
    private val filesChip = AppsViewModel.PermChip(R.string.permission_group_files_label)
    private val messagingChip = AppsViewModel.PermChip(R.string.permission_group_messaging_label)

    fun readyState() = AppsViewModel.State.Ready(
        items = listOf(
            appItem(
                "com.google.chrome", "Chrome",
                installerIconPkg = "com.android.vending",
                permChips = listOf(cameraChip, locationChip, audioChip),
            ),
            appItem(
                "org.mozilla.firefox", "Firefox",
                installerIconPkg = "com.android.vending",
                permChips = listOf(cameraChip, locationChip, audioChip, filesChip),
            ),
            appItem(
                "com.duckduckgo.mobile.android", "DuckDuckGo",
                installerIconPkg = "com.android.vending",
                permChips = listOf(cameraChip, locationChip),
            ),
            appItem(
                "com.google.android.apps.maps", "Google Maps",
                installerIconPkg = "com.android.vending",
                permChips = listOf(cameraChip, locationChip, audioChip, contactsChip),
            ),
            appItem(
                "com.spotify.music", "Spotify",
                installerIconPkg = "com.android.vending",
                permChips = listOf(audioChip),
            ),
            appItem(
                "org.thoughtcrime.securesms", "Signal",
                installerIconPkg = "com.android.vending",
                permChips = listOf(cameraChip, locationChip, audioChip, contactsChip, callsChip, messagingChip),
            ),
            appItem(
                "com.whatsapp", "WhatsApp",
                installerIconPkg = "com.android.vending",
                permChips = listOf(cameraChip, locationChip, audioChip, contactsChip, callsChip, messagingChip, filesChip),
            ),
            appItem(
                "com.android.systemui", "System UI",
                isSystemApp = true,
                installerLabel = "Pre-installed",
                updatedAtFormatted = null,
            ),
            appItem(
                "com.google.android.gms", "Google Play Services",
                isSystemApp = true,
                installerLabel = "Pre-installed",
                permChips = listOf(cameraChip, locationChip, audioChip, contactsChip, callsChip, filesChip),
            ),
        ),
        itemCount = 9,
    )

    fun emptyReadyState() = AppsViewModel.State.Ready(
        items = emptyList(),
        itemCount = 0,
    )
}
