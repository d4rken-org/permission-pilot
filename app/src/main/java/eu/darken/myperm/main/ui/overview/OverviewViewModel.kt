package eu.darken.myperm.main.ui.overview

import android.annotation.SuppressLint
import android.os.Build
import android.os.Process
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.apps.core.features.getPermission
import eu.darken.myperm.apps.core.features.isGranted
import eu.darken.myperm.apps.core.features.isSideloaded
import eu.darken.myperm.common.AndroidVersionCodes
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.BuildWrap
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavEvent
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.known.APerm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class OverviewViewModel @Inject constructor(
    @Suppress("unused") handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    packageRepo: AppRepo,
    permissionRepo: PermissionRepo,
    private val upgradeRepo: UpgradeRepo,
) : ViewModel4(dispatcherProvider = dispatcherProvider) {

    data class DeviceInfo(
        val deviceName: String,
        val androidVersion: String,
        val patchLevel: String,
    )

    data class SummaryInfo(
        val activeProfileUser: Int = 0,
        val activeProfileSystem: Int = 0,
        val otherProfileUser: Int = 0,
        val otherProfileSystem: Int = 0,
        val sideloaded: Int = 0,
        val installerAppsUser: Int = 0,
        val installerAppsSystem: Int = 0,
        val systemAlertWindowUser: Int = 0,
        val systemAlertWindowSystem: Int = 0,
        val noInternetUser: Int = 0,
        val noInternetSystem: Int = 0,
        val clonesUser: Int = 0,
        val clonesSystem: Int = 0,
        val sharedIdsUser: Int = 0,
        val sharedIdsSystem: Int = 0,
    )

    data class State(
        val deviceInfo: DeviceInfo? = null,
        val summaryInfo: SummaryInfo? = null,
        val upgradeInfo: UpgradeRepo.Info? = null,
        val versionDesc: String = BuildConfigWrap.VERSION_DESCRIPTION,
        val isLoading: Boolean = true,
    )

    private val deviceData: Flow<DeviceInfo> = flow {
        emit(
            DeviceInfo(
                deviceName = "${Build.DEVICE} (${Build.MODEL})",
                androidVersion = AndroidVersionCodes.current?.longFormat ?: "API ${BuildWrap.VersionWrap.SDK_INT}",
                patchLevel = Build.VERSION.SECURITY_PATCH,
            )
        )
    }

    val state: Flow<State> = combine(
        deviceData.onStart { emit(DeviceInfo("", "", "")) },
        combine(
            packageRepo.state,
            permissionRepo.state,
        ) { appRepoState, permissionRepoState ->
            val apps = (appRepoState as? AppRepo.State.Ready)?.pkgs ?: return@combine null
            val permissions = (permissionRepoState as? PermissionRepo.State.Ready)?.permissions ?: return@combine null
            if (appRepoState.id != permissionRepoState.basedOnAppState) return@combine null

            SummaryInfo(
                activeProfileUser = apps.count { it.userHandle == Process.myUserHandle() && !it.isSystemApp },
                activeProfileSystem = apps.count { it.userHandle == Process.myUserHandle() && it.isSystemApp },
                otherProfileUser = apps.count { it.userHandle != Process.myUserHandle() && !it.isSystemApp },
                otherProfileSystem = apps.count { it.userHandle != Process.myUserHandle() && it.isSystemApp },
                sideloaded = apps.count { it.isSideloaded() },
                installerAppsUser = apps.count { !it.isSystemApp && it.getPermission(APerm.REQUEST_INSTALL_PACKAGES)?.isGranted ?: false },
                installerAppsSystem = apps.count { it.isSystemApp && it.getPermission(APerm.REQUEST_INSTALL_PACKAGES)?.isGranted ?: false },
                systemAlertWindowUser = apps.count { !it.isSystemApp && it.getPermission(APerm.SYSTEM_ALERT_WINDOW)?.isGranted ?: false },
                systemAlertWindowSystem = apps.count { it.isSystemApp && it.getPermission(APerm.SYSTEM_ALERT_WINDOW)?.isGranted ?: false },
                noInternetUser = apps.count { !it.isSystemApp && it.internetAccess != InternetAccess.DIRECT },
                noInternetSystem = apps.count { it.isSystemApp && it.internetAccess != InternetAccess.DIRECT },
                clonesUser = apps.count { !it.isSystemApp && it.twins.isNotEmpty() },
                clonesSystem = apps.count { it.isSystemApp && it.twins.isNotEmpty() },
                sharedIdsUser = apps.count { !it.isSystemApp && it.siblings.isNotEmpty() },
                sharedIdsSystem = apps.count { it.isSystemApp && it.siblings.isNotEmpty() },
            )
        }.onStart { emit(null) },
        upgradeRepo.upgradeInfo.map<UpgradeRepo.Info, UpgradeRepo.Info?> { it }.onStart { emit(null) },
    ) { device, summary, upgrade ->
        State(
            deviceInfo = device,
            summaryInfo = summary,
            upgradeInfo = upgrade,
            isLoading = summary == null,
        )
    }

    fun onRefresh() = launch {
        // Trigger repo refresh via AppRepo
    }

    fun onUpgrade() = launch {
        navEvents.emit(NavEvent.Finish) // Handled in host
    }

    fun goToSettings() {
        navTo(Nav.Settings.Index)
    }

    companion object {
        private val TAG = logTag("Overview", "VM")
    }
}
