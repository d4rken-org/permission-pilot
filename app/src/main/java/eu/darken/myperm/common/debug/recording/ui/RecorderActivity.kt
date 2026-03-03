package eu.darken.myperm.common.debug.recording.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.compose.runtime.collectAsState
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.theming.PermPilotTheme
import eu.darken.myperm.common.uix.Activity2

@AndroidEntryPoint
class RecorderActivity : Activity2() {

    private val vm: RecorderActivityVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (intent.getStringExtra(RECORD_PATH) == null) {
            finish()
            return
        }

        setContent {
            PermPilotTheme {
                val backgroundColor = MaterialTheme.colorScheme.background
                val useDarkIcons = backgroundColor.luminance() > 0.5f
                SideEffect {
                    window.decorView.setBackgroundColor(backgroundColor.toArgb())
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.isAppearanceLightStatusBars = useDarkIcons
                    insetsController.isAppearanceLightNavigationBars = useDarkIcons
                }

                LaunchedEffect(Unit) {
                    vm.events.collect { event ->
                        when (event) {
                            is RecorderActivityVM.Event.ShareIntent -> {
                                try {
                                    startActivity(event.intent)
                                } catch (_: Exception) {
                                }
                            }

                            is RecorderActivityVM.Event.Finish -> finish()
                        }
                    }
                }

                val state by vm.state.collectAsState(initial = null)
                state?.let {
                    RecorderScreen(
                        state = it,
                        onShare = { vm.share() },
                        onKeep = { vm.keep() },
                        onDiscard = { vm.discard() },
                        onPrivacyPolicy = { vm.goPrivacyPolicy() },
                    )
                }
            }
        }
    }

    companion object {
        internal val TAG = logTag("Debug", "Log", "RecorderActivity")
        const val RECORD_PATH = "logPath"
        const val RECORDING_STARTED_AT = "recordingStartedAt"

        fun getLaunchIntent(context: Context, path: String, startedAt: Long = 0L): Intent {
            val intent = Intent(context, RecorderActivity::class.java)
            intent.putExtra(RECORD_PATH, path)
            intent.putExtra(RECORDING_STARTED_AT, startedAt)
            return intent
        }
    }
}
