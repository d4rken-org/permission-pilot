package eu.darken.myperm.apps.core

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.installer.getInstallerInfo
import eu.darken.myperm.apps.core.types.BaseApp
import eu.darken.myperm.apps.core.types.NormalApp
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.shareLatest
import eu.darken.myperm.common.hasApiLevel
import eu.darken.myperm.permissions.core.Permission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager,
    @AppScope private val appScope: CoroutineScope
) {

    private val refreshTrigger = MutableStateFlow(UUID.randomUUID())

    fun refresh() {
        log(TAG) { "refresh() " }
        refreshTrigger.value = UUID.randomUUID()
    }

    val apps: Flow<Set<BaseApp>> = refreshTrigger.mapLatest {
        val packageInfos = retrievePackageInfo()

        val allApps = packageInfos.map { it.toDefaultApp() }.toSet<BaseApp>()

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

    private fun PackageInfo.toDefaultApp(): NormalApp {
        val requestedPerms = requestedPermissions?.mapIndexed { index, permissionId ->
            val flags = requestedPermissionsFlags[index]

            UsesPermission(
                id = Permission.Id(permissionId),
                flags = flags,
            )
        } ?: emptyList()

        return NormalApp(
            packageInfo = this,
            label = applicationInfo?.loadLabel(packageManager)?.toString()?.takeIf { it != packageName },
            requestedPermissions = requestedPerms,
            declaredPermissions = permissions?.toSet() ?: emptyList(),
            installerInfo = getInstallerInfo(packageManager)
        )
    }

    companion object {
        private val TAG = logTag("App", "Repo")
    }
}