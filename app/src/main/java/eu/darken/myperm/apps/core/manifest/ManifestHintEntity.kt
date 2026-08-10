package eu.darken.myperm.apps.core.manifest

import androidx.room.ColumnInfo
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
    // Default 0 marks rows written before versioning existed — always below the
    // current ManifestHintScanner.SCANNER_VERSION, so they get re-scanned.
    @ColumnInfo(defaultValue = "0") val scannerVersion: Int = 0,
) {
    val totalQueryCount: Int get() = packageQueryCount + intentQueryCount + providerQueryCount
}
