package eu.darken.myperm.export.core.formatter

import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.export.core.AppExportConfig
import eu.darken.myperm.export.core.AppExportConfig.PermissionDetailLevel
import eu.darken.myperm.export.core.ExportFormat
import eu.darken.myperm.export.core.PermissionExportConfig
import eu.darken.myperm.export.core.ResolvedPermissionInfo
import javax.inject.Inject

class CsvFormatter @Inject constructor() : ExportFormatter {

    override val format: ExportFormat = ExportFormat.CSV

    override fun formatApps(
        apps: List<AppInfo>,
        config: AppExportConfig,
        permissionLookup: (String) -> ResolvedPermissionInfo?,
    ): String = buildString {
        val headers = buildHeaders(config)
        appendLine(headers.joinToString(","))

        apps.sortedBy { it.label.lowercase() }.forEach { app ->
            if (config.permissionDetailLevel != PermissionDetailLevel.NONE && app.requestedPermissions.isNotEmpty()) {
                app.requestedPermissions.sortedBy { it.permissionId }.forEach { perm ->
                    val resolved = permissionLookup(perm.permissionId)
                    val row = buildAppRow(app, config) + buildPermRow(perm, resolved, config)
                    appendLine(row.joinToString(",") { escapeCsv(it) })
                }
            } else {
                val row = buildAppRow(app, config)
                appendLine(row.joinToString(",") { escapeCsv(it) })
            }
        }
    }.trimEnd() + "\n"

    override fun formatPermissions(
        permissions: List<ResolvedPermissionInfo>,
        config: PermissionExportConfig,
    ): String = buildString {
        val headers = buildPermHeaders(config)
        appendLine(headers.joinToString(","))

        permissions.sortedBy { it.id }.forEach { perm ->
            if (config.includeRequestingApps) {
                val apps = if (config.grantedOnly) {
                    perm.requestingApps.filter { it.isGranted }
                } else {
                    perm.requestingApps
                }.sortedBy { it.label.lowercase() }

                if (apps.isEmpty()) {
                    val row = buildPermBaseRow(perm, config)
                    appendLine(row.joinToString(",") { escapeCsv(it) })
                } else {
                    apps.forEach { app ->
                        val row = buildPermBaseRow(perm, config) + listOf(
                            app.label,
                            app.pkgName,
                            if (app.isGranted) "granted" else "denied",
                        )
                        appendLine(row.joinToString(",") { escapeCsv(it) })
                    }
                }
            } else {
                val row = buildPermBaseRow(perm, config)
                appendLine(row.joinToString(",") { escapeCsv(it) })
            }
        }
    }.trimEnd() + "\n"

    private fun buildHeaders(config: AppExportConfig): List<String> = buildList {
        add("package_name")
        add("app_label")
        if (config.includeMetaInfo) {
            addAll(listOf("version_name", "version_code", "installed_at", "updated_at", "target_sdk", "min_sdk", "compile_sdk", "is_system_app"))
        }
        if (config.permissionDetailLevel != PermissionDetailLevel.NONE) {
            add("permission_id")
            add("permission_status")
            if (config.permissionDetailLevel >= PermissionDetailLevel.NAME_STATUS_TYPE) add("permission_type")
            if (config.permissionDetailLevel >= PermissionDetailLevel.FULL) {
                add("protection_type")
                add("permission_description")
            }
        }
    }

    private fun buildAppRow(app: AppInfo, config: AppExportConfig): List<String> = buildList {
        add(app.pkgName)
        add(app.label)
        if (config.includeMetaInfo) {
            add(app.versionName ?: "")
            add(app.versionCode.toString())
            add(app.installedAt?.let { MarkdownFormatter.formatDate(it) } ?: "")
            add(app.updatedAt?.let { MarkdownFormatter.formatDate(it) } ?: "")
            add(app.apiTargetLevel?.toString() ?: "")
            add(app.apiMinimumLevel?.toString() ?: "")
            add(app.apiCompileLevel?.toString() ?: "")
            add(if (app.isSystemApp) "true" else "false")
        }
    }

    private fun buildPermRow(
        perm: eu.darken.myperm.apps.core.PermissionUse,
        resolved: ResolvedPermissionInfo?,
        config: AppExportConfig,
    ): List<String> = buildList {
        add(perm.permissionId)
        add(perm.status.name.lowercase())
        if (config.permissionDetailLevel >= PermissionDetailLevel.NAME_STATUS_TYPE) {
            add(resolved?.type?.name?.lowercase() ?: "unknown")
        }
        if (config.permissionDetailLevel >= PermissionDetailLevel.FULL) {
            add(resolved?.protectionType ?: "")
            add(resolved?.description ?: "")
        }
    }

    private fun buildPermHeaders(config: PermissionExportConfig): List<String> = buildList {
        add("permission_id")
        add("permission_label")
        if (config.includeSummaryCounts) {
            add("requesting_count")
            add("granted_count")
        }
        if (config.includeRequestingApps) {
            add("app_label")
            add("app_package")
            add("app_status")
        }
    }

    private fun buildPermBaseRow(
        perm: ResolvedPermissionInfo,
        config: PermissionExportConfig,
    ): List<String> = buildList {
        add(perm.id)
        add(perm.label ?: perm.id.substringAfterLast('.'))
        if (config.includeSummaryCounts) {
            add(perm.requestingAppCount.toString())
            add(perm.grantedAppCount.toString())
        }
    }

    companion object {
        private val FORMULA_PREFIXES = charArrayOf('=', '+', '-', '@')

        fun escapeCsv(value: String): String {
            val sanitized = if (value.isNotEmpty() && value[0] in FORMULA_PREFIXES) {
                "'$value"
            } else {
                value
            }
            return if (sanitized.contains(',') || sanitized.contains('"') || sanitized.contains('\n') || sanitized.contains('\r')) {
                "\"${sanitized.replace("\"", "\"\"")}\""
            } else {
                sanitized
            }
        }
    }
}
