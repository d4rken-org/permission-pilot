package eu.darken.myperm.debug.manifest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.apps.core.manifest.ApkManifestReader
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Debug-only broadcast receiver to fire the parity checker.
 *
 * Trigger: `adb shell am broadcast -a eu.darken.myperm.DEBUG_PARSER_PARITY`
 *
 * Output lives in logcat under the `PP:Debug:ParserParity` tag.
 */
@AndroidEntryPoint
class ParserParityReceiver : BroadcastReceiver() {

    @Inject lateinit var apkManifestReader: ApkManifestReader

    @Inject @AppScope lateinit var appScope: CoroutineScope

    @Inject lateinit var dispatcherProvider: DispatcherProvider

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        log(TAG) { "Parity check triggered via broadcast" }
        appScope.launch(dispatcherProvider.IO) {
            val checker = ParserParityChecker(context.applicationContext, apkManifestReader)
            checker.runAll()
        }
    }

    companion object {
        const val ACTION = "eu.darken.myperm.DEBUG_PARSER_PARITY"
        private val TAG = logTag("Debug", "ParityReceiver")
    }
}
