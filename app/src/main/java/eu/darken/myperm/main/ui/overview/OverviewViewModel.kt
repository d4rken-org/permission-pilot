package eu.darken.myperm.main.ui.overview

import android.annotation.SuppressLint
import android.os.Build
import android.os.Process
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.common.AndroidVersionCodes
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.BuildWrap
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.room.snapshot.DisplayableApp
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.UpgradeRepo
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
    appRepo: AppRepo,
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

    private val myUserHandleId = Process.myUserHandle().hashCode()
    private val storePkgNames = AKnownPkg.APP_STORES.map { it.id.pkgName }.toSet()

    val state = combine(
        deviceData.onStart { emit(DeviceInfo("", "", "")) },
        appRepo.displayState.map { displayState ->
            val apps = (displayState as? AppRepo.DisplayState.Ready)?.apps ?: return@map null
            buildSummary(apps)
        }.onStart { emit(null) },
        upgradeRepo.upgradeInfo.map<UpgradeRepo.Info, UpgradeRepo.Info?> { it }.onStart { emit(null) },
    ) { device, summary, upgrade ->
        State(
            deviceInfo = device,
            summaryInfo = summary,
            upgradeInfo = upgrade,
            isLoading = summary == null,
        )
    }.asStateFlow()

    private fun buildSummary(apps: List<DisplayableApp>): SummaryInfo {
        val installPackagesId = APerm.REQUEST_INSTALL_PACKAGES.id.value
        val systemAlertWindowId = APerm.SYSTEM_ALERT_WINDOW.id.value

        return SummaryInfo(
            activeProfileUser = apps.count { it.userHandleId == myUserHandleId && !it.isSystemApp },
            activeProfileSystem = apps.count { it.userHandleId == myUserHandleId && it.isSystemApp },
            otherProfileUser = apps.count { it.userHandleId != myUserHandleId && !it.isSystemApp },
            otherProfileSystem = apps.count { it.userHandleId != myUserHandleId && it.isSystemApp },
            sideloaded = apps.count { !it.isSystemApp && it.allInstallerPkgNames.none { pkg -> pkg in storePkgNames } },
            installerAppsUser = apps.count { !it.isSystemApp && it.hasGrantedPermission(installPackagesId) },
            installerAppsSystem = apps.count { it.isSystemApp && it.hasGrantedPermission(installPackagesId) },
            systemAlertWindowUser = apps.count { !it.isSystemApp && it.hasGrantedPermission(systemAlertWindowId) },
            systemAlertWindowSystem = apps.count { it.isSystemApp && it.hasGrantedPermission(systemAlertWindowId) },
            noInternetUser = apps.count { !it.isSystemApp && it.internetAccess != "DIRECT" },
            noInternetSystem = apps.count { it.isSystemApp && it.internetAccess != "DIRECT" },
            clonesUser = apps.count { !it.isSystemApp && it.twinCount > 0 },
            clonesSystem = apps.count { it.isSystemApp && it.twinCount > 0 },
            sharedIdsUser = apps.count { !it.isSystemApp && it.siblingCount > 0 },
            sharedIdsSystem = apps.count { it.isSystemApp && it.siblingCount > 0 },
        )
    }

    private fun DisplayableApp.hasGrantedPermission(permissionId: String): Boolean =
        requestedPermissions.any {
            it.permissionId == permissionId &&
                    (it.status == UsesPermission.Status.GRANTED || it.status == UsesPermission.Status.GRANTED_IN_USE)
        }

    fun onRefresh() = launch {
        // Trigger repo refresh via AppRepo
    }

    fun onUpgrade() {
        navTo(Nav.Main.Upgrade)
    }

    fun goToSettings() {
        navTo(Nav.Settings.Index)
    }

    companion object {
        private val TAG = logTag("Overview", "VM")
    }
}
