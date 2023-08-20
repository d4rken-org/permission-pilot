package eu.darken.myperm

import android.app.Application
import android.os.DeadObjectException
import android.os.TransactionTooLargeException
import coil.Coil
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import eu.darken.myperm.common.debug.autoreport.AutomaticBugReporter
import eu.darken.myperm.common.debug.logging.LogCatLogger
import eu.darken.myperm.common.debug.logging.Logging
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.debug.recording.core.RecorderModule
import eu.darken.myperm.common.error.causes
import eu.darken.myperm.settings.core.GeneralSettings
import javax.inject.Inject

@HiltAndroidApp
open class App : Application() {

    @Inject lateinit var bugReporter: AutomaticBugReporter
    @Inject lateinit var recorderModule: RecorderModule
    @Inject lateinit var imageLoaderFactory: ImageLoaderFactory
    @Inject lateinit var generalSettings: GeneralSettings

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Logging.install(LogCatLogger())
            log(TAG) { "BuildConfig.DEBUG=true" }
        }

        bugReporter.setup(this)

        Coil.setImageLoader(imageLoaderFactory)

        // https://github.com/d4rken-org/permission-pilot/issues/186
        Thread.setDefaultUncaughtExceptionHandler(ipcGuard)

        log(TAG) { "onCreate() done! ${Exception().asLog()}" }
    }

    private val ipcGuard = object : Thread.UncaughtExceptionHandler {

        private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            val ipcMessages = setOf(
                "Package manager has died",
                "DeadSystemException"
            )
            val ipcExceptions = setOf(
                DeadObjectException::class,
                TransactionTooLargeException::class,
            )
            val isIpcIssue = ipcMessages.any { it == throwable.message }
                    || throwable.causes.any { cause -> ipcExceptions.any { it.isInstance(cause) } }

            if (isIpcIssue) {
                log(TAG, WARN) { "Crashing due to IPC buffer overflow!" }
                if (generalSettings.ipcParallelisation.value == 0) {
                    log(TAG, WARN) { "Reducing `ipcParallelisation` from AUTO (0) to 1" }
                    generalSettings.ipcParallelisation.value = 1
                }
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        internal val TAG = logTag("App")
    }
}
