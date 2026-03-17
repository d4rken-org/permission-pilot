package eu.darken.myperm.export.core

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.export.core.formatter.ExportFormatter
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.core.container.DeclaredPermission
import eu.darken.myperm.permissions.core.features.InstallTimeGrant
import eu.darken.myperm.permissions.core.features.RuntimeGrant
import eu.darken.myperm.permissions.core.features.SpecialAccess
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
    private val formatters: Map<ExportFormat, @JvmSuppressWildcards ExportFormatter>,
) {

    suspend fun exportApps(
        apps: List<AppInfo>,
        permissions: Collection<BasePermission>,
        config: AppExportConfig,
    ): Result<String> = withContext(dispatcherProvider.IO) {
        runCatching {
            log(TAG) { "Exporting ${apps.size} apps as ${config.format}" }
            val formatter = formatters.getValue(config.format)
            val resolvedPerms = resolvePermissions(permissions)
            val permLookup = resolvedPerms.associateBy { it.id }
            formatter.formatApps(apps, config) { permLookup[it] }
        }
    }

    suspend fun exportPermissions(
        permissions: Collection<BasePermission>,
        config: PermissionExportConfig,
    ): Result<String> = withContext(dispatcherProvider.IO) {
        runCatching {
            log(TAG) { "Exporting ${permissions.size} permissions as ${config.format}" }
            val formatter = formatters.getValue(config.format)
            val resolvedPerms = resolvePermissions(permissions)
            formatter.formatPermissions(resolvedPerms, config)
        }
    }

    fun previewApps(
        apps: List<AppInfo>,
        permissions: Collection<BasePermission>,
        config: AppExportConfig,
    ): String {
        val formatter = formatters[config.format] ?: return ""
        val resolvedPerms = resolvePermissions(permissions)
        val permLookup = resolvedPerms.associateBy { it.id }
        return formatter.formatApps(apps.take(1), config) { permLookup[it] }
    }

    fun previewPermissions(
        permissions: Collection<BasePermission>,
        config: PermissionExportConfig,
    ): String {
        val formatter = formatters[config.format] ?: return ""
        val resolvedPerms = resolvePermissions(permissions.take(1))
        return formatter.formatPermissions(resolvedPerms, config)
    }

    private fun resolvePermissions(permissions: Collection<BasePermission>): List<ResolvedPermissionInfo> {
        return permissions.map { perm ->
            val type = when {
                perm.tags.any { it is RuntimeGrant } -> PermissionType.RUNTIME
                perm.tags.any { it is SpecialAccess } -> PermissionType.SPECIAL
                perm.tags.any { it is InstallTimeGrant } -> PermissionType.INSTALL_TIME
                else -> PermissionType.UNKNOWN
            }
            ResolvedPermissionInfo(
                id = perm.id.value,
                label = perm.getLabel(context),
                description = perm.getDescription(context),
                type = type,
                protectionType = (perm as? DeclaredPermission)?.protectionType?.name,
                requestingAppCount = perm.requestingApps.size,
                grantedAppCount = perm.grantingApps.size,
                requestingApps = perm.requestingApps.map { ref ->
                    ResolvedAppRef(
                        pkgName = ref.pkgName,
                        label = ref.label,
                        isGranted = ref.status.isGranted,
                    )
                },
            )
        }
    }

    companion object {
        private val TAG = logTag("Export", "Engine")
    }
}
