package eu.darken.myperm.export.core.formatter

import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.export.core.AppExportConfig
import eu.darken.myperm.export.core.AppExportConfig.PermissionDetailLevel
import eu.darken.myperm.export.core.ExportFormat
import eu.darken.myperm.export.core.PermissionExportConfig
import eu.darken.myperm.export.core.ResolvedPermissionInfo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class MarkdownFormatter @Inject constructor() : ExportFormatter {

    override val format: ExportFormat = ExportFormat.MARKDOWN

    override fun formatApps(
        apps: List<AppInfo>,
        config: AppExportConfig,
        permissionLookup: (String) -> ResolvedPermissionInfo?,
    ): String = buildString {
        appendLine("# App Info Export")
        appendLine("Generated: ${formatDate(Instant.now())}")
        appendLine()

        apps.sortedBy { it.label.lowercase() }.forEach { app ->
            appendLine("## ${escapeMd(app.label)} (${escapeMd(app.pkgName)})")
            appendLine()

            if (config.includeMetaInfo) {
                appendLine("| Field | Value |")
                appendLine("|---|---|")
                app.versionName?.let { appendLine("| Version | ${escapeMd(it)} (${app.versionCode}) |") }
                app.installedAt?.let { appendLine("| Installed | ${formatDate(it)} |") }
                app.updatedAt?.let { appendLine("| Updated | ${formatDate(it)} |") }
                app.apiTargetLevel?.let { appendLine("| Target SDK | $it |") }
                app.apiMinimumLevel?.let { appendLine("| Min SDK | $it |") }
                app.apiCompileLevel?.let { appendLine("| Compile SDK | $it |") }
                appendLine("| System app | ${if (app.isSystemApp) "Yes" else "No"} |")
                appendLine()
            }

            if (config.permissionDetailLevel != PermissionDetailLevel.NONE) {
                val perms = app.requestedPermissions.sortedBy { it.permissionId }
                appendLine("### Permissions (${perms.size} total)")
                appendLine()

                if (perms.isEmpty()) {
                    appendLine("No permissions requested.")
                    appendLine()
                } else {
                    val headers = buildList {
                        add("Permission")
                        add("Status")
                        if (config.permissionDetailLevel >= PermissionDetailLevel.NAME_STATUS_TYPE) add("Type")
                        if (config.permissionDetailLevel >= PermissionDetailLevel.FULL) {
                            add("Protection")
                            add("Description")
                        }
                    }
                    appendLine("| ${headers.joinToString(" | ")} |")
                    appendLine("| ${headers.joinToString(" | ") { "---" }} |")

                    perms.forEach { perm ->
                        val resolved = permissionLookup(perm.permissionId)
                        val cols = buildList {
                            add(escapeMd(resolved?.label ?: perm.permissionId))
                            add(perm.status.name.lowercase().replaceFirstChar { it.uppercase() })
                            if (config.permissionDetailLevel >= PermissionDetailLevel.NAME_STATUS_TYPE) {
                                add(resolved?.type?.name?.lowercase()?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "Unknown")
                            }
                            if (config.permissionDetailLevel >= PermissionDetailLevel.FULL) {
                                add(escapeMd(resolved?.protectionType ?: ""))
                                add(escapeMd(resolved?.description ?: ""))
                            }
                        }
                        appendLine("| ${cols.joinToString(" | ")} |")
                    }
                    appendLine()
                }
            }

            appendLine("---")
            appendLine()
        }
    }.trimEnd() + "\n"

    override fun formatPermissions(
        permissions: List<ResolvedPermissionInfo>,
        config: PermissionExportConfig,
    ): String = buildString {
        appendLine("# Permission Info Export")
        appendLine("Generated: ${formatDate(Instant.now())}")
        appendLine()

        permissions.sortedBy { it.id }.forEach { perm ->
            val label = perm.label ?: perm.id.substringAfterLast('.')
            appendLine("## ${escapeMd(label)} (${escapeMd(perm.id)})")
            appendLine()

            if (config.includeSummaryCounts) {
                appendLine("**${perm.grantedAppCount} granted** of ${perm.requestingAppCount} requesting apps")
                appendLine()
            }

            if (config.includeRequestingApps) {
                val apps = if (config.grantedOnly) {
                    perm.requestingApps.filter { it.isGranted }
                } else {
                    perm.requestingApps
                }.sortedBy { it.label.lowercase() }

                if (apps.isEmpty()) {
                    appendLine("No apps${if (config.grantedOnly) " granted" else ""}.")
                    appendLine()
                } else {
                    appendLine("| App | Package | Status |")
                    appendLine("|---|---|---|")
                    apps.forEach { app ->
                        appendLine("| ${escapeMd(app.label)} | ${escapeMd(app.pkgName)} | ${if (app.isGranted) "Granted" else "Denied"} |")
                    }
                    appendLine()
                }
            }

            appendLine("---")
            appendLine()
        }
    }.trimEnd() + "\n"

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())

        fun formatDate(instant: Instant): String = dateFormatter.format(instant)

        fun escapeMd(text: String): String = text.replace("|", "\\|")
    }
}
