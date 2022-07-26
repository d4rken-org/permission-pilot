package eu.darken.myperm.permissions.core.container

import android.content.pm.PermissionInfo
import androidx.annotation.StringRes
import androidx.core.content.pm.PermissionInfoCompat
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.features.HasApkData
import eu.darken.myperm.apps.core.features.requestsPermission
import eu.darken.myperm.permissions.core.Permission


data class DeclaredPermission(
    val permissionInfo: PermissionInfo,
    override val requestingPkgs: List<HasApkData> = emptyList(),
    override val declaringPkgs: Collection<HasApkData> = emptyList(),
) : BasePermission() {

    override val id: Permission.Id
        get() = Permission.Id(permissionInfo.name)

    override val grantingPkgs: Collection<HasApkData> by lazy {
        requestingPkgs
            .filter { it.requestsPermission(this) }
            .filter { it.getPermission(id)?.isGranted == true }
    }

    enum class ProtectionType(
        @StringRes val labelRes: Int,
    ) {
        NORMAL(R.string.permissions_protection_type_normal_label),
        DANGEROUS(R.string.permissions_protection_type_dangerous_label),
        SIGNATURE(R.string.permissions_protection_type_signature_label),
        SIGNATURE_OR_SYSTEM(R.string.permissions_protection_type_signatureorsystem_label),
        INTERNAL(R.string.permissions_protection_type_internal_label),
        UNKNOWN(R.string.permissions_protection_type_unknown_label),
        ;
    }

    val protectionType: ProtectionType
        get() = when (PermissionInfoCompat.getProtection(permissionInfo)) {
            PermissionInfo.PROTECTION_NORMAL -> ProtectionType.NORMAL
            PermissionInfo.PROTECTION_DANGEROUS -> ProtectionType.DANGEROUS
            PermissionInfo.PROTECTION_SIGNATURE -> ProtectionType.SIGNATURE
            PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM -> ProtectionType.SIGNATURE_OR_SYSTEM
            PermissionInfo.PROTECTION_INTERNAL -> ProtectionType.INTERNAL
            else -> ProtectionType.UNKNOWN
        }

    enum class ProtectionFlag(
        val flag: Int,
    ) {
        PRIVILEGED(flag = PermissionInfo.PROTECTION_FLAG_PRIVILEGED),
        SYSTEM(flag = PermissionInfo.PROTECTION_FLAG_SYSTEM),
        DEVELOPMENT(flag = PermissionInfo.PROTECTION_FLAG_DEVELOPMENT),
        APPOP(flag = PermissionInfo.PROTECTION_FLAG_APPOP),
        PRE23(flag = PermissionInfo.PROTECTION_FLAG_PRE23),
        INSTALLER(flag = PermissionInfo.PROTECTION_FLAG_INSTALLER),
        VERIFIER(flag = PermissionInfo.PROTECTION_FLAG_VERIFIER),
        PREINSTALLED(flag = PermissionInfo.PROTECTION_FLAG_PREINSTALLED),
        SETUP(flag = PermissionInfo.PROTECTION_FLAG_SETUP),
        INSTANT(flag = PermissionInfo.PROTECTION_FLAG_INSTANT),
        RUNTIME_ONLY(flag = PermissionInfo.PROTECTION_FLAG_RUNTIME_ONLY),
        OEM(flag = 0x4000),
        VENDOR_PRIVILEGED(flag = 0x8000),
        SYSTEM_TEXT_CLASSIFIER(flag = 0x10000),
        DOCUMENTER(flag = 0x40000),
        CONFIGURATOR(flag = 0x80000),
        INCIDENT_REPORT_APPROVER(flag = 0x100000),
        APP_PREDICTOR(flag = 0x200000),
        COMPANION(flag = 0x800000),
        RETAIL_DEMO(flag = 0x1000000),
        RECENTS(flag = 0x2000000),
        ROLE(flag = 0x4000000),
        KNOWN_SIGNER(flag = 0x8000000)
        ;
    }

    val protectionFlags: Set<ProtectionFlag> by lazy {
        ProtectionFlag.values().filter {
            it.flag and PermissionInfoCompat.getProtectionFlags(permissionInfo) > 0
        }.toSet()
    }
}