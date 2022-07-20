package eu.darken.myperm.permissions.ui.list

sealed class PermissionsEvents {
    data class ShowFilterDialog(
        val options: PermsFilterOptions
    ) : PermissionsEvents()

    data class ShowSortDialog(
        val options: PermsSortOptions
    ) : PermissionsEvents()
}
