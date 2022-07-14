package eu.darken.myperm.permissions.ui.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.navigation.navArgs
import eu.darken.myperm.common.uix.ViewModel3
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.types.BasePermission
import eu.darken.myperm.permissions.core.types.DeclaredPermission
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

            when (perm) {
                is DeclaredPermission -> PermissionOverviewVH.Item(
                    permission = perm
                ).run { infoItems.add(this) }
            }

            Details(
                perm = perm,
                label = perm.label ?: perm.id,
                items = infoItems,
            )
        }
        .asLiveData2()
}