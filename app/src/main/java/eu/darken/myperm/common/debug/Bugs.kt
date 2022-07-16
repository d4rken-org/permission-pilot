package eu.darken.myperm.common.debug

import eu.darken.myperm.common.debug.autoreport.AutomaticBugReporter
import eu.darken.myperm.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag

object Bugs {
    var reporter: AutomaticBugReporter? = null
    fun report(exception: Exception) {
        log(TAG, VERBOSE) { "Reporting $exception" }
        if (reporter == null) {
            log(TAG, WARN) { "Bug tracking not initialized yet." }
            return
        }
        reporter?.notify(exception)
    }

    private val TAG = logTag("Debug", "Bugs")
}