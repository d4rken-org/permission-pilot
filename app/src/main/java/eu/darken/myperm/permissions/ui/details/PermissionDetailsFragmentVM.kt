package eu.darken.myperm.permissions.ui.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.navigation.navArgs
import eu.darken.myperm.common.uix.ViewModel3
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.types.BasePermission
import eu.darken.myperm.permissions.ui.details.items.AppDeclaringPermissionVH
import eu.darken.myperm.permissions.ui.details.items.AppRequestingPermissionVH
import eu.darken.myperm.permissions.ui.details.items.PermissionOverviewVH
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class PermissionDetailsFragmentVM @Inject constructor(
    @Suppress("UNUSED_PARAMETER") handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val permissionsRepo: PermissionRepo,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    private val navArgs: PermissionDetailsFragmentArgs by handle.navArgs()

    data class Details(
        val perm: BasePermission,
        val label: String,
        val items: List<PermissionDetailsAdapter.Item>,
    )

    val details: LiveData<Details> = permissionsRepo.permissions
        .map { perms -> perms.single { it.id == navArgs.permissionId } }
        .map { perm ->
            val infoItems = mutableListOf<PermissionDetailsAdapter.Item>()

            PermissionOverviewVH.Item(
                permission = perm
            ).run { infoItems.add(this) }


            perm.declaringApps.map { app ->
                AppDeclaringPermissionVH.Item(
                    permission = perm,
                    app = app,
                    onItemClicked = {
                        PermissionDetailsFragmentDirections
                            .actionPermissionDetailsFragmentToAppDetailsFragment(it.app.id)
                            .navigate()
                    }
                )

            }.run { infoItems.addAll(this) }

            perm.requestingApps.map { app ->
                AppRequestingPermissionVH.Item(
                    permission = perm,
                    app = app,
                    onItemClicked = {
                        PermissionDetailsFragmentDirections
                            .actionPermissionDetailsFragmentToAppDetailsFragment(it.app.id)
                            .navigate()
                    }
                )
            }.run { infoItems.addAll(this) }

            Details(
                perm = perm,
                label = perm.label ?: perm.id.value,
                items = infoItems,
            )
        }
        .asLiveData2()
}