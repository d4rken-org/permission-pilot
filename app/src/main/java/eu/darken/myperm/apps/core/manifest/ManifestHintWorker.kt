package eu.darken.myperm.apps.core.manifest

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import kotlinx.coroutines.flow.first

@HiltWorker
class ManifestHintWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appRepo: AppRepo,
    private val manifestHintRepo: ManifestHintRepo,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val apps = when (val state = appRepo.appData.first()) {
                is AppRepo.AppDataState.Ready -> state.apps
                is AppRepo.AppDataState.NoSnapshot -> {
                    log(TAG) { "No snapshot available, skipping" }
                    return Result.success()
                }
            }
            manifestHintRepo.runScan(apps)
            Result.success()
        } catch (e: Exception) {
            log(TAG, WARN) { "Manifest hint scan failed: ${e.asLog()}" }
            Result.retry()
        }
    }

    companion object {
        private val TAG = logTag("Apps", "Manifest", "HintWorker")
    }
}
