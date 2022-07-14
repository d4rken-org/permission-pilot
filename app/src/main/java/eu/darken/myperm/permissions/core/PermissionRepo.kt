package eu.darken.myperm.permissions.core

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.types.BaseApp
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.shareLatest
import eu.darken.myperm.permissions.core.types.BasePermission
import eu.darken.myperm.permissions.core.types.NormalPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    @AppScope private val appScope: CoroutineScope,
    private val packageManager: PackageManager,
    private val appRepo: AppRepo,
) {

    private val refreshTrigger = MutableStateFlow(UUID.randomUUID())

    fun refresh() {
        log(TAG) { "refresh() " }
        refreshTrigger.value = UUID.randomUUID()
    }

    val permissions: Flow<Set<BasePermission>> = combine(
        appRepo.packages,
        refreshTrigger
    ) { apps, _ ->
        val permissions = mutableSetOf<PermissionInfo>()

        val groupList = packageManager.getAllPermissionGroups(0) + listOf(null)

        groupList.forEach { permissionGroup ->
            val name = permissionGroup?.name
            log(TAG, VERBOSE) { "Querying permission group $name" }
            try {
                permissions.addAll(packageManager.queryPermissionsByGroup(name, 0))
            } catch (e: PackageManager.NameNotFoundException) {
                log(TAG) { "Failed to retrieve permission group $permissionGroup: $e" }
            }
        }

        val mappedPermissions = mutableListOf<BasePermission>()

        mappedPermissions.addAll(permissions.map { it.toNormalPermission(apps) })

        mappedPermissions.toSet()
    }.shareLatest(TAG, appScope)


    private fun PermissionInfo.toNormalPermission(apps: Collection<BaseApp>): NormalPermission = NormalPermission(
        permission = this,
        label = loadLabel(packageManager).toString(),
        description = (loadDescription(packageManager) ?: nonLocalizedDescription)?.toString(),
        requestingApps = apps.filter { it.requestsPermission(name) }
    )

    companion object {
        private val TAG = logTag("Permissions", "Repo")
    }
}