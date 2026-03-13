package eu.darken.myperm.watcher.ui.reports

import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.room.entity.PermissionChangeEntity
import eu.darken.myperm.common.uix.ViewModel4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val changeDao: PermissionChangeDao,
) : ViewModel4(dispatcherProvider) {

    data class ReportItem(
        val id: Long,
        val packageName: String,
        val appLabel: String?,
        val eventType: String,
        val detectedAt: Long,
        val isSeen: Boolean,
    )

    sealed class State {
        data object Loading : State()
        data class Ready(val items: List<ReportItem>) : State()
    }

    val state: Flow<State> = changeDao.getAll().map { entities ->
        State.Ready(
            items = entities.map { it.toItem() }
        )
    }

    fun onReportClicked(item: ReportItem) = launch {
        changeDao.markSeen(item.id)
        navTo(Nav.Watcher.ReportDetail(item.id))
    }

    fun markAllSeen() = launch {
        changeDao.markAllSeen()
    }

    private fun PermissionChangeEntity.toItem() = ReportItem(
        id = id,
        packageName = packageName,
        appLabel = appLabel,
        eventType = eventType,
        detectedAt = detectedAt,
        isSeen = isSeen,
    )

    companion object {
        private val TAG = logTag("Watcher", "Reports", "VM")
    }
}
