package eu.darken.myperm.apps.ui.details

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.AppStoreTool
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.ReadableApk
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.apps.ui.details.items.*
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.livedata.SingleLiveEvent
import eu.darken.myperm.common.navigation.navArgs
import eu.darken.myperm.common.uix.ViewModel3
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.core.container.DeclaredPermission
import eu.darken.myperm.permissions.core.container.ExtraPermission
import eu.darken.myperm.permissions.core.container.UnknownPermission
import eu.darken.myperm.permissions.core.features.RuntimeGrant
import eu.darken.myperm.permissions.core.features.SpecialAccess
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
    appStoreTool: AppStoreTool
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    private val navArgs: AppDetailsFragmentArgs by handle.navArgs()

    val events = SingleLiveEvent<AppDetailsEvents>()

    data class Details(
        val label: String,
        val app: Pkg? = null,
        val items: List<AppDetailsAdapter.Item> = emptyList(),
    )

    val details: LiveData<Details> = appRepo.apps
        .map { apps -> apps.single { it.id == navArgs.appId } }
        .map { app ->
            val infoItems = mutableListOf<AppDetailsAdapter.Item>()
            val permissions = permissionRepo.permissions.first()

            AppOverviewVH.Item(
                app = app,
                onGoToSettings = { events.postValue(AppDetailsEvents.ShowAppSystemDetails(it)) },
                onInstallerTextClicked = { installer ->
                    AppDetailsFragmentDirections.toSelf(appId = installer.id).navigate()
                },
                onInstallerIconClicked = { installer -> appStoreTool.openAppStoreFor(app, installer) }
            ).run { infoItems.add(this) }

            if (app.twins.isNotEmpty()) {
                AppTwinsVH.Item(
                    app,
                    onTwinClicked = {
                        AppDetailsFragmentDirections.toSelf(it.id, it.getLabel(context)).navigate()
                    }
                ).run { infoItems.add(this) }
            }

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

            (app as? ReadableApk)?.requestedPermissions
                ?.map { usesPerm ->
                    val basePerm = permissions.singleOrNull { it.id == usesPerm.id }
                        ?: throw IllegalArgumentException("Can't find $usesPerm")
                    usesPerm to basePerm
                }
                ?.sortedWith(
                    compareByDescending<Pair<UsesPermission, BasePermission>> { (usesPerm, basePerm) ->
                        when (basePerm) {
                            is DeclaredPermission -> 2
                            is ExtraPermission -> 1
                            is UnknownPermission -> 0
                        }
                    }.thenBy { (usesPerm, basePerm) ->
                        usesPerm.status.ordinal
                    }.thenBy { (usesPerm, basePerm) ->
                        when {
                            basePerm.tags.contains(RuntimeGrant) -> 2
                            basePerm.tags.contains(SpecialAccess) -> 1
                            else -> 0
                        }
                    }
                )
                ?.map { (usesPerm, basePerm) ->
                    when (basePerm) {
                        is DeclaredPermission, is ExtraPermission -> UsesPermissionVH.Item(
                            pkg = app,
                            appPermission = usesPerm,
                            permission = basePerm,
                            onItemClicked = {
                                AppDetailsFragmentDirections.actionAppDetailsFragmentToPermissionDetailsFragment(
                                    permissionId = it.permission.id,
                                    permissionLabel = it.permission.getLabel(context)
                                ).navigate()
                            },
                            onTogglePermission = {
                                events.postValue(
                                    AppDetailsEvents.PermissionEvent(
                                        it.permission.getAction(
                                            context,
                                            app
                                        )
                                    )
                                )
                            }
                        )
                        is UnknownPermission -> UnknownPermissionVH.Item(
                            pkg = app,
                            appPermission = usesPerm,
                            permission = basePerm,
                            onItemClicked = {
                                AppDetailsFragmentDirections.actionAppDetailsFragmentToPermissionDetailsFragment(
                                    permissionId = it.permission.id,
                                    permissionLabel = it.permission.getLabel(context)
                                ).navigate()
                            }
                        )
                    }
                }
                ?.run { infoItems.addAll(this) }


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