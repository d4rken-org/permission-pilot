package eu.darken.myperm.permissions.ui.list

sealed class PermissionListEvent {
    data class ShowFilterDialog(val options: PermsFilterOptions) : PermissionListEvent()

    data class ShowSortDialog(val options: PermsSortOptions) : PermissionListEvent()
}
