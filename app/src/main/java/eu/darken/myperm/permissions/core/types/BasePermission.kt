package eu.darken.myperm.permissions.core.types

import eu.darken.myperm.apps.core.types.BaseApp
import eu.darken.myperm.permissions.core.Permission

sealed class BasePermission : Permission {

    abstract val label: String?
    abstract val description: String?
    abstract val requestingApps: Collection<BaseApp>
    abstract val grantedApps: Collection<BaseApp>
    abstract val declaringApps: Collection<BaseApp>

    abstract val isAospPermission: Boolean

    val riskClassifier: RiskClassifier
        get() = RiskClassifier.HIGH

    enum class RiskClassifier {
        HIGH,
        MEDIUM,
        LOW
    }
}