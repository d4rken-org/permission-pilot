package eu.darken.myperm.apps.core

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.types.BaseApp
import eu.darken.myperm.apps.core.types.NormalApp
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.shareLatest
import eu.darken.myperm.common.hasApiLevel
import eu.darken.myperm.permissions.core.PermissionId
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
//                .filter { it.packageName == "eu.thedarken.sdm" }
        packageInfos.map { it.toDefaultApp() }.toSet()
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

            BaseApp.UsesPermission(
                id = PermissionId(permissionId),
                flags = flags,
            )
        } ?: emptyList()
        return NormalApp(
            packageInfo = this,
            label = applicationInfo?.loadLabel(packageManager)?.toString(),
            requestedPermissions = requestedPerms,
            declaredPermissions = permissions?.toSet() ?: emptyList()
        )
    }

    companion object {
        private val TAG = logTag("App", "Repo")
    }
}