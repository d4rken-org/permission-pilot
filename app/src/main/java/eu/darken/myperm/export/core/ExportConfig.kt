package eu.darken.myperm.export.core

data class AppExportConfig(
    val format: ExportFormat = ExportFormat.MARKDOWN,
    val includeMetaInfo: Boolean = true,
    val permissionDetailLevel: PermissionDetailLevel = PermissionDetailLevel.NAME_AND_STATUS,
) {
    enum class PermissionDetailLevel {
        NONE,
        NAME_AND_STATUS,
        NAME_STATUS_TYPE,
        FULL,
    }
}

data class PermissionExportConfig(
    val format: ExportFormat = ExportFormat.MARKDOWN,
    val includeRequestingApps: Boolean = true,
    val grantedOnly: Boolean = false,
    val includeSummaryCounts: Boolean = true,
)
