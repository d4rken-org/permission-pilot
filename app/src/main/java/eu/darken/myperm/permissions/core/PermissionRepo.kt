package eu.darken.myperm.permissions.core

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.common.IPCFunnel
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.shareLatest
import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.core.container.DeclaredPermission
import eu.darken.myperm.permissions.core.container.ExtraPermission
import eu.darken.myperm.permissions.core.container.PermissionAppRef
import eu.darken.myperm.permissions.core.container.UnknownPermission
import eu.darken.myperm.permissions.core.features.InstallTimeGrant
import eu.darken.myperm.permissions.core.features.PermissionTag
import eu.darken.myperm.permissions.core.features.RuntimeGrant
import eu.darken.myperm.permissions.core.features.SpecialAccess
import eu.darken.myperm.permissions.core.known.AExtraPerm
import eu.darken.myperm.permissions.core.known.APerm
import eu.darken.myperm.permissions.core.known.APermGrp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.onStart
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.measureTimedValue

@Singleton
class PermissionRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    @AppScope private val appScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
    private val appRepo: AppRepo,
    private val ipcFunnel: IPCFunnel,
) {

    private val refreshTrigger = MutableStateFlow(UUID.randomUUID())

    fun refresh() {
        log(TAG) { "refresh() " }
        refreshTrigger.value = UUID.randomUUID()
    }

    val state: Flow<State> = combineTransform(
        appRepo.appData,
        appRepo.scanError,
        refreshTrigger,
    ) { appDataState, scanError, _ ->
        // Scan failed with no cached snapshot: cascade the error so permissions consumers
        // see it too, instead of sitting on Loading forever.
        if (scanError != null && appDataState is AppRepo.AppDataState.NoSnapshot) {
            emit(State.Error(scanError))
            return@combineTransform
        }

        emit(State.Loading())

        val apps = (appDataState as? AppRepo.AppDataState.Ready)?.apps ?: return@combineTransform

        try {
            val start = System.currentTimeMillis()

            // Build indexes from cached app data
            val permToApps = buildPermToAppsIndex(apps)

            // Build declaring apps index from DB
            val declaredPermToApps = buildDeclaredPermToAppsIndex(apps)

            val mappedPermissions = mutableSetOf<BasePermission>()

            val fromAosp = measureTimedValue {
                getPermissionsAOSP(permToApps, declaredPermToApps)
            }.let {
                log(TAG) { "Perf: ${it.value.size} permissions from AOSP in ${it.duration.inWholeMilliseconds}ms" }
                it.value
            }
            mappedPermissions.addAll(fromAosp)

            val aospIds = fromAosp.map { it.id }.toSet()

            val declared = measureTimedValue {
                getPermissionsDeclared(permToApps, declaredPermToApps, aospIds)
            }.let {
                log(TAG) { "Perf: ${it.value.size} permissions declared by apps in ${it.duration.inWholeMilliseconds}ms" }
                it.value
            }
            mappedPermissions.addAll(declared)

            val extra = measureTimedValue {
                getPermissionsExtra(permToApps)
            }.let {
                log(TAG) { "Perf: ${it.value.size} extra permissions in ${it.duration.inWholeMilliseconds}ms" }
                it.value
            }
            mappedPermissions.addAll(extra)

            val undeclared = measureTimedValue {
                getUndeclaredPermissions(permToApps, declaredPermToApps, mappedPermissions)
            }.let {
                log(TAG) { "Perf: ${it.value.size} undeclared permissions in ${it.duration.inWholeMilliseconds}ms" }
                it.value
            }
            mappedPermissions.addAll(undeclared)

            val stop = System.currentTimeMillis()
            log(TAG) { "Perf: Total permissions: ${mappedPermissions.size} in ${stop - start}ms" }

            emit(State.Ready(permissions = mappedPermissions))
        } catch (c: CancellationException) {
            throw c
        } catch (e: Exception) {
            // Handle inside the transform so the flow stays alive; an outer .catch would
            // complete the shared upstream and later refresh() calls couldn't re-run.
            log(TAG, ERROR) { "Failed to generate permission data: ${e.asLog()}" }
            emit(State.Error(e))
        }
    }
        .onStart { emit(State.Loading()) }
        .shareLatest(scope = appScope, started = SharingStarted.Lazily)

    private fun buildPermToAppsIndex(
        apps: List<AppInfo>
    ): Map<String, List<PermissionAppRef>> {
        val result = mutableMapOf<String, MutableList<PermissionAppRef>>()
        for (app in apps) {
            for (perm in app.requestedPermissions) {
                val ref = PermissionAppRef(
                    pkgName = app.pkgName,
                    userHandleId = app.userHandleId,
                    label = app.label,
                    isSystemApp = app.isSystemApp,
                    status = perm.status,
                    pkgType = app.pkgType,
                )
                result.getOrPut(perm.permissionId) { mutableListOf() }.add(ref)
            }
        }
        return result
    }

    private suspend fun buildDeclaredPermToAppsIndex(
        apps: List<AppInfo>
    ): Map<String, List<PermissionAppRef>> {
        val declaredPerms = appRepo.getLatestDeclaredPerms()
        val appsByKey = apps.associateBy { Pair(it.pkgName, it.userHandleId) }

        val result = mutableMapOf<String, MutableList<PermissionAppRef>>()
        for (entity in declaredPerms) {
            val key = Pair(entity.pkgName, entity.userHandleId)
            val app = appsByKey[key] ?: continue
            val ref = PermissionAppRef(
                pkgName = app.pkgName,
                userHandleId = app.userHandleId,
                label = app.label,
                isSystemApp = app.isSystemApp,
                status = UsesPermission.Status.UNKNOWN,
                pkgType = app.pkgType,
            )
            result.getOrPut(entity.permissionId) { mutableListOf() }.add(ref)
        }
        return result
    }

    private suspend fun getPermissionsAOSP(
        permToApps: Map<String, List<PermissionAppRef>>,
        declaredPermToApps: Map<String, List<PermissionAppRef>>,
    ): Collection<BasePermission> {
        // getAllPermissionGroups is the platform-level query. A failure here means we
        // can't enumerate system permissions reliably — bubble it up so the caller
        // emits State.Error rather than returning silently-partial data.
        val groups = ipcFunnel.packageManager.getAllPermissionGroups(0) + listOf(null)
        return groups
            .mapNotNull { permissionGroup ->
                val name = permissionGroup?.name
                log(TAG, VERBOSE) { "Querying permission group $name" }
                try {
                    ipcFunnel.packageManager.queryPermissionsByGroup(name, 0)
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Exception) {
                    log(TAG, WARN) { "Failed to retrieve permission group $permissionGroup: ${e.asLog()}" }
                    null
                }
            }
            .flatten()
            .map { it.toDeclaredPermission(permToApps, declaredPermToApps) }
            .distinctBy { it.id }
    }

    private suspend fun getPermissionsDeclared(
        permToApps: Map<String, List<PermissionAppRef>>,
        declaredPermToApps: Map<String, List<PermissionAppRef>>,
        aospIds: Set<Permission.Id>,
    ): Collection<BasePermission> = withContext(dispatcherProvider.IO) {
        declaredPermToApps.keys
            .filter { Permission.Id(it) !in aospIds }
            .map { permId ->
                val id = Permission.Id(permId)
                val permInfo = try {
                    ipcFunnel.packageManager.getPermissionInfo2(id, PackageManager.GET_META_DATA)
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Exception) {
                    log(TAG, WARN) { "getPermissionInfo2 failed for $id: ${e.asLog()}" }
                    null
                }
                if (permInfo != null) {
                    permInfo.toDeclaredPermission(permToApps, declaredPermToApps)
                } else {
                    id.toUnusedPermission(permToApps)
                }
            }
            .distinctBy { it.id }
    }

    private fun getPermissionsExtra(
        permToApps: Map<String, List<PermissionAppRef>>,
    ): Collection<BasePermission> {
        val androidSystemPkgName = AKnownPkg.AndroidSystem.id.pkgName
        val declaringRefs = permToApps.values
            .flatten()
            .filter { it.pkgName == androidSystemPkgName }
            .distinctBy { it.pkgName to it.userHandleId }
            .map { it.copy(status = UsesPermission.Status.UNKNOWN) }

        return AExtraPerm.values
            .map { perm ->
                ExtraPermission(
                    id = perm.id,
                    tags = perm.tags,
                    groupIds = perm.groupIds,
                    requestingApps = permToApps[perm.id.value] ?: emptyList(),
                    declaringApps = declaringRefs,
                )
            }
    }

    private suspend fun getUndeclaredPermissions(
        permToApps: Map<String, List<PermissionAppRef>>,
        declaredPermToApps: Map<String, List<PermissionAppRef>>,
        mappedPermissions: Collection<BasePermission>,
    ): Collection<BasePermission> = withContext(dispatcherProvider.IO) {
        val mappedIds = mappedPermissions.map { it.id }.toSet()
        permToApps.keys
            .map { Permission.Id(it) }
            .filter { it !in mappedIds }
            .map { id ->
                val info = try {
                    ipcFunnel.packageManager.getPermissionInfo2(id, PackageManager.GET_META_DATA)
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Exception) {
                    log(TAG, WARN) { "getPermissionInfo2 failed for $id: ${e.asLog()}" }
                    null
                }
                when {
                    info != null -> info.toDeclaredPermission(permToApps, declaredPermToApps)
                    else -> id.toUnusedPermission(permToApps)
                }
            }
            .distinctBy { it.id }
    }


    private fun PermissionInfo.toDeclaredPermission(
        permToApps: Map<String, List<PermissionAppRef>>,
        declaredPermToApps: Map<String, List<PermissionAppRef>>,
    ): DeclaredPermission {
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
            requestingApps = permToApps[name] ?: emptyList(),
            declaringApps = declaredPermToApps[name] ?: emptyList(),
            groupIds = groupIds,
            tags = knownTags + detectedTags,
        )
    }

    private fun Permission.Id.toUnusedPermission(
        permToApps: Map<String, List<PermissionAppRef>>,
    ): UnknownPermission = UnknownPermission(
        id = this,
        requestingApps = permToApps[this.value] ?: emptyList(),
        groupIds = APerm.values.singleOrNull { it.id == this }?.groupIds?.let { want ->
            APermGrp.values.filter { want.contains(it.id) }.map { it.id }
        } ?: emptySet(),
        tags = APerm.values.singleOrNull { it.id == this }?.tags ?: emptySet(),
    )

    sealed class State {
        data class Loading(
            val startedAt: Instant = Instant.now()
        ) : State()

        data class Ready(
            val updatedAt: Instant = Instant.now(),
            val permissions: Collection<BasePermission>,
        ) : State()

        data class Error(
            val error: Throwable,
            val at: Instant = Instant.now(),
        ) : State()
    }

    companion object {
        private val TAG = logTag("Permissions", "Repo")
    }
}
