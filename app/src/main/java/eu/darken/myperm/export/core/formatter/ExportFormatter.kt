package eu.darken.myperm.export.core.formatter

import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.export.core.AppExportConfig
import eu.darken.myperm.export.core.ExportFormat
import eu.darken.myperm.export.core.PermissionExportConfig
import eu.darken.myperm.export.core.ResolvedPermissionInfo

interface ExportFormatter {

    val format: ExportFormat

    fun formatApps(
        apps: List<AppInfo>,
        config: AppExportConfig,
        permissionLookup: (String) -> ResolvedPermissionInfo?,
    ): String

    fun formatPermissions(
        permissions: List<ResolvedPermissionInfo>,
        config: PermissionExportConfig,
    ): String
}
