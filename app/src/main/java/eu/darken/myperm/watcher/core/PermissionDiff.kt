package eu.darken.myperm.watcher.core

import kotlinx.serialization.Serializable

@Serializable
data class PermissionDiff(
    val addedPermissions: List<String> = emptyList(),
    val removedPermissions: List<String> = emptyList(),
    val grantChanges: List<GrantChange> = emptyList(),
    val addedDeclared: List<String> = emptyList(),
    val removedDeclared: List<String> = emptyList(),
) {
    @Serializable
    data class GrantChange(
        val permissionId: String,
        val oldStatus: String,
        val newStatus: String,
    )

    val isEmpty: Boolean
        get() = addedPermissions.isEmpty()
                && removedPermissions.isEmpty()
                && grantChanges.isEmpty()
                && addedDeclared.isEmpty()
                && removedDeclared.isEmpty()
}
