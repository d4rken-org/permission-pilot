package eu.darken.myperm.permissions.ui.list

import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.container.UnknownPermission
import eu.darken.myperm.permissions.core.features.InstallTimeGrant
import eu.darken.myperm.permissions.core.features.ManifestDoc
import eu.darken.myperm.permissions.core.features.PermissionTag
import eu.darken.myperm.permissions.core.features.RuntimeGrant
import eu.darken.myperm.permissions.core.features.SpecialAccess
import eu.darken.myperm.permissions.core.known.APermGrp

internal object PermissionsPreviewData {

    private fun permItem(
        permName: String,
        label: String? = null,
        type: String = "declared",
        requestingCount: Int = 0,
        grantedCount: Int = 0,
        tags: Set<PermissionTag> = emptySet(),
    ) = PermissionsViewModel.PermItem(
        id = Permission.Id(permName),
        label = label,
        type = type,
        requestingCount = requestingCount,
        grantedCount = grantedCount,
        permission = UnknownPermission(
            id = Permission.Id(permName),
            tags = tags,
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
                PermissionsViewModel.GroupItem(group = APermGrp.Camera, permCount = 3, isExpanded = true)
            ),
            PermissionsViewModel.ListItem.Perm(
                permItem(
                    "android.permission.CAMERA",
                    "Camera access",
                    "declared",
                    requestingCount = 45,
                    grantedCount = 6,
                    tags = setOf(RuntimeGrant, ManifestDoc),
                )
            ),
            PermissionsViewModel.ListItem.Perm(
                permItem(
                    "android.permission.RECORD_VIDEO",
                    null,
                    "extra",
                    requestingCount = 12,
                    grantedCount = 3,
                    tags = emptySet(),
                )
            ),
            PermissionsViewModel.ListItem.Perm(
                permItem(
                    "android.permission.MANAGE_APP_OPS_MODES",
                    "Special camera access",
                    "declared",
                    requestingCount = 8,
                    grantedCount = 8,
                    tags = setOf(SpecialAccess),
                )
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

    fun activeFilterState() = PermissionsViewModel.State.Ready(
        listData = emptyList(),
        countPermissions = 0,
        countGroups = 0,
        filterOptions = PermsFilterOptions(filters = setOf(PermsFilterOptions.Filter.RUNTIME)),
        sortOptions = PermsSortOptions(mainSort = PermsSortOptions.Sort.APPS_GRANTED),
    )
}
