package eu.darken.myperm

import android.app.Application
import coil.Coil
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import eu.darken.myperm.common.debug.autoreport.AutomaticBugReporter
import eu.darken.myperm.common.debug.logging.*
import javax.inject.Inject

@HiltAndroidApp
open class App : Application() {

    @Inject lateinit var bugReporter: AutomaticBugReporter
    @Inject lateinit var imageLoaderFactory: ImageLoaderFactory

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Logging.install(LogCatLogger())
            log(TAG) { "BuildConfig.DEBUG=true" }
        }

        bugReporter.setup(this)

        Coil.setImageLoader(imageLoaderFactory)

        log(TAG) { "onCreate() done! ${Exception().asLog()}" }
    }

    companion object {
        internal val TAG = logTag("AKSv4")
    }
}
