package eu.darken.myperm.common.room.snapshot

import eu.darken.myperm.common.room.entity.SnapshotPkgDeclaredPermEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgPermEntity

data class PkgSnapshotData(
    val pkg: SnapshotPkgEntity,
    val permissions: List<SnapshotPkgPermEntity>,
    val declaredPermissions: List<SnapshotPkgDeclaredPermEntity>,
)
