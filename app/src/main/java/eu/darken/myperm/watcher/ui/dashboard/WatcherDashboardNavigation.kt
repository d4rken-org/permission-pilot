package eu.darken.myperm.watcher.ui.dashboard

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

class WatcherDashboardNavigation @Inject constructor() : NavigationEntry {
    override fun EntryProviderScope<NavKey>.setup() {
        entry<Nav.Tab.Watcher> { WatcherDashboardScreenHost() }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class Mod {
        @Binds
        @IntoSet
        abstract fun bind(entry: WatcherDashboardNavigation): NavigationEntry
    }
}
