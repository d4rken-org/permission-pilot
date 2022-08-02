package eu.darken.myperm.permissions.core

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.apps.core.features.declaresPermission
import eu.darken.myperm.apps.core.features.requestsPermission
import eu.darken.myperm.apps.core.getPermissionInfo2
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.shareLatest
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.core.container.DeclaredPermission
import eu.darken.myperm.permissions.core.container.ExtraPermission
import eu.darken.myperm.permissions.core.container.UnknownPermission
import eu.darken.myperm.permissions.core.features.InstallTimeGrant
import eu.darken.myperm.permissions.core.features.PermissionTag
import eu.darken.myperm.permissions.core.features.RuntimeGrant
import eu.darken.myperm.permissions.core.features.SpecialAccess
import eu.darken.myperm.permissions.core.known.AExtraPerm
import eu.darken.myperm.permissions.core.known.APerm
import eu.darken.myperm.permissions.core.known.APermGrp
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

    val permissions: Flow<Collection<BasePermission>> = combine(
        appRepo.apps,
        refreshTrigger
    ) { apps, _ ->

        val fromAosp = (packageManager.getAllPermissionGroups(0) + listOf(null))
            .mapNotNull { permissionGroup ->
                val name = permissionGroup?.name
                log(TAG, VERBOSE) { "Querying permission group $name" }
                try {
                    packageManager.queryPermissionsByGroup(name, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    log(TAG) { "Failed to retrieve permission group $permissionGroup: $e" }
                    null
                }
            }
            .flatten()

        val appWithPermissions = apps.toList()

        val mappedPermissions = mutableSetOf<BasePermission>()

        // All we know from the system
        fromAosp
            .map { it.toDeclaredPermission(appWithPermissions) }
            .distinctBy { it.id }
            .let {
                log(TAG) { "${it.size} permissions from AOSP" }
                mappedPermissions.addAll(it)
            }


        // All that apps have declared themselves
        appWithPermissions
            .asSequence()
            .map { it.declaredPermissions }
            .flatten()
            .distinctBy { it.id }
            .filter { newPerm -> mappedPermissions.none { it.id == newPerm.id } }
            .map { it.toDeclaredPermission(appWithPermissions) }
            .toList()
            .let {
                log(TAG) { "${it.size} permissions declared by apps" }
                mappedPermissions.addAll(it)
            }

        AExtraPerm.values
            .map { perm ->
                ExtraPermission(
                    id = perm.id,
                    tags = perm.tags,
                    groupIds = perm.groupIds,
                    requestingPkgs = apps.filter { it.requestsPermission(perm.id) },
                    declaringPkgs = apps.filter { it.id == AKnownPkg.AndroidSystem.id }
                )
            }
            .let {
                log(TAG) { "${it.size} special permissions" }
                mappedPermissions.addAll(it)
            }

        // All that are specified by apps via `uses-permission`
        // It's possible that some of these are unused as no other app declares them.
        appWithPermissions
            .asSequence()
            .map { usesPerms -> usesPerms.requestedPermissions.map { it.id } }
            .flatten()
            .distinct()
            .filter { newPerm -> mappedPermissions.none { it.id == newPerm } }
            .map { id ->
                val info = packageManager.getPermissionInfo2(id, PackageManager.GET_META_DATA)
                when {
                    info != null -> info.toDeclaredPermission(appWithPermissions)
                    else -> id.toUnusedPermission(appWithPermissions)
                }
            }
            .distinctBy { it.id }
            .toList()
            .let {
                log(TAG) { "${it.size} unknown permissions requested by apps" }
                mappedPermissions.addAll(it)
            }

        mappedPermissions.also { log(TAG) { "${it.size} total permissions discovered." } }
    }
        .catch {
            log(TAG, ERROR) { "Failed to generate permission data: ${it.asLog()}" }
            throw it
        }
        .shareLatest(scope = appScope, started = SharingStarted.Lazily)

    private fun PermissionInfo.toDeclaredPermission(apps: Collection<BasePkg>): DeclaredPermission {
        val groupIds = APerm.values.singleOrNull { it.id == id }?.groupIds?.let { want ->
            APermGrp.values.filter { want.contains(it.id) }.map { it.id }
        } ?: emptySet()

        val knownTags = APerm.values.singleOrNull { it.id == id }?.tags ?: emptySet()

        val detectedTags = mutableSetOf<PermissionTag>()

        when {
            protectionTypeCompat == ProtectionType.DANGEROUS -> {
                detectedTags.add(RuntimeGrant)
            }
            protectionFlagsCompat.contains(ProtectionFlag.APPOP) -> {
                detectedTags.add(SpecialAccess)
            }
            else -> {
                detectedTags.add(InstallTimeGrant)
            }
        }

        return DeclaredPermission(
            permissionInfo = this,
            requestingPkgs = apps.filter { it.requestsPermission(id) },
            declaringPkgs = apps.filter { it.declaresPermission(id) },
            groupIds = groupIds,
            tags = knownTags + detectedTags,
        )
    }

    private fun Permission.Id.toUnusedPermission(apps: Collection<BasePkg>): UnknownPermission = UnknownPermission(
        id = this,
        requestingPkgs = apps.filter { it.requestsPermission(this) },
        groupIds = APerm.values.singleOrNull { it.id == this }?.groupIds?.let { want ->
            APermGrp.values.filter { want.contains(it.id) }.map { it.id }
        } ?: emptySet(),
        tags = APerm.values.singleOrNull { it.id == this }?.tags ?: emptySet(),
    )

    companion object {
        private val TAG = logTag("Permissions", "Repo")
    }
}