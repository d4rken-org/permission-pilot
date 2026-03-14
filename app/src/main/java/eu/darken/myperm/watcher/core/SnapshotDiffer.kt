package eu.darken.myperm.watcher.core

import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.room.entity.SnapshotPkgDeclaredPermEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgPermEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotDiffer @Inject constructor() {

    fun diff(
        previousPerms: List<SnapshotPkgPermEntity>,
        previousDeclared: List<SnapshotPkgDeclaredPermEntity>,
        currentPerms: List<CurrentPermission>,
        currentDeclared: List<String>,
    ): PermissionDiff {
        val prevPermIds = previousPerms.map { it.permissionId }.toSet()
        val currPermIds = currentPerms.map { it.permissionId }.toSet()

        val addedPermissions = (currPermIds - prevPermIds).toList()
        val removedPermissions = (prevPermIds - currPermIds).toList()

        val prevStatusMap = previousPerms.associate { it.permissionId to UsesPermission.Status.valueOf(it.status) }
        val grantChanges = currentPerms
            .filter { it.permissionId in prevPermIds }
            .mapNotNull { curr ->
                val prevStatus = prevStatusMap[curr.permissionId] ?: return@mapNotNull null
                if (prevStatus != curr.status) {
                    PermissionDiff.GrantChange(
                        permissionId = curr.permissionId,
                        oldStatus = prevStatus,
                        newStatus = curr.status,
                    )
                } else null
            }

        val prevDeclaredIds = previousDeclared.map { it.permissionId }.toSet()
        val currDeclaredIds = currentDeclared.toSet()
        val addedDeclared = (currDeclaredIds - prevDeclaredIds).toList()
        val removedDeclared = (prevDeclaredIds - currDeclaredIds).toList()

        return PermissionDiff(
            addedPermissions = addedPermissions,
            removedPermissions = removedPermissions,
            grantChanges = grantChanges,
            addedDeclared = addedDeclared,
            removedDeclared = removedDeclared,
        )
    }

    data class CurrentPermission(
        val permissionId: String,
        val status: UsesPermission.Status,
    )
}
