package eu.darken.myperm.main.ui.overview

import android.annotation.SuppressLint
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.features.BatteryOptimization
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.apps.ui.list.AppsFilterOptions
import eu.darken.myperm.common.AndroidVersionCodes
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.BuildWrap
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.room.entity.PkgType
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.permissions.core.known.APerm
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
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
    private val appRepo: AppRepo,
    private val upgradeRepo: UpgradeRepo,
    private val generalSettings: GeneralSettings,
) : ViewModel4(dispatcherProvider = dispatcherProvider) {

    data class DeviceInfo(
        val deviceName: String,
        val androidVersion: String,
        val patchLevel: String,
    )

    enum class SummarySection {
        PROFILE, INSTALL_SOURCE, PRIVACY, SECURITY, MANIFEST, SYSTEM
    }

    enum class SummaryCategory(val section: SummarySection) {
        ACTIVE_PROFILE(SummarySection.PROFILE),
        OTHER_PROFILES(SummarySection.PROFILE),
        CLONES(SummarySection.PROFILE),
        GOOGLE_PLAY(SummarySection.INSTALL_SOURCE),
        OEM_STORE(SummarySection.INSTALL_SOURCE),
        MANUALLY_INSTALLED(SummarySection.INSTALL_SOURCE),
        CAMERA(SummarySection.PRIVACY),
        LOCATION(SummarySection.PRIVACY),
        MICROPHONE(SummarySection.PRIVACY),
        CONTACTS(SummarySection.PRIVACY),
        INSTALLERS(SummarySection.SECURITY),
        OVERLAYERS(SummarySection.SECURITY),
        MANIFEST_FLAGS(SummarySection.MANIFEST),
        NO_INTERNET(SummarySection.SYSTEM),
        SHARED_IDS(SummarySection.SYSTEM),
        BATTERY_OPT(SummarySection.SYSTEM),
        OLD_API(SummarySection.SYSTEM),
    }

    data class SummaryInfo(
        val counts: Map<SummaryCategory, PkgCount>,
    ) {
        operator fun get(category: SummaryCategory): PkgCount =
            counts.getOrElse(category) { PkgCount(0, 0) }
    }

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

    val isRefreshing: StateFlow<Boolean> = appRepo.isScanning

    private val storePkgNames = AKnownPkg.APP_STORES.map { it.id.pkgName }.toSet()
    private val oemPkgNames = AKnownPkg.OEM_STORES.map { it.id.pkgName }.toSet()
    private val googlePlayPkgName = AKnownPkg.GooglePlay.id.pkgName

    val state = combine(
        deviceData.onStart { emit(DeviceInfo("", "", "")) },
        appRepo.appData.map { appDataState ->
            val apps = (appDataState as? AppRepo.AppDataState.Ready)?.apps ?: return@map null
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

    private fun buildSummary(apps: List<AppInfo>): SummaryInfo {
        val installPackagesId = APerm.REQUEST_INSTALL_PACKAGES.id.value
        val systemAlertWindowId = APerm.SYSTEM_ALERT_WINDOW.id.value
        val cameraId = APerm.CAMERA.id.value
        val fineLocationId = APerm.ACCESS_FINE_LOCATION.id.value
        val coarseLocationId = APerm.ACCESS_COARSE_LOCATION.id.value
        val recordAudioId = APerm.RECORD_AUDIO.id.value
        val readContactsId = APerm.READ_CONTACTS.id.value
        val queryAllPackagesId = APerm.QUERY_ALL_PACKAGES.id.value

        fun countPkg(predicate: (AppInfo) -> Boolean): PkgCount {
            var user = 0
            var system = 0
            for (app in apps) {
                if (!predicate(app)) continue
                if (app.isSystemApp) system++ else user++
            }
            return PkgCount(user, system)
        }

        val counts = mapOf(
            SummaryCategory.ACTIVE_PROFILE to countPkg { it.pkgType == PkgType.PRIMARY },
            SummaryCategory.OTHER_PROFILES to countPkg {
                it.pkgType == PkgType.SECONDARY_PROFILE || it.pkgType == PkgType.SECONDARY_USER
            },
            SummaryCategory.CLONES to countPkg { it.twinCount > 0 },
            SummaryCategory.GOOGLE_PLAY to countPkg {
                !it.isSystemApp && it.allInstallerPkgNames.any { pkg -> pkg == googlePlayPkgName }
            },
            SummaryCategory.OEM_STORE to countPkg {
                !it.isSystemApp && it.allInstallerPkgNames.any { pkg -> pkg in oemPkgNames }
            },
            SummaryCategory.MANUALLY_INSTALLED to countPkg {
                !it.isSystemApp && it.allInstallerPkgNames.none { pkg -> pkg in storePkgNames }
            },
            SummaryCategory.CAMERA to countPkg { it.hasGrantedPermission(cameraId) },
            SummaryCategory.LOCATION to countPkg {
                it.hasGrantedPermission(fineLocationId) || it.hasGrantedPermission(coarseLocationId)
            },
            SummaryCategory.MICROPHONE to countPkg { it.hasGrantedPermission(recordAudioId) },
            SummaryCategory.CONTACTS to countPkg { it.hasGrantedPermission(readContactsId) },
            SummaryCategory.INSTALLERS to countPkg { it.hasGrantedPermission(installPackagesId) },
            SummaryCategory.OVERLAYERS to countPkg { it.hasGrantedPermission(systemAlertWindowId) },
            SummaryCategory.NO_INTERNET to countPkg {
                it.internetAccess != InternetAccess.DIRECT && it.internetAccess != InternetAccess.UNKNOWN
            },
            SummaryCategory.SHARED_IDS to countPkg { it.siblingCount > 0 },
            SummaryCategory.BATTERY_OPT to countPkg {
                it.batteryOptimization != BatteryOptimization.MANAGED_BY_SYSTEM
            },
            SummaryCategory.OLD_API to countPkg {
                it.apiTargetLevel != null && it.apiTargetLevel < AppsFilterOptions.OLD_API_THRESHOLD
            },
            SummaryCategory.MANIFEST_FLAGS to countPkg {
                it.hasManifestFlags == true || it.hasGrantedPermission(queryAllPackagesId)
            },
        )

        return SummaryInfo(counts)
    }

    private fun AppInfo.hasGrantedPermission(permissionId: String): Boolean =
        requestedPermissions.any { it.permissionId == permissionId && it.status.isGranted }

    fun onRefresh() {
        log(TAG) { "onRefresh()" }
        appRepo.refresh()
    }

    fun onUpgrade() {
        navTo(Nav.Main.Upgrade)
    }

    fun onCategoryClicked(filters: Set<AppsFilterOptions.Filter>) = launch {
        generalSettings.appsFilterOptions.update { AppsFilterOptions(filters) }
        navTo(Nav.Tab.Apps)
    }

    fun goToSettings() {
        navTo(Nav.Settings.Index)
    }

    companion object {
        private val TAG = logTag("Overview", "VM")
    }
}
