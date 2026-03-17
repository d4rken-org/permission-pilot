package eu.darken.myperm.export.core

data class ResolvedPermissionInfo(
    val id: String,
    val label: String?,
    val description: String?,
    val type: PermissionType,
    val protectionType: String?,
    val requestingAppCount: Int,
    val grantedAppCount: Int,
    val requestingApps: List<ResolvedAppRef>,
)

data class ResolvedAppRef(
    val pkgName: String,
    val label: String,
    val isGranted: Boolean,
)

enum class PermissionType {
    RUNTIME,
    INSTALL_TIME,
    SPECIAL,
    UNKNOWN,
}
