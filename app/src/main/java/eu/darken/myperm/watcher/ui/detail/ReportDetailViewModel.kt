package eu.darken.myperm.watcher.ui.detail

import android.content.Context
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.room.dao.SnapshotPkgDao
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.apps.core.getPermissionInfo2
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.ProtectionType
import eu.darken.myperm.permissions.core.ProtectionFlag
import eu.darken.myperm.permissions.core.protectionFlagsCompat
import eu.darken.myperm.permissions.core.protectionTypeCompat
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.watcher.core.PermissionDiff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

enum class GrantType { RUNTIME, INSTALL_TIME, SPECIAL_ACCESS, UNKNOWN }

data class EnrichedPermission(
    val id: String,
    val label: String?,
    val description: String?,
    val grantType: GrantType,
)

@HiltViewModel(assistedFactory = ReportDetailViewModel.Factory::class)
class ReportDetailViewModel @AssistedInject constructor(
    @Assisted private val reportId: Long,
    dispatcherProvider: DispatcherProvider,
    private val changeDao: PermissionChangeDao,
    private val snapshotPkgDao: SnapshotPkgDao,
    @ApplicationContext private val context: Context,
    private val json: Json,
) : ViewModel4(dispatcherProvider) {

    @AssistedFactory
    interface Factory {
        fun create(reportId: Long): ReportDetailViewModel
    }

    data class State(
        val packageName: String = "",
        val appLabel: String? = null,
        val eventType: String = "",
        val versionName: String? = null,
        val previousVersionName: String? = null,
        val versionCode: Long? = null,
        val previousVersionCode: Long? = null,
        val installerLabel: String? = null,
        val isSystemApp: Boolean = false,
        val userHandleId: Int = 0,
        val detectedAt: Long = 0,
        val diff: PermissionDiff = PermissionDiff(),
        val permissionInfoMap: Map<String, EnrichedPermission> = emptyMap(),
        val isLoading: Boolean = true,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    init {
        launch {
            val entity = changeDao.getById(reportId)
            if (entity != null) {
                val diff = try {
                    json.decodeFromString<PermissionDiff>(entity.changesJson)
                } catch (e: Exception) {
                    log(TAG, WARN) { "Failed to deserialize changesJson for report $reportId: ${e.asLog()}" }
                    PermissionDiff()
                }

                val snapshotPkg = entity.sourceSnapshotId?.let { sid ->
                    snapshotPkgDao.getPkgByName(sid, entity.packageName, entity.userHandleId)
                }

                val installerLabel = snapshotPkg?.installerPkgName?.let { name ->
                    AKnownPkg.values.firstOrNull { it.id.pkgName == name }?.labelRes
                        ?.let { context.getString(it) }
                        ?: name.substringAfterLast('.')
                }

                val permissionInfoMap = resolvePermissions(diff)

                _state.value = State(
                    packageName = entity.packageName,
                    appLabel = entity.appLabel,
                    userHandleId = entity.userHandleId,
                    eventType = entity.eventType,
                    versionName = entity.versionName,
                    previousVersionName = entity.previousVersionName,
                    versionCode = entity.versionCode,
                    previousVersionCode = entity.previousVersionCode,
                    installerLabel = installerLabel,
                    isSystemApp = snapshotPkg?.isSystemApp ?: false,
                    detectedAt = entity.detectedAt,
                    diff = diff,
                    permissionInfoMap = permissionInfoMap,
                    isLoading = false,
                )
            } else {
                _state.value = State(isLoading = false)
            }
        }
    }

    private fun resolvePermissions(diff: PermissionDiff): Map<String, EnrichedPermission> {
        val allIds = buildSet {
            addAll(diff.addedPermissions)
            addAll(diff.removedPermissions)
            addAll(diff.addedDeclared)
            addAll(diff.removedDeclared)
            addAll(diff.grantChanges.map { it.permissionId })
        }

        val pm = context.packageManager
        return allIds.associateWith { permId ->
            val permissionId = Permission.Id(permId)
            val permObj = object : Permission {
                override val id = permissionId
            }

            val label = permObj.getLabel(context)
            val description = permObj.getDescription(context)

            val permInfo = pm.getPermissionInfo2(permissionId)
            val grantType = if (permInfo != null) {
                when {
                    permInfo.protectionTypeCompat == ProtectionType.DANGEROUS -> GrantType.RUNTIME
                    permInfo.protectionFlagsCompat.contains(ProtectionFlag.APPOP) -> GrantType.SPECIAL_ACCESS
                    else -> GrantType.INSTALL_TIME
                }
            } else {
                GrantType.UNKNOWN
            }

            EnrichedPermission(
                id = permId,
                label = label,
                description = description,
                grantType = grantType,
            )
        }
    }

    fun onViewApp() {
        val current = _state.value
        navTo(Nav.Details.AppDetails(current.packageName, current.userHandleId, current.appLabel))
    }

    companion object {
        private val TAG = logTag("Watcher", "ReportDetail", "VM")
    }
}
