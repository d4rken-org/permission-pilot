package eu.darken.myperm.common.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import eu.darken.myperm.apps.core.features.BatteryOptimization
import eu.darken.myperm.apps.core.features.InternetAccess

@Entity(
    tableName = "snapshot_pkgs",
    primaryKeys = ["snapshotId", "pkgName", "userHandleId"],
    foreignKeys = [
        ForeignKey(
            entity = SnapshotEntity::class,
            parentColumns = ["snapshotId"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("snapshotId")],
)
data class SnapshotPkgEntity(
    val snapshotId: String,
    val pkgName: String,
    val userHandleId: Int,
    val pkgType: PkgType,
    val versionName: String?,
    val versionCode: Long,
    val sharedUserId: String?,
    val apiTargetLevel: Int?,
    val apiCompileLevel: Int?,
    val apiMinimumLevel: Int?,
    val isSystemApp: Boolean,
    val installedAt: Long?,
    val updatedAt: Long?,
    val internetAccess: InternetAccess,
    val batteryOptimization: BatteryOptimization,
    val installerPkgName: String?,
    val applicationFlags: Int,
    val cachedLabel: String?,
    val twinCount: Int = 0,
    val siblingCount: Int = 0,
    val hasAccessibilityServices: Boolean = false,
    val hasDeviceAdmin: Boolean = false,
    val allInstallerPkgNames: String? = null,
)
