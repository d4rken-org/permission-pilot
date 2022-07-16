package eu.darken.myperm.common.lists.differ

import eu.darken.myperm.common.lists.ListItem

interface DifferItem : ListItem {
    val stableId: Long

    val payloadProvider: ((DifferItem, DifferItem) -> DifferItem?)?
        get() = { old, new -> new }
}