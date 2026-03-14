package eu.darken.myperm.watcher.core

import eu.darken.myperm.apps.core.features.UsesPermission
import kotlinx.serialization.Serializable

@Serializable
data class PermissionDiff(
    val addedPermissions: List<String> = emptyList(),
    val removedPermissions: List<String> = emptyList(),
    val grantChanges: List<GrantChange> = emptyList(),
    val addedDeclared: List<String> = emptyList(),
    val removedDeclared: List<String> = emptyList(),
    val gainedCount: Int = 0,
    val lostCount: Int = 0,
) {
    @Serializable
    data class GrantChange(
        val permissionId: String,
        val oldStatus: UsesPermission.Status,
        val newStatus: UsesPermission.Status,
    )

    val isEmpty: Boolean
        get() = addedPermissions.isEmpty()
                && removedPermissions.isEmpty()
                && grantChanges.isEmpty()
                && addedDeclared.isEmpty()
                && removedDeclared.isEmpty()
}
