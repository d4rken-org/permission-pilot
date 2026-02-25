package eu.darken.myperm.common.navigation

import kotlinx.serialization.Serializable

object Nav {
    sealed interface Main : NavigationDestination {
        @Serializable
        data object Onboarding : Main
    }

    sealed interface Tab : NavigationDestination {
        @Serializable
        data object Overview : Tab

        @Serializable
        data object Apps : Tab

        @Serializable
        data object Permissions : Tab
    }

    sealed interface Details : NavigationDestination {
        @Serializable
        data class AppDetails(
            val pkgName: String,
            val userHandle: Int,
            val appLabel: String? = null,
        ) : Details

        @Serializable
        data class PermissionDetails(
            val permissionId: String,
            val permLabel: String? = null,
        ) : Details
    }

    sealed interface Settings : NavigationDestination {
        @Serializable
        data object Index : Settings

        @Serializable
        data object General : Settings

        @Serializable
        data object Support : Settings

        @Serializable
        data object Acknowledgements : Settings
    }
}
