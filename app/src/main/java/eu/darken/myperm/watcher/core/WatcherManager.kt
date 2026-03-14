package eu.darken.myperm.watcher.core

import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.room.entity.TriggerReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatcherManager @Inject constructor(
    private val appRepo: AppRepo,
    private val watcherDiffRunner: WatcherDiffRunner,
) {

    enum class Phase {
        SCANNING,
        CHECKING_CHANGES,
    }

    private val _phase = MutableStateFlow<Phase?>(null)
    val phase: StateFlow<Phase?> = _phase.asStateFlow()

    private val mutex = Mutex()

    suspend fun scanDiffAndPrune(reason: TriggerReason, keepCount: Int = 2) = mutex.withLock {
        log(TAG) { "scanDiffAndPrune(reason=$reason, keepCount=$keepCount)" }
        try {
            _phase.value = Phase.SCANNING
            appRepo.scanAndSave(reason)
            _phase.value = Phase.CHECKING_CHANGES
            watcherDiffRunner.processNewSnapshots()
            appRepo.pruneSnapshots(keepCount)
        } finally {
            _phase.value = null
        }
    }

    companion object {
        private val TAG = logTag("Watcher", "Manager")
    }
}
