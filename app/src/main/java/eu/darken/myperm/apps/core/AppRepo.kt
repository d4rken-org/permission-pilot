package eu.darken.myperm.apps.core

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserHandle
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.container.NormalApp
import eu.darken.myperm.apps.core.container.WorkProfileApp
import eu.darken.myperm.apps.core.container.getTwinApps
import eu.darken.myperm.apps.core.features.ApkPkg
import eu.darken.myperm.apps.core.features.getInstallerInfo
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.shareLatest
import eu.darken.myperm.common.hasApiLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager,
    @AppScope private val appScope: CoroutineScope,
    appEventListener: AppEventListener,
) {

    private val refreshTrigger = MutableStateFlow(UUID.randomUUID())

    fun refresh() {
        log(TAG) { "refresh() " }
        refreshTrigger.value = UUID.randomUUID()
    }

    val apps: Flow<Set<ApkPkg>> = combine(
        refreshTrigger,
        appEventListener.events.onStart { emit(AppEventListener.Event.PackageInstalled(AKnownPkg.AndroidSystem.id)) },
    ) { _, _ ->
        val packageInfos = retrievePackageInfo()

        val twinApps = context.getTwinApps()
        val allApps = packageInfos.map { it.toDefaultApp(twinApps) }.toSet<ApkPkg>()

        allApps.filterIsInstance<NormalApp>().forEach { app ->
            app.siblings = allApps
                .filter { it.sharedUserId != null }
                .filter { app.packageName != it.id.value && it.sharedUserId == app.sharedUserId }
                .toSet()
        }

        allApps
    }.shareLatest(scope = appScope, started = SharingStarted.Lazily)

    private suspend fun retrievePackageInfo(): Collection<PackageInfo> {
        log(TAG) { "retrievePackageInfo()" }

        val flags = PackageManager.GET_PERMISSIONS
        val uninstalledFlag = if (hasApiLevel(Build.VERSION_CODES.N)) PackageManager.MATCH_UNINSTALLED_PACKAGES
        else PackageManager.GET_UNINSTALLED_PACKAGES
        val packageInfos = packageManager.getInstalledPackages(flags or uninstalledFlag)
        log(TAG) { "Retrieved ${packageInfos.size} packageInfos" }

        return packageInfos
    }

    private fun PackageInfo.toDefaultApp(
        twinApps: Map<UserHandle, List<WorkProfileApp>>
    ): NormalApp {
        val twins = twinApps.entries
            .map { es -> es.value.filter { it.id.value == packageName } }
            .flatten()
        return NormalApp(
            packageInfo = this,
            installerInfo = getInstallerInfo(packageManager),
            twins = twins
        )
    }

    companion object {
        private val TAG = logTag("App", "Repo")
    }
}