package eu.darken.myperm.apps.ui.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.types.BaseApp
import eu.darken.myperm.apps.core.types.NormalApp
import eu.darken.myperm.apps.ui.details.items.AppOverviewVH
import eu.darken.myperm.apps.ui.details.items.DeclaredPermissionVH
import eu.darken.myperm.apps.ui.details.items.UnknownPermissionVH
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.navigation.navArgs
import eu.darken.myperm.common.uix.ViewModel3
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.types.DeclaredPermission
import eu.darken.myperm.permissions.core.types.UnknownPermission
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class AppDetailsFragmentVM @Inject constructor(
    @Suppress("UNUSED_PARAMETER") handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    appRepo: AppRepo,
    permissionRepo: PermissionRepo,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    private val navArgs: AppDetailsFragmentArgs by handle.navArgs()

    data class Details(
        val app: BaseApp,
        val label: String,
        val items: List<AppDetailsAdapter.Item>,
    )

    val details: LiveData<Details> = appRepo.apps
        .map { apps -> apps.single { it.id == navArgs.appId } }
        .map { app ->
            val infoItems = mutableListOf<AppDetailsAdapter.Item>()
            val permissions = permissionRepo.permissions.first()

            when (app) {
                is NormalApp -> AppOverviewVH.Item(
                    app = app
                ).run { infoItems.add(this) }
            }

            app.requestedPermissions.forEach { appPermission ->
                val permission = permissions.singleOrNull { it.id == appPermission.id }
                    ?: throw IllegalArgumentException("Can't find $appPermission")

                val permItem: AppDetailsAdapter.Item = when (permission) {
                    is DeclaredPermission -> DeclaredPermissionVH.Item(
                        appPermission = appPermission,
                        permission = permission,
                        onItemClicked = {
                            AppDetailsFragmentDirections
                                .actionAppDetailsFragmentToPermissionDetailsFragment(it.permission.id)
                                .navigate()
                        }
                    )
                    is UnknownPermission -> UnknownPermissionVH.Item(
                        appPermission = appPermission,
                        permission = permission,
                        onItemClicked = {
                            AppDetailsFragmentDirections
                                .actionAppDetailsFragmentToPermissionDetailsFragment(it.permission.id)
                                .navigate()
                        }
                    )
                }
                infoItems.add(permItem)
            }

            Details(
                app = app,
                label = app.label ?: app.packageName,
                items = infoItems
            )
        }
        .asLiveData2()
}