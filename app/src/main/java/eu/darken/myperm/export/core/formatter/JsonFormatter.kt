package eu.darken.myperm.export.core.formatter

import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.export.core.AppExportConfig
import eu.darken.myperm.export.core.AppExportConfig.PermissionDetailLevel
import eu.darken.myperm.export.core.ExportFormat
import eu.darken.myperm.export.core.PermissionExportConfig
import eu.darken.myperm.export.core.ResolvedPermissionInfo
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject

class JsonFormatter @Inject constructor() : ExportFormatter {

    override val format: ExportFormat = ExportFormat.JSON

    override fun formatApps(
        apps: List<AppInfo>,
        config: AppExportConfig,
        permissionLookup: (String) -> ResolvedPermissionInfo?,
    ): String {
        val root = buildJsonObject {
            put("exportedAt", Instant.now().toString())
            put("apps", buildJsonArray {
                apps.sortedBy { it.label.lowercase() }.forEach { app ->
                    add(buildAppObject(app, config, permissionLookup))
                }
            })
        }
        return prettyPrint(root)
    }

    override fun formatPermissions(
        permissions: List<ResolvedPermissionInfo>,
        config: PermissionExportConfig,
    ): String {
        val root = buildJsonObject {
            put("exportedAt", Instant.now().toString())
            put("permissions", buildJsonArray {
                permissions.sortedBy { it.id }.forEach { perm ->
                    add(buildPermObject(perm, config))
                }
            })
        }
        return prettyPrint(root)
    }

    private fun buildAppObject(
        app: AppInfo,
        config: AppExportConfig,
        permissionLookup: (String) -> ResolvedPermissionInfo?,
    ): JsonObject = buildJsonObject {
        put("packageName", app.pkgName.value)
        put("label", app.label)

        if (config.includeMetaInfo) {
            app.versionName?.let { put("versionName", it) }
            put("versionCode", app.versionCode)
            app.installedAt?.let { put("installedAt", it.toString()) }
            app.updatedAt?.let { put("updatedAt", it.toString()) }
            app.apiTargetLevel?.let { put("targetSdk", it) }
            app.apiMinimumLevel?.let { put("minSdk", it) }
            app.apiCompileLevel?.let { put("compileSdk", it) }
            put("isSystemApp", app.isSystemApp)
        }

        if (config.permissionDetailLevel != PermissionDetailLevel.NONE) {
            put("permissions", buildJsonArray {
                app.requestedPermissions.sortedBy { it.permissionId }.forEach { perm ->
                    val resolved = permissionLookup(perm.permissionId)
                    add(buildJsonObject {
                        put("id", perm.permissionId)
                        put("status", perm.status.name.lowercase())
                        if (config.permissionDetailLevel >= PermissionDetailLevel.NAME_STATUS_TYPE) {
                            put("type", resolved?.type?.name?.lowercase() ?: "unknown")
                        }
                        if (config.permissionDetailLevel >= PermissionDetailLevel.FULL) {
                            resolved?.protectionType?.let { put("protectionType", it) }
                            resolved?.description?.let { put("description", it) }
                        }
                    })
                }
            })
        }
    }

    private fun buildPermObject(
        perm: ResolvedPermissionInfo,
        config: PermissionExportConfig,
    ): JsonObject = buildJsonObject {
        put("id", perm.id)
        put("label", perm.label ?: perm.id.substringAfterLast('.'))

        if (config.includeSummaryCounts) {
            put("requestingCount", perm.requestingAppCount)
            put("grantedCount", perm.grantedAppCount)
        }

        if (config.includeRequestingApps) {
            val apps = if (config.grantedOnly) {
                perm.requestingApps.filter { it.isGranted }
            } else {
                perm.requestingApps
            }.sortedBy { it.label.lowercase() }

            put("apps", buildJsonArray {
                apps.forEach { app ->
                    add(buildJsonObject {
                        put("packageName", app.pkgName)
                        put("label", app.label)
                        put("status", if (app.isGranted) "granted" else "denied")
                    })
                }
            })
        }
    }

    companion object {
        fun prettyPrint(element: JsonElement, indent: String = ""): String = buildString {
            when (element) {
                is JsonPrimitive -> append(element.toString())
                is JsonArray -> {
                    if (element.isEmpty()) {
                        append("[]")
                    } else {
                        appendLine("[")
                        element.forEachIndexed { index, item ->
                            append("$indent  ")
                            append(prettyPrint(item, "$indent  "))
                            if (index < element.size - 1) append(",")
                            appendLine()
                        }
                        append("$indent]")
                    }
                }
                is JsonObject -> {
                    if (element.isEmpty()) {
                        append("{}")
                    } else {
                        appendLine("{")
                        val entries = element.entries.toList()
                        entries.forEachIndexed { index, (key, value) ->
                            append("$indent  \"$key\": ")
                            append(prettyPrint(value, "$indent  "))
                            if (index < entries.size - 1) append(",")
                            appendLine()
                        }
                        append("$indent}")
                    }
                }
            }
        }
    }
}
