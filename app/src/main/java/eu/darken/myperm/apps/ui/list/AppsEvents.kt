package eu.darken.myperm.apps.ui.list

sealed class AppsEvents {
    data class ShowFilterDialog(
        val options: FilterOptions
    ) : AppsEvents()

    data class ShowSortDialog(
        val options: SortOptions
    ) : AppsEvents()
}
