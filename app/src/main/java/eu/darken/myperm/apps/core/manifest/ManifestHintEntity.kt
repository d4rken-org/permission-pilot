package eu.darken.myperm.apps.core.manifest

import androidx.room.Entity
import eu.darken.myperm.apps.core.Pkg

@Entity(tableName = "manifest_hints", primaryKeys = ["pkgName"])
data class ManifestHintEntity(
    val pkgName: Pkg.Name,
    val versionCode: Long,
    val lastUpdateTime: Long,
    val hasActionMainQuery: Boolean,
    val packageQueryCount: Int,
    val intentQueryCount: Int,
    val providerQueryCount: Int,
    val scannedAt: Long,
) {
    val totalQueryCount: Int get() = packageQueryCount + intentQueryCount + providerQueryCount
}
