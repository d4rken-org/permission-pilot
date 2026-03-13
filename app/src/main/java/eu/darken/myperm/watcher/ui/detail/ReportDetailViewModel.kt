package eu.darken.myperm.watcher.ui.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.watcher.core.PermissionDiff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

@HiltViewModel(assistedFactory = ReportDetailViewModel.Factory::class)
class ReportDetailViewModel @AssistedInject constructor(
    @Assisted private val reportId: Long,
    dispatcherProvider: DispatcherProvider,
    private val changeDao: PermissionChangeDao,
    private val json: Json,
) : ViewModel4(dispatcherProvider) {

    @AssistedFactory
    interface Factory {
        fun create(reportId: Long): ReportDetailViewModel
    }

    data class State(
        val packageName: String = "",
        val appLabel: String? = null,
        val eventType: String = "",
        val versionName: String? = null,
        val detectedAt: Long = 0,
        val diff: PermissionDiff = PermissionDiff(),
        val isLoading: Boolean = true,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    init {
        launch {
            val entity = changeDao.getById(reportId)
            if (entity != null) {
                val diff = try {
                    json.decodeFromString<PermissionDiff>(entity.changesJson)
                } catch (_: Exception) {
                    PermissionDiff()
                }
                _state.value = State(
                    packageName = entity.packageName,
                    appLabel = entity.appLabel,
                    eventType = entity.eventType,
                    versionName = entity.versionName,
                    detectedAt = entity.detectedAt,
                    diff = diff,
                    isLoading = false,
                )
            } else {
                _state.value = State(isLoading = false)
            }
        }
    }

    companion object {
        private val TAG = logTag("Watcher", "ReportDetail", "VM")
    }
}
