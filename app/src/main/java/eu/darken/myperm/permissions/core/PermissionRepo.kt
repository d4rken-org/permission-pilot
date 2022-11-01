package eu.darken.myperm.permissions.core

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.apps.core.features.declaresPermission
import eu.darken.myperm.apps.core.features.requestsPermission
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.common.IPCFunnel
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.coroutine.DispatcherProvider
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.util.*
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
        appRepo.state,
        refreshTrigger
    ) { appRepoState, _ ->
        emit(State.Loading())

        val apps = (appRepoState as? AppRepo.State.Ready)?.pkgs ?: return@combineTransform

        val start = System.currentTimeMillis()

        val perms = coroutineScope {
            val aosp = async(dispatcherProvider.Default) {
                measureTimedValue {
                    getPermissionsAOSP(apps)
                }.let {
                    log(TAG) { "Perf: ${it.value.size} permissions from AOSP in ${it.duration.inWholeMilliseconds}ms" }
                    it.value
                }
            }
            val declared = async(dispatcherProvider.Default) {
                measureTimedValue {
                    getPermissionsDeclared(apps).filter { newPerm -> aosp.await().none { it.id == newPerm.id } }
                }.let {
                    log(TAG) { "Perf: ${it.value.size} permissions declared by apps in ${it.duration.inWholeMilliseconds}ms" }
                    it.value
                }
            }
            val extra = async(dispatcherProvider.Default) {
                measureTimedValue {
                    getPermissionsExtra(apps)
                }.let {
                    log(TAG) { "Perf: ${it.value.size} extra permissions in ${it.duration.inWholeMilliseconds}ms" }
                    it.value
                }
            }
            awaitAll(aosp, declared, extra)
            listOf(aosp.await(), declared.await(), extra.await())
        }
        val mappedPermissions = mutableSetOf<BasePermission>()

        // All we know from the system
        val fromAosp = perms[0]
        mappedPermissions.addAll(fromAosp)

        // All that apps have declared themselves
        val declared = perms[1]
        mappedPermissions.addAll(declared)

        // Extra permissions
        val extra = perms[2]
        mappedPermissions.addAll(extra)

        // All that are specified by apps via `uses-permission`
        // It's possible that some of these are unused as no other app declares them.
        val undeclared = measureTimedValue {
            getUndeclaredPermissions(apps, mappedPermissions)
        }.let {
            log(TAG) { "Perf: ${it.value.size} undeclared permissions in ${it.duration.inWholeMilliseconds}ms" }
            it.value
        }
        mappedPermissions.addAll(undeclared)

        val stop = System.currentTimeMillis()
        log(TAG) { "Perf: Total permissions: ${mappedPermissions.size} in ${stop - start}ms" }

        emit(State.Ready(permissions = mappedPermissions, basedOnAppState = appRepoState.id))
    }
        .catch {
            log(TAG, ERROR) { "Failed to generate permission data: ${it.asLog()}" }
            throw it
        }
        .onStart { emit(State.Loading()) }
        .shareLatest(scope = appScope, started = SharingStarted.Lazily)

    private suspend fun getPermissionsAOSP(
        apps: Collection<BasePkg>
    ): Collection<BasePermission> = coroutineScope {
        (ipcFunnel.packageManager.getAllPermissionGroups(0) + listOf(null))
            .mapNotNull { permissionGroup ->
                val name = permissionGroup?.name
                log(TAG, VERBOSE) { "Querying permission group $name" }
                try {
                    ipcFunnel.packageManager.queryPermissionsByGroup(name, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    log(TAG) { "Failed to retrieve permission group $permissionGroup: $e" }
                    null
                }
            }
            .flatten()
            .map {
                async { it.toDeclaredPermission(apps) }
            }
            .awaitAll()
            .distinctBy { it.id }
    }

    private suspend fun getPermissionsDeclared(
        apps: Collection<BasePkg>
    ): Collection<BasePermission> = coroutineScope {
        apps
            .asSequence()
            .map { it.declaredPermissions }
            .flatten()
            .distinctBy { it.id }
            .map { async { it.toDeclaredPermission(apps) } }
            .toList()
            .awaitAll()
    }


    private suspend fun getPermissionsExtra(
        apps: Collection<BasePkg>
    ): Collection<BasePermission> = AExtraPerm.values
        .map { perm ->
            ExtraPermission(
                id = perm.id,
                tags = perm.tags,
                groupIds = perm.groupIds,
                requestingPkgs = apps.filter { it.requestsPermission(perm.id) },
                declaringPkgs = apps.filter { it.id == AKnownPkg.AndroidSystem.id }
            )
        }

    private suspend fun getUndeclaredPermissions(
        apps: Collection<BasePkg>,
        mappedPermissions: Collection<BasePermission>,
    ): Collection<BasePermission> = coroutineScope {
        apps
            .asSequence()
            .map { usesPerms ->
                async {
                    usesPerms.requestedPermissions.map { it.id }
                        .distinct()
                        .filter { newPerm -> mappedPermissions.none { it.id == newPerm } }
                        .map { id ->
                            val info = ipcFunnel.packageManager.getPermissionInfo2(id, PackageManager.GET_META_DATA)
                            when {
                                info != null -> info.toDeclaredPermission(apps)
                                else -> id.toUnusedPermission(apps)
                            }
                        }
                }
            }
            .toList()
            .awaitAll()
            .flatten()
            .distinctBy { it.id }
    }


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

    sealed class State {
        data class Loading(
            val startedAt: Instant = Instant.now()
        ) : State()

        data class Ready(
            val updatedAt: Instant = Instant.now(),
            val permissions: Collection<BasePermission>,
            val basedOnAppState: UUID,
        ) : State()
    }

    companion object {
        private val TAG = logTag("Permissions", "Repo")
    }
}