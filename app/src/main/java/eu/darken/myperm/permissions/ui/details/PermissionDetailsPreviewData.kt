package eu.darken.myperm.permissions.ui.details

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.permissions.core.ProtectionType
import eu.darken.myperm.permissions.core.features.ManifestDoc
import eu.darken.myperm.permissions.core.features.RuntimeGrant

internal object PermissionDetailsPreviewData {

    fun loadedState() = PermissionDetailsViewModel.State(
        label = "CAMERA",
        permissionId = "android.permission.CAMERA",
        permission = null,
        description = "Camera",
        fullDescription = "Required to be able to access the camera device.",
        protectionType = ProtectionType.DANGEROUS,
        protectionFlags = emptyList(),
        tags = listOf(RuntimeGrant, ManifestDoc),
        grantedUserCount = 12,
        totalUserCount = 45,
        grantedSystemCount = 3,
        totalSystemCount = 8,
        declaringApps = listOf(
            PermissionDetailsViewModel.DeclaringAppItem(
                pkgName = "android",
                pkg = Pkg.Container(Pkg.Id("android")),
                label = "Android System",
                isSystemApp = true,
                userHandle = 0,
            ),
        ),
        requestingApps = listOf(
            PermissionDetailsViewModel.RequestingAppItem(
                pkgName = "com.google.chrome",
                pkg = Pkg.Container(Pkg.Id("com.google.chrome")),
                label = "Chrome",
                isSystemApp = false,
                status = UsesPermission.Status.GRANTED,
                userHandle = 0,
            ),
            PermissionDetailsViewModel.RequestingAppItem(
                pkgName = "org.mozilla.firefox",
                pkg = Pkg.Container(Pkg.Id("org.mozilla.firefox")),
                label = "Firefox",
                isSystemApp = false,
                status = UsesPermission.Status.DENIED,
                userHandle = 0,
            ),
            PermissionDetailsViewModel.RequestingAppItem(
                pkgName = "com.android.camera",
                pkg = Pkg.Container(Pkg.Id("com.android.camera")),
                label = "Camera",
                isSystemApp = true,
                status = UsesPermission.Status.GRANTED,
                userHandle = 0,
            ),
        ),
        isLoading = false,
    )

    fun loadingState() = PermissionDetailsViewModel.State(
        label = "CAMERA",
        permissionId = "android.permission.CAMERA",
        isLoading = true,
    )
}
