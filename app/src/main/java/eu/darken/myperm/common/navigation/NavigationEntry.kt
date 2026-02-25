package eu.darken.myperm.common.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

interface NavigationEntry {
    fun EntryProviderScope<NavKey>.setup()
}
