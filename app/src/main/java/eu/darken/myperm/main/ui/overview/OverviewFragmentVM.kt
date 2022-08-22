package eu.darken.myperm.main.ui.overview

import android.annotation.SuppressLint
import android.os.Build
import android.os.Process
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.apps.core.features.getPermission
import eu.darken.myperm.apps.core.features.isGranted
import eu.darken.myperm.apps.core.features.isSideloaded
import eu.darken.myperm.common.AndroidVersionCodes
import eu.darken.myperm.common.BuildWrap
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.uix.ViewModel3
import eu.darken.myperm.main.ui.overview.items.DeviceVH
import eu.darken.myperm.main.ui.overview.items.SummaryVH
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.known.APerm
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class OverviewFragmentVM @Inject constructor(
    @Suppress("unused") private val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    packageRepo: AppRepo,
    permissionRepo: PermissionRepo,
    private val generalSettings: GeneralSettings,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    data class State(
        val items: List<OverviewAdapter.Item> = listOf(),
        val isLoading: Boolean = true,
    )

    private val deviceData: Flow<DeviceVH.Item> = combine(
        packageRepo.apps,
        permissionRepo.permissions,
    ) { apps, permissions ->
        val item: DeviceVH.Item = DeviceVH.Item.Content(
            deviceName = "${Build.DEVICE} (${Build.MODEL})",
            androidVersion = AndroidVersionCodes.current?.longFormat ?: "API ${BuildWrap.VersionWrap.SDK_INT}",
            patchLevel = Build.VERSION.SECURITY_PATCH
        )
        item
    }
        .onStart { emit(DeviceVH.Item.Loading) }

    private val summaryData: Flow<SummaryVH.Item> = combine(
        packageRepo.apps,
        permissionRepo.permissions,
    ) { apps, permissions ->
        val item: SummaryVH.Item = SummaryVH.Item.Content(
            pkgCountActiveProfile = PkgCount(
                user = apps.count { it.userHandle == Process.myUserHandle() && !it.isSystemApp },
                system = apps.count { it.userHandle == Process.myUserHandle() && it.isSystemApp },
            ),
            pkgCountOtherProfile = PkgCount(
                user = apps.count { it.userHandle != Process.myUserHandle() && !it.isSystemApp },
                system = apps.count { it.userHandle != Process.myUserHandle() && it.isSystemApp }
            ),
            pkgCountSideloaded = PkgCount(
                user = apps.count { it.isSideloaded() },
                system = 0,
            ),
            pkgCountInstallerApps = PkgCount(
                user = apps.count { !it.isSystemApp && it.getPermission(APerm.REQUEST_INSTALL_PACKAGES)?.isGranted ?: false },
                system = apps.count { it.isSystemApp && it.getPermission(APerm.REQUEST_INSTALL_PACKAGES)?.isGranted ?: false }
            ),
            pkgCountSystemAlertWindow = PkgCount(
                user = apps.count { !it.isSystemApp && it.getPermission(APerm.SYSTEM_ALERT_WINDOW)?.isGranted ?: false },
                system = apps.count { it.isSystemApp && it.getPermission(APerm.SYSTEM_ALERT_WINDOW)?.isGranted ?: false },
            ),
            pkgCountNoInternet = PkgCount(
                user = apps.count { !it.isSystemApp && it.internetAccess != InternetAccess.DIRECT },
                system = apps.count { it.isSystemApp && it.internetAccess != InternetAccess.DIRECT },
            ),
            pkgCountClones = PkgCount(
                user = apps.count { it.isSystemApp && it.twins.isNotEmpty() },
                system = apps.count { !it.isSystemApp && it.twins.isNotEmpty() }
            ),
            pkgCountSharedIds = PkgCount(
                user = apps.count { it.isSystemApp && it.siblings.isNotEmpty() },
                system = apps.count { !it.isSystemApp && it.siblings.isNotEmpty() },
            )
        )
        item
    }
        .onStart { emit(SummaryVH.Item.Loading) }

    val listData: LiveData<State> = combine(
        deviceData,
        summaryData,
    ) { device, summary ->
        State(
            items = listOf(
                device,
                summary
            ),
            isLoading = false
        )
    }
        .onStart { emit(State()) }
        .asLiveData2()

}