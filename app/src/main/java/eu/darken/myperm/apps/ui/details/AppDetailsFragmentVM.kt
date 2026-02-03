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
import eu.darken.myperm.apps.core.apps
import eu.darken.myperm.apps.core.features.ReadableApk
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.apps.ui.details.items.AppOverviewVH
import eu.darken.myperm.apps.ui.details.items.AppSiblingsVH
import eu.darken.myperm.apps.ui.details.items.AppTwinsVH
import eu.darken.myperm.apps.ui.details.items.UnknownPermissionVH
import eu.darken.myperm.apps.ui.details.items.UsesPermissionVH
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
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
import eu.darken.myperm.permissions.core.permissions
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.coroutines.flow.combine
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
    appStoreTool: AppStoreTool,
    private val generalSettings: GeneralSettings
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    private val navArgs: AppDetailsFragmentArgs by handle.navArgs()
    private val filterOptions = generalSettings.appDetailsFilterOptions.flow

    val events = SingleLiveEvent<AppDetailsEvents>()

    data class Details(
        val label: String,
        val app: Pkg? = null,
        val items: List<AppDetailsAdapter.Item> = emptyList(),
        val isEmptyDueToFilter: Boolean = false,
    )

    val details: LiveData<Details> = combine(
        appRepo.apps.map { apps -> apps.single { it.id == navArgs.appId } },
        filterOptions
    ) { app, filterOpts ->
        val infoItems = mutableListOf<AppDetailsAdapter.Item>()
        val permissions = permissionRepo.permissions.first()

        AppOverviewVH.Item(
            app = app,
            onOpenApp = {
                val intent = context.packageManager.getLaunchIntentForPackage(it.packageName)
                if (intent != null) {
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        log(TAG, ERROR) { "Launch intent failed for $app: ${e.asLog()}" }
                        errorEvents.postValue(e)
                    }
                } else {
                    log(TAG, ERROR) { "No launch intent available for ${it.packageName}" }
                    errorEvents.postValue(IllegalStateException("No launch intent available"))
                }
            },
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

        val allPermissions = (app as? ReadableApk)?.requestedPermissions
            ?.map { usesPerm ->
                val basePerm = permissions.singleOrNull { it.id == usesPerm.id }
                    ?: throw IllegalArgumentException("Can't find $usesPerm")
                usesPerm to basePerm
            }
            ?: emptyList()

        val filteredPermissions = allPermissions
            .filter { (usesPerm, basePerm) ->
                // Always show UNKNOWN status permissions
                usesPerm.status == UsesPermission.Status.UNKNOWN ||
                        filterOpts.keys.any { filter -> filter.matches(usesPerm, basePerm) }
            }
            .sortedWith(
                compareByDescending<Pair<UsesPermission, BasePermission>> { (_, basePerm) ->
                    when (basePerm) {
                        is DeclaredPermission -> 2
                        is ExtraPermission -> 1
                        is UnknownPermission -> 0
                    }
                }.thenBy { (usesPerm, _) ->
                    usesPerm.status.ordinal
                }.thenBy { (_, basePerm) ->
                    when {
                        basePerm.tags.contains(RuntimeGrant) -> 2
                        basePerm.tags.contains(SpecialAccess) -> 1
                        else -> 0
                    }
                }
            )
            .map { (usesPerm, basePerm) ->
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
                                AppDetailsEvents.PermissionEvent(it.permission.getAction(context, app))
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

        infoItems.addAll(filteredPermissions)

        // Check if permissions were filtered out (app has permissions but none match filter)
        val isEmptyDueToFilter = allPermissions.isNotEmpty() && filteredPermissions.isEmpty()

        Details(
            app = app,
            label = navArgs.appLabel ?: app.getLabel(context) ?: app.id.toString(),
            items = infoItems,
            isEmptyDueToFilter = isEmptyDueToFilter
        )
    }
        .onStart { navArgs.appLabel?.let { emit(Details(label = it)) } }
        .asLiveData2()

    fun onGoSettings() {
        val pkg = details.value?.app ?: return
        events.postValue(AppDetailsEvents.ShowAppSystemDetails(pkg))
    }

    fun showFilterDialog() {
        events.postValue(AppDetailsEvents.ShowFilterDialog(generalSettings.appDetailsFilterOptions.value))
    }

    fun updateFilterOptions(action: (AppDetailsFilterOptions) -> AppDetailsFilterOptions) {
        generalSettings.appDetailsFilterOptions.update { action(it) }
    }
}