package eu.darken.myperm.export.ui

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.SingleEventFlow
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.export.core.AppExportConfig
import eu.darken.myperm.export.core.ExportEngine
import eu.darken.myperm.export.core.ExportFormat
import eu.darken.myperm.export.core.ExportSelectionStore
import eu.darken.myperm.export.core.ExportWriter
import eu.darken.myperm.export.core.PermissionExportConfig
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.core.permissions
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    private val appRepo: AppRepo,
    private val permissionRepo: PermissionRepo,
    private val exportEngine: ExportEngine,
    private val exportWriter: ExportWriter,
    private val exportSelectionStore: ExportSelectionStore,
    upgradeRepo: UpgradeRepo,
) : ViewModel4(dispatcherProvider = dispatcherProvider) {

    val isPro: StateFlow<Boolean> = upgradeRepo.upgradeInfo
        .map { it.isPro }
        .stateIn(vmScope, SharingStarted.Eagerly, upgradeRepo.upgradeInfo.value.isPro)

    private var mode: ExportMode = ExportMode.Apps(emptyList())
    private var selectedIds: List<String> = emptyList()

    sealed class ExportMode {
        data class Apps(val ids: List<Pair<String, Int>>) : ExportMode()
        data class Permissions(val ids: List<String>) : ExportMode()
    }

    private val appConfig = MutableStateFlow(AppExportConfig())
    private val permConfig = MutableStateFlow(PermissionExportConfig())
    private val exportResult = MutableStateFlow<ExportResult?>(null)
    private val preview = MutableStateFlow<String?>(null)
    private val isPreviewLoading = MutableStateFlow(false)

    sealed class ExportResult {
        data object InProgress : ExportResult()
        data class Success(val uri: Uri) : ExportResult()
        data class Error(val throwable: Throwable) : ExportResult()
    }

    val events = SingleEventFlow<Event>()

    sealed interface Event {
        data class LaunchSaf(val mimeType: String, val fileName: String) : Event
    }

    data class State(
        val mode: ExportMode,
        val isPro: Boolean,
        val itemCount: Int,
        val effectiveItemCount: Int,
        val isFreeLimited: Boolean,
        val preview: String? = null,
        val isPreviewLoading: Boolean = false,
        val appConfig: AppExportConfig? = null,
        val permConfig: PermissionExportConfig? = null,
        val exportResult: ExportResult? = null,
    )

    val state: StateFlow<State?> = combine(
        appRepo.appData,
        permissionRepo.state,
        appConfig,
        permConfig,
        combine(exportResult, preview, isPreviewLoading) { r, p, l -> Triple(r, p, l) },
    ) { appDataState, permState, appCfg, permCfg, (result, previewText, previewLoading) ->
        val allApps = (appDataState as? AppRepo.AppDataState.Ready)?.apps ?: emptyList()
        val allPerms = (permState as? PermissionRepo.State.Ready)?.permissions ?: emptyList()
        val pro = isPro.value

        when (val m = mode) {
            is ExportMode.Apps -> {
                val selectedApps = resolveApps(allApps, m.ids)
                val effectiveCfg = enforceFreemium(appCfg, pro)
                val effectiveApps = if (!pro) selectedApps.take(FREE_EXPORT_LIMIT) else selectedApps

                State(
                    mode = m,
                    isPro = pro,
                    itemCount = selectedApps.size,
                    effectiveItemCount = effectiveApps.size,
                    isFreeLimited = !pro && selectedApps.size > FREE_EXPORT_LIMIT,
                    preview = previewText,
                    isPreviewLoading = previewLoading,
                    appConfig = effectiveCfg,
                    exportResult = result,
                )
            }

            is ExportMode.Permissions -> {
                val selectedPerms = resolvePermissions(allPerms, m.ids)
                val effectiveCfg = enforceFreemium(permCfg, pro)
                val effectivePerms = if (!pro) selectedPerms.take(FREE_EXPORT_LIMIT) else selectedPerms

                State(
                    mode = m,
                    isPro = pro,
                    itemCount = selectedPerms.size,
                    effectiveItemCount = effectivePerms.size,
                    isFreeLimited = !pro && selectedPerms.size > FREE_EXPORT_LIMIT,
                    preview = previewText,
                    isPreviewLoading = previewLoading,
                    permConfig = effectiveCfg,
                    exportResult = result,
                )
            }
        }
    }.asStateFlow()

    init {
        // Async preview generation: debounce config changes, run on IO
        @OptIn(FlowPreview::class)
        combine(appConfig, permConfig, appRepo.appData, permissionRepo.state) { appCfg, permCfg, appData, permState ->
            appCfg to permCfg to (appData to permState)
        }.debounce(200L).map {
            regeneratePreview()
        }.launchInViewModel()
    }

    private suspend fun regeneratePreview() {
        isPreviewLoading.value = true
        val result = withContext(dispatcherProvider.IO) {
            runCatching {
                val allApps = (appRepo.appData.first() as? AppRepo.AppDataState.Ready)?.apps ?: emptyList()
                val allPerms = (permissionRepo.state.first() as? PermissionRepo.State.Ready)?.permissions ?: emptyList()
                val pro = isPro.value

                when (val m = mode) {
                    is ExportMode.Apps -> {
                        val selectedApps = resolveApps(allApps, m.ids)
                        val effectiveApps = if (!pro) selectedApps.take(FREE_EXPORT_LIMIT) else selectedApps
                        val effectiveCfg = enforceFreemium(appConfig.value, pro)
                        exportEngine.previewApps(effectiveApps, allPerms, effectiveCfg)
                    }
                    is ExportMode.Permissions -> {
                        val selectedPerms = resolvePermissions(allPerms, m.ids)
                        val effectivePerms = if (!pro) selectedPerms.take(FREE_EXPORT_LIMIT) else selectedPerms
                        val effectiveCfg = enforceFreemium(permConfig.value, pro)
                        exportEngine.previewPermissions(effectivePerms, effectiveCfg)
                    }
                }
            }
        }
        preview.value = result.getOrElse { e ->
            log(TAG, WARN) { "Preview generation failed: $e" }
            null
        }
        isPreviewLoading.value = false
    }

    fun init(route: Nav.Export.Config) {
        selectedIds = exportSelectionStore.consume(route.token) ?: emptyList()
        mode = when (route.mode) {
            "apps" -> ExportMode.Apps(selectedIds.map { id ->
                val parts = id.split(":")
                parts[0] to parts[1].toInt()
            })

            "permissions" -> ExportMode.Permissions(selectedIds)
            else -> {
                log(TAG, WARN) { "Unknown export mode: ${route.mode}" }
                ExportMode.Apps(emptyList())
            }
        }
        log(TAG) { "Initialized with mode=$mode, ${selectedIds.size} items" }
    }

    fun updateAppConfig(transform: (AppExportConfig) -> AppExportConfig) {
        appConfig.value = transform(appConfig.value)
    }

    fun updatePermConfig(transform: (PermissionExportConfig) -> PermissionExportConfig) {
        permConfig.value = transform(permConfig.value)
    }

    fun startExport() {
        val format = when (mode) {
            is ExportMode.Apps -> appConfig.value.format
            is ExportMode.Permissions -> permConfig.value.format
        }
        val fileName = "permission-pilot-export.${format.extension}"
        events.tryEmit(Event.LaunchSaf(format.mimeType, fileName))
    }

    fun onSafResult(uri: Uri?) = launch {
        if (uri == null) return@launch // User cancelled

        exportResult.value = ExportResult.InProgress

        val allApps = (appRepo.appData.first() as? AppRepo.AppDataState.Ready)?.apps ?: emptyList()
        val allPerms = permissionRepo.permissions.first()
        val pro = isPro.value

        val result = when (val m = mode) {
            is ExportMode.Apps -> {
                val apps = resolveApps(allApps, m.ids).let {
                    if (!pro) it.take(FREE_EXPORT_LIMIT) else it
                }
                val cfg = enforceFreemium(appConfig.value, pro)
                exportEngine.exportApps(apps, allPerms, cfg)
            }

            is ExportMode.Permissions -> {
                val perms = resolvePermissions(allPerms, m.ids).let {
                    if (!pro) it.take(FREE_EXPORT_LIMIT) else it
                }
                val cfg = enforceFreemium(permConfig.value, pro)
                exportEngine.exportPermissions(perms, cfg)
            }
        }

        result.onSuccess { content ->
            exportWriter.write(uri, content).onSuccess {
                exportResult.value = ExportResult.Success(uri)
            }.onFailure { e ->
                log(TAG, WARN) { "Write failed: $e" }
                exportResult.value = ExportResult.Error(e)
            }
        }.onFailure { e ->
            log(TAG, WARN) { "Export failed: $e" }
            exportResult.value = ExportResult.Error(e)
        }
    }

    fun resetExportResult() {
        exportResult.value = null
    }

    fun onUpgrade() {
        navTo(Nav.Main.Upgrade)
    }

    private fun resolveApps(
        allApps: List<AppInfo>,
        ids: List<Pair<String, Int>>,
    ): List<AppInfo> {
        val idSet = ids.toSet()
        return allApps.filter { (it.pkgName to it.userHandleId) in idSet }
    }

    private fun resolvePermissions(
        allPerms: Collection<BasePermission>,
        ids: List<String>,
    ): Collection<BasePermission> {
        val idSet = ids.map { Permission.Id(it) }.toSet()
        return allPerms.filter { it.id in idSet }
    }

    private fun enforceFreemium(config: AppExportConfig, isPro: Boolean): AppExportConfig {
        return if (isPro) config else config.copy(format = ExportFormat.MARKDOWN)
    }

    private fun enforceFreemium(config: PermissionExportConfig, isPro: Boolean): PermissionExportConfig {
        return if (isPro) config else config.copy(format = ExportFormat.MARKDOWN)
    }

    companion object {
        private const val FREE_EXPORT_LIMIT = 5
        private val TAG = logTag("Export", "VM")
    }
}
