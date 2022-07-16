package eu.darken.myperm.permissions.core

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.types.BaseApp
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.shareLatest
import eu.darken.myperm.permissions.core.types.BasePermission
import eu.darken.myperm.permissions.core.types.DeclaredPermission
import eu.darken.myperm.permissions.core.types.UnknownPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
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
        appRepo.apps,
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

        val mappedPermissions = mutableSetOf<BasePermission>()

        // All we know from the system
        permissions
            .map { it.toDeclaredPermission(apps) }
            .run { mappedPermissions.addAll(this) }

        // All that apps have declared themselves
        apps
            .map { it.declaredPermissions }
            .flatten()
            .map { it.toDeclaredPermission(apps) }
            .run { mappedPermissions.addAll(this) }

        // All that are specified by apps via `uses-permission`
        // It's possible that some of these are unused as no other app declares them.
        apps
            .map { usesPerms -> usesPerms.requestedPermissions.map { it.id }.toSet() }
            .flatten()
            .map { id ->
                val info = try {
                    packageManager.getPermissionInfo(id.value, PackageManager.GET_META_DATA)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
                when {
                    info != null -> info.toDeclaredPermission(apps)
                    else -> id.toUnusedPermission(apps)
                }
            }
            .run { mappedPermissions.addAll(this) }

        mappedPermissions
    }
        .catch {
            log(TAG, ERROR) { "Failed to generate permission data: ${it.asLog()}" }
            throw it
        }
        .shareLatest(scope = appScope, started = SharingStarted.Lazily)

    private fun PermissionInfo.toDeclaredPermission(apps: Collection<BaseApp>): DeclaredPermission = DeclaredPermission(
        permission = this,
        label = loadLabel(packageManager).toString().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        },
        description = (loadDescription(packageManager) ?: nonLocalizedDescription)?.toString(),
        requestingPkgs = apps.filter { it.requestsPermission(id) },
        declaringPkgs = apps.filter { it.declaresPermission(id) }
    )

    private fun Permission.Id.toUnusedPermission(apps: Collection<BaseApp>): UnknownPermission = UnknownPermission(
        id = this,
        requestingPkgs = apps.filter { it.requestsPermission(this) }
    )

    companion object {
        private val TAG = logTag("Permissions", "Repo")
    }
}