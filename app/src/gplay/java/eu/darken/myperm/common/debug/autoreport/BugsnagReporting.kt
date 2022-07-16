package eu.darken.myperm.common.debug.autoreport

import android.app.Application
import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.getkeepsafe.relinker.ReLinker
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.App
import eu.darken.myperm.common.InstallId
import eu.darken.myperm.common.debug.Bugs
import eu.darken.myperm.common.debug.autoreport.bugsnag.BugsnagErrorHandler
import eu.darken.myperm.common.debug.autoreport.bugsnag.BugsnagLogger
import eu.darken.myperm.common.debug.autoreport.bugsnag.NOPBugsnagErrorHandler
import eu.darken.myperm.common.debug.logging.Logging
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class BugsnagReporting @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bugReportSettings: DebugSettings,
    private val installId: InstallId,
    private val bugsnagLogger: Provider<BugsnagLogger>,
    private val bugsnagErrorHandler: Provider<BugsnagErrorHandler>,
    private val nopBugsnagErrorHandler: Provider<NOPBugsnagErrorHandler>,
) : AutomaticBugReporter {

    override fun setup(application: Application) {
        ReLinker
            .log { message -> log(App.TAG) { "ReLinker: $message" } }
            .loadLibrary(application, "bugsnag-plugin-android-anr")

        val isEnabled = bugReportSettings.isAutoReportingEnabled.value
        log(TAG) { "setup(): isEnabled=$isEnabled" }

        try {
            val bugsnagConfig = Configuration.load(context).apply {
                if (bugReportSettings.isAutoReportingEnabled.value) {
                    Logging.install(bugsnagLogger.get())
                    setUser(installId.id, null, null)
                    autoTrackSessions = true
                    addOnError(bugsnagErrorHandler.get())
                    log(TAG) { "Bugsnag setup done!" }
                } else {
                    autoTrackSessions = false
                    addOnError(nopBugsnagErrorHandler.get())
                    log(TAG) { "Installing Bugsnag NOP error handler due to user opt-out!" }
                }
            }

            Bugsnag.start(context, bugsnagConfig)
            Bugs.reporter = this
        } catch (e: IllegalStateException) {
            log(TAG, WARN) { "Bugsnag API Key not configured." }
        }
    }

    override fun notify(throwable: Throwable) {
        Bugsnag.notify(throwable)
    }

    companion object {
        private val TAG = logTag("Debug", "AutoReporting")
    }
}