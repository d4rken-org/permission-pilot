package eu.darken.myperm.permissions.ui.details

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavigationEntry
import javax.inject.Inject

class PermissionDetailsNavigation @Inject constructor() : NavigationEntry {
    override fun EntryProviderScope<NavKey>.setup() {
        entry<Nav.Details.PermissionDetails> { key -> PermissionDetailsScreenHost(route = key) }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class Mod {
        @Binds
        @IntoSet
        abstract fun bind(entry: PermissionDetailsNavigation): NavigationEntry
    }
}
