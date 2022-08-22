package eu.darken.myperm.permissions.ui.details

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.features.getPermissionUses
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.livedata.SingleLiveEvent
import eu.darken.myperm.common.navigation.navArgs
import eu.darken.myperm.common.uix.ViewModel3
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.ui.details.items.AppDeclaringPermissionVH
import eu.darken.myperm.permissions.ui.details.items.AppRequestingPermissionVH
import eu.darken.myperm.permissions.ui.details.items.PermissionOverviewVH
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class PermissionDetailsFragmentVM @Inject constructor(
    @Suppress("UNUSED_PARAMETER") handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    private val permissionsRepo: PermissionRepo,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    private val navArgs: PermissionDetailsFragmentArgs by handle.navArgs()

    val events = SingleLiveEvent<PermissionDetailsEvents>()

    data class Details(
        val label: String,
        val perm: BasePermission? = null,
        val items: List<PermissionDetailsAdapter.Item> = emptyList(),
    )

    val details: LiveData<Details> = permissionsRepo.permissions
        .map { perms -> perms.single { it.id == navArgs.permissionId } }
        .map { perm ->
            val infoItems = mutableListOf<PermissionDetailsAdapter.Item>()

            PermissionOverviewVH.Item(
                permission = perm,
                onIconClick = {
                    events.postValue(PermissionDetailsEvents.PermissionEvent(it.permission.getAction(context)))
                }
            ).run { infoItems.add(this) }


            perm.declaringPkgs.map { app ->
                AppDeclaringPermissionVH.Item(
                    permission = perm,
                    app = app,
                    onItemClicked = {
                        PermissionDetailsFragmentDirections
                            .actionPermissionDetailsFragmentToAppDetailsFragment(it.app.id, it.app.getLabel(context))
                            .navigate()
                    }
                )

            }.run { infoItems.addAll(this) }

            perm.requestingPkgs
                .map { app ->
                    AppRequestingPermissionVH.Item(
                        permission = perm,
                        app = app,
                        status = app.getPermissionUses(perm.id).status,
                        onItemClicked = {
                            PermissionDetailsFragmentDirections.actionPermissionDetailsFragmentToAppDetailsFragment(
                                it.app.id,
                                it.app.getLabel(context)
                            ).navigate()
                        },
                        onIconClicked = {
                            events.postValue(
                                PermissionDetailsEvents.PermissionEvent(it.permission.getAction(context, app))
                            )
                        }
                    )
                }
                .sortedWith(compareBy<AppRequestingPermissionVH.Item> { it.status }.thenBy { it.app.isSystemApp })
                .run { infoItems.addAll(this) }

            Details(
                perm = perm,
                label = perm.id.value.split(".").lastOrNull() ?: perm.id.value,
                items = infoItems,
            )
        }
        .onStart { navArgs.permissionLabel?.let { Details(label = it) } }
        .asLiveData2()
}