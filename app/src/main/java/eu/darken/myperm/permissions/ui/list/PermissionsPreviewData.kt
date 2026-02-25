package eu.darken.myperm.permissions.ui.list

import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.container.UnknownPermission
import eu.darken.myperm.permissions.core.features.ManifestDoc
import eu.darken.myperm.permissions.core.features.RuntimeGrant
import eu.darken.myperm.permissions.core.known.APermGrp

internal object PermissionsPreviewData {

    private fun permItem(
        permName: String,
        label: String? = null,
        type: String = "declared",
        requestingCount: Int = 0,
        grantedCount: Int = 0,
    ) = PermissionsViewModel.PermItem(
        id = Permission.Id(permName),
        label = label,
        type = type,
        requestingCount = requestingCount,
        grantedCount = grantedCount,
        permission = UnknownPermission(
            id = Permission.Id(permName),
            tags = if (type == "declared") setOf(RuntimeGrant, ManifestDoc) else emptySet(),
            groupIds = emptySet(),
        ),
    )

    fun readyState() = PermissionsViewModel.State.Ready(
        listData = listOf(
            PermissionsViewModel.ListItem.Group(
                PermissionsViewModel.GroupItem(group = APermGrp.Camera, permCount = 2, isExpanded = true)
            ),
            PermissionsViewModel.ListItem.Perm(
                permItem("android.permission.CAMERA", "Camera", "declared", requestingCount = 45, grantedCount = 12)
            ),
            PermissionsViewModel.ListItem.Perm(
                permItem("android.permission.RECORD_VIDEO", null, "extra", requestingCount = 8, grantedCount = 3)
            ),
            PermissionsViewModel.ListItem.Group(
                PermissionsViewModel.GroupItem(group = APermGrp.Location, permCount = 3, isExpanded = false)
            ),
            PermissionsViewModel.ListItem.Group(
                PermissionsViewModel.GroupItem(group = APermGrp.Contacts, permCount = 2, isExpanded = false)
            ),
        ),
        countPermissions = 7,
        countGroups = 3,
    )

    fun emptyReadyState() = PermissionsViewModel.State.Ready(
        listData = emptyList(),
        countPermissions = 0,
        countGroups = 0,
    )
}
