package eu.darken.myperm.common.debug.recording.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.R
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.theming.PermPilotTheme
import eu.darken.myperm.common.uix.Activity2

@AndroidEntryPoint
class RecorderActivity : Activity2() {

    private val vm: RecorderActivityVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val hasSessionId = intent.getStringExtra(RECORD_SESSION_ID) != null
        val hasLegacyPath = intent.getStringExtra(RECORD_PATH) != null
        if (!hasSessionId && !hasLegacyPath) {
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

                var hasShared by rememberSaveable { mutableStateOf(false) }
                var showSentConfirm by rememberSaveable { mutableStateOf(false) }
                var showDiscardConfirm by rememberSaveable { mutableStateOf(false) }

                LifecycleResumeEffect(Unit) {
                    if (hasShared) {
                        showSentConfirm = true
                        hasShared = false
                    }
                    onPauseOrDispose {}
                }

                LaunchedEffect(Unit) {
                    vm.events.collect { event ->
                        when (event) {
                            is RecorderActivityVM.Event.ShareIntent -> {
                                hasShared = true
                                try {
                                    startActivity(event.intent)
                                } catch (e: Exception) {
                                    log(TAG, WARN) { "Failed to start share activity: $e" }
                                    hasShared = false
                                }
                            }

                            is RecorderActivityVM.Event.Finish -> finish()
                        }
                    }
                }

                if (showSentConfirm) {
                    AlertDialog(
                        onDismissRequest = { showSentConfirm = false },
                        title = { Text(stringResource(R.string.support_debuglog_sent_title)) },
                        text = { Text(stringResource(R.string.support_debuglog_sent_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showSentConfirm = false
                                vm.discard()
                            }) {
                                Text(stringResource(R.string.debug_debuglog_screen_discard_action))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showSentConfirm = false
                                vm.keep()
                            }) {
                                Text(stringResource(R.string.general_close_action))
                            }
                        },
                    )
                }

                if (showDiscardConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDiscardConfirm = false },
                        title = { Text(stringResource(R.string.support_debuglog_session_delete_title)) },
                        text = { Text(stringResource(R.string.support_debuglog_session_delete_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showDiscardConfirm = false
                                vm.discard()
                            }) {
                                Text(stringResource(R.string.debug_debuglog_screen_discard_action))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDiscardConfirm = false }) {
                                Text(stringResource(android.R.string.cancel))
                            }
                        },
                    )
                }

                val state by vm.state.collectAsStateWithLifecycle(initialValue = null)
                state?.let {
                    RecorderScreen(
                        state = it,
                        onShare = { vm.share() },
                        onKeep = { vm.keep() },
                        onDiscard = { showDiscardConfirm = true },
                        onPrivacyPolicy = { vm.goPrivacyPolicy() },
                    )
                }
            }
        }
    }

    companion object {
        internal val TAG = logTag("Debug", "Log", "RecorderActivity")
        const val RECORD_SESSION_ID = "sessionId"
        const val RECORD_PATH = "logPath"

        fun getLaunchIntent(context: Context, sessionId: String?, legacyPath: String? = null): Intent {
            val intent = Intent(context, RecorderActivity::class.java)
            if (sessionId != null) intent.putExtra(RECORD_SESSION_ID, sessionId)
            if (legacyPath != null) intent.putExtra(RECORD_PATH, legacyPath)
            return intent
        }
    }
}
