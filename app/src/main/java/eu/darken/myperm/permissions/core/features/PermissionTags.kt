package eu.darken.myperm.permissions.core.features

sealed class PermissionTag

object ManifestDoc : PermissionTag()

object RuntimeGrant : PermissionTag()

object InstallTimeGrant : PermissionTag()

object SpecialAccess : PermissionTag()

object Highlighted : PermissionTag()

object NotNormalPerm : PermissionTag()