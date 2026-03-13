package eu.darken.myperm.watcher.core

import kotlinx.serialization.Serializable

@Serializable
enum class WatcherScope {
    ALL,
    NON_SYSTEM,
}
