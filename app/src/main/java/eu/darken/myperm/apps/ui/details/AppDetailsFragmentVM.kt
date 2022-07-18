package eu.darken.myperm.apps.ui.details

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.container.NormalApp
import eu.darken.myperm.apps.core.features.ApkPkg
import eu.darken.myperm.apps.core.features.AppStore
import eu.darken.myperm.apps.ui.details.items.AppOverviewVH
import eu.darken.myperm.apps.ui.details.items.AppSiblingsVH
import eu.darken.myperm.apps.ui.details.items.DeclaredPermissionVH
import eu.darken.myperm.apps.ui.details.items.UnknownPermissionVH
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.livedata.SingleLiveEvent
import eu.darken.myperm.common.navigation.navArgs
import eu.darken.myperm.common.uix.ViewModel3
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.types.DeclaredPermission
import eu.darken.myperm.permissions.core.types.UnknownPermission
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class AppDetailsFragmentVM @Inject constructor(
    @Suppress("UNUSED_PARAMETER") handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    private val webpageTool: WebpageTool,
    appRepo: AppRepo,
    permissionRepo: PermissionRepo,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    private val navArgs: AppDetailsFragmentArgs by handle.navArgs()

    val events = SingleLiveEvent<AppDetailsEvents>()

    data class Details(
        val label: String,
        val app: ApkPkg? = null,
        val items: List<AppDetailsAdapter.Item> = emptyList(),
    )

    val details: LiveData<Details> = appRepo.apps
        .map { apps -> apps.single { it.id == navArgs.appId } }
        .map { app ->
            val infoItems = mutableListOf<AppDetailsAdapter.Item>()
            val permissions = permissionRepo.permissions.first()

            when (app) {
                is NormalApp -> {
                    AppOverviewVH.Item(
                        app = app,
                        onGoToSettings = { events.postValue(AppDetailsEvents.ShowAppSystemDetails(it)) },
                        onInstallerClicked = { installer ->
                            if (installer is AppStore) {
                                installer
                                    .urlGenerator?.invoke(app.id)
                                    ?.let { webpageTool.open(it) }
                            } else {
                                AppDetailsFragmentDirections.toSelf(
                                    appId = installer.id,
                                ).navigate()
                            }
                        }
                    ).run { infoItems.add(this) }

                    if (app.sharedUserId != null || app.siblings.isNotEmpty()) {
                        AppSiblingsVH.Item(
                            app,
                            onSiblingClicked = {
                                AppDetailsFragmentDirections.toSelf(
                                    appId = it.id,
                                    appLabel = it.getLabel(context),
                                ).navigate()
                            }
                        ).run { infoItems.add(this) }
                    }
                }
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
                                .actionAppDetailsFragmentToPermissionDetailsFragment(
                                    permissionId = it.permission.id,
                                    permissionLabel = it.permission.getLabel(context)
                                )
                                .navigate()
                        }
                    )
                    is UnknownPermission -> UnknownPermissionVH.Item(
                        appPermission = appPermission,
                        permission = permission,
                        onItemClicked = {
                            AppDetailsFragmentDirections
                                .actionAppDetailsFragmentToPermissionDetailsFragment(
                                    permissionId = it.permission.id,
                                    permissionLabel = it.permission.getLabel(context)
                                )
                                .navigate()
                        }
                    )
                }
                infoItems.add(permItem)
            }

            Details(
                app = app,
                label = navArgs.appLabel ?: app.getLabel(context) ?: app.id.toString(),
                items = infoItems
            )
        }
        .onStart { navArgs.appLabel?.let { emit(Details(label = it)) } }
        .asLiveData2()

    fun onGoSettings() {
        val pkg = details.value?.app ?: return
        events.postValue(AppDetailsEvents.ShowAppSystemDetails(pkg))
    }
}