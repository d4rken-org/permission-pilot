package eu.darken.myperm.common.debug.autoreport

import android.app.Application
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.InstallId
import eu.darken.myperm.common.debug.logging.logTag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GooglePlayReporting @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bugReportSettings: DebugSettings,
    private val installId: InstallId,
) : AutomaticBugReporter {

    override fun setup(application: Application) {
        // NOOP
    }

    override fun notify(throwable: Throwable) {
        // NOOP
    }

    companion object {
        private val TAG = logTag("Debug", "GooglePlayReporting")
    }
}