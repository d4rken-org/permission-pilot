package eu.darken.myperm.common.debug.autoreport

import android.app.Application

interface AutomaticBugReporter {

    fun setup(application: Application)

    fun notify(throwable: Throwable)
}