package eu.darken.myperm.common.navigation

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@InstallIn(SingletonComponent::class)
@Module
abstract class NavigationModule {
    @Multibinds
    abstract fun navigationEntries(): Set<NavigationEntry>
}
