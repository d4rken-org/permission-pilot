package eu.darken.myperm.apps.core

import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.apps.core.container.PrimaryProfilePkg
import eu.darken.myperm.apps.core.container.SecondaryProfilePkg
import eu.darken.myperm.apps.core.container.SecondaryUserPkg
import eu.darken.myperm.apps.core.container.UninstalledDataPkg
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.apps.core.manifest.ManifestHintEntity
import eu.darken.myperm.apps.core.manifest.ManifestHintScanner
import eu.darken.myperm.permissions.core.known.APerm
import eu.darken.myperm.common.room.entity.PkgType
import eu.darken.myperm.common.room.entity.SnapshotPkgDeclaredPermEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgPermEntity
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotMapper @Inject constructor() {

    fun toEntities(
        snapshotId: String,
        pkg: BasePkg,
        resolvedLabel: String,
    ): PkgEntities {
        val userHandleId = pkg.userHandle.hashCode()

        val pkgEntity = SnapshotPkgEntity(
            snapshotId = snapshotId,
            pkgName = pkg.id.pkgName,
            userHandleId = userHandleId,
            pkgType = pkg.toPkgType(),
            versionName = pkg.versionName,
            versionCode = pkg.versionCode,
            sharedUserId = pkg.sharedUserId,
            apiTargetLevel = pkg.apiTargetLevel,
            apiCompileLevel = pkg.apiCompileLevel,
            apiMinimumLevel = pkg.apiMinimumLevel,
            isSystemApp = pkg.isSystemApp,
            installedAt = pkg.installedAt?.toEpochMilli(),
            updatedAt = pkg.updatedAt?.toEpochMilli(),
            internetAccess = pkg.internetAccess,
            batteryOptimization = pkg.batteryOptimization,
            installerPkgName = pkg.installerInfo.installingPkg?.id?.pkgName,
            applicationFlags = pkg.applicationInfo?.flags ?: 0,
            cachedLabel = resolvedLabel,
            twinCount = pkg.twins.size,
            siblingCount = pkg.siblings.size,
            hasAccessibilityServices = pkg.accessibilityServices.isNotEmpty(),
            hasDeviceAdmin = pkg.requestedPermissions.any { it.id == APerm.BIND_DEVICE_ADMIN.id },
            allInstallerPkgNames = pkg.installerInfo.allInstallers
                .map { it.id.pkgName.value }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(","),
        )

        // Some apps have duplicate permissions in their manifest (e.g. Health Connect has 100+ duplicates)
        val permEntities = pkg.requestedPermissions
            .distinctBy { it.id.value }
            .map { perm ->
                SnapshotPkgPermEntity(
                    snapshotId = snapshotId,
                    pkgName = pkg.id.pkgName,
                    userHandleId = userHandleId,
                    permissionId = perm.id.value,
                    status = perm.status.name,
                )
            }

        val declaredPermEntities = pkg.declaredPermissions
            .distinctBy { it.name }
            .map { perm ->
                SnapshotPkgDeclaredPermEntity(
                    snapshotId = snapshotId,
                    pkgName = pkg.id.pkgName,
                    userHandleId = userHandleId,
                    permissionId = perm.name,
                    protectionLevel = perm.protectionLevel,
                )
            }

        return PkgEntities(pkgEntity, permEntities, declaredPermEntities)
    }

    fun toAppInfo(
        pkgEntity: SnapshotPkgEntity,
        permEntities: List<SnapshotPkgPermEntity>,
        declaredPermCount: Int,
        manifestHint: ManifestHintEntity? = null,
    ): AppInfo = AppInfo(
        pkgName = pkgEntity.pkgName,
        userHandleId = pkgEntity.userHandleId,
        label = pkgEntity.cachedLabel ?: pkgEntity.pkgName.value,
        versionName = pkgEntity.versionName,
        versionCode = pkgEntity.versionCode,
        isSystemApp = pkgEntity.isSystemApp,
        installerPkgName = pkgEntity.installerPkgName,
        apiTargetLevel = pkgEntity.apiTargetLevel,
        apiCompileLevel = pkgEntity.apiCompileLevel,
        apiMinimumLevel = pkgEntity.apiMinimumLevel,
        internetAccess = pkgEntity.internetAccess,
        batteryOptimization = pkgEntity.batteryOptimization,
        installedAt = pkgEntity.installedAt?.let { Instant.ofEpochMilli(it) },
        updatedAt = pkgEntity.updatedAt?.let { Instant.ofEpochMilli(it) },
        requestedPermissions = permEntities.map { perm ->
            PermissionUse(
                permissionId = perm.permissionId,
                status = try {
                    UsesPermission.Status.valueOf(perm.status)
                } catch (_: IllegalArgumentException) {
                    UsesPermission.Status.UNKNOWN
                },
            )
        },
        declaredPermissionCount = declaredPermCount,
        pkgType = pkgEntity.pkgType,
        twinCount = pkgEntity.twinCount,
        siblingCount = pkgEntity.siblingCount,
        hasAccessibilityServices = pkgEntity.hasAccessibilityServices,
        hasDeviceAdmin = pkgEntity.hasDeviceAdmin,
        allInstallerPkgNames = pkgEntity.allInstallerPkgNames
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.map { Pkg.Name(it) }
            ?: emptyList(),
        sharedUserId = pkgEntity.sharedUserId,
        hasManifestFlags = manifestHint?.let { ManifestHintScanner.hasFlaggedIssues(it) },
    )

    data class PkgEntities(
        val pkg: SnapshotPkgEntity,
        val permissions: List<SnapshotPkgPermEntity>,
        val declaredPermissions: List<SnapshotPkgDeclaredPermEntity>,
    )

    companion object {
        private fun BasePkg.toPkgType(): PkgType = when (this) {
            is PrimaryProfilePkg -> PkgType.PRIMARY
            is SecondaryProfilePkg -> PkgType.SECONDARY_PROFILE
            is SecondaryUserPkg -> PkgType.SECONDARY_USER
            is UninstalledDataPkg -> PkgType.UNINSTALLED
        }
    }
}
