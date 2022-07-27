package eu.darken.myperm.common.debug.autoreporting

import android.app.Application
import eu.darken.myperm.common.debug.autoreport.AutomaticBugReporter
import eu.darken.myperm.common.debug.logging.log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FossAutoReporting @Inject constructor() : AutomaticBugReporter {
    override fun setup(application: Application) {
        // NOOP
    }

    override fun notify(throwable: Throwable) {
        throw IllegalStateException("Who initliazed this? Without setup no calls to here!")
    }
}