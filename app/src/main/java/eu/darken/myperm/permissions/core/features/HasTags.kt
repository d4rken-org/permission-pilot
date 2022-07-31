package eu.darken.myperm.permissions.core.features

interface HasTags {
    val tags: Set<Tag>
        get() = emptySet()

    interface Tag
}