package eu.darken.myperm.apps.core.manifest

import androidx.room.Entity

@Entity(tableName = "manifest_hints", primaryKeys = ["pkgName"])
data class ManifestHintEntity(
    val pkgName: String,
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
