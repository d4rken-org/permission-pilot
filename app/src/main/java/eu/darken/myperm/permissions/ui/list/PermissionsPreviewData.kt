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
                PermissionsViewModel.GroupItem(group = APermGrp.Apps, permCount = 9, isExpanded = false)
            ),
            PermissionsViewModel.ListItem.Group(
                PermissionsViewModel.GroupItem(group = APermGrp.Calendar, permCount = 2, isExpanded = false)
            ),
            PermissionsViewModel.ListItem.Group(
                PermissionsViewModel.GroupItem(group = APermGrp.Calls, permCount = 8, isExpanded = false)
            ),
            PermissionsViewModel.ListItem.Group(
                PermissionsViewModel.GroupItem(group = APermGrp.Camera, permCount = 1, isExpanded = true)
            ),
            PermissionsViewModel.ListItem.Perm(
                permItem("android.permission.CAMERA", "take pictures and videos", "declared", requestingCount = 34, grantedCount = 15)
            ),
            PermissionsViewModel.ListItem.Group(
                PermissionsViewModel.GroupItem(group = APermGrp.Connectivity, permCount = 3, isExpanded = false)
            ),
            PermissionsViewModel.ListItem.Group(
                PermissionsViewModel.GroupItem(group = APermGrp.Contacts, permCount = 2, isExpanded = false)
            ),
            PermissionsViewModel.ListItem.Group(
                PermissionsViewModel.GroupItem(group = APermGrp.Files, permCount = 4, isExpanded = false)
            ),
            PermissionsViewModel.ListItem.Group(
                PermissionsViewModel.GroupItem(group = APermGrp.Location, permCount = 3, isExpanded = false)
            ),
            PermissionsViewModel.ListItem.Group(
                PermissionsViewModel.GroupItem(group = APermGrp.Messaging, permCount = 6, isExpanded = false)
            ),
            PermissionsViewModel.ListItem.Group(
                PermissionsViewModel.GroupItem(group = APermGrp.Audio, permCount = 1, isExpanded = false)
            ),
            PermissionsViewModel.ListItem.Group(
                PermissionsViewModel.GroupItem(group = APermGrp.Sensors, permCount = 3, isExpanded = false)
            ),
        ),
        countPermissions = 42,
        countGroups = 11,
    )

    fun emptyReadyState() = PermissionsViewModel.State.Ready(
        listData = emptyList(),
        countPermissions = 0,
        countGroups = 0,
    )
}
