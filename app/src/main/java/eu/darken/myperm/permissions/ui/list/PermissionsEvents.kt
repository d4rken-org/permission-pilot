package eu.darken.myperm.permissions.ui.list

sealed class PermissionsEvents {
    data class ShowFilterDialog(
        val options: FilterOptions
    ) : PermissionsEvents()

    data class ShowSortDialog(
        val options: SortOptions
    ) : PermissionsEvents()
}
