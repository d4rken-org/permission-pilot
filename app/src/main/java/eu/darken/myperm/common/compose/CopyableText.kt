package eu.darken.myperm.common.compose

import android.content.ClipData
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import eu.darken.myperm.R
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import kotlinx.coroutines.launch

private val TAG = logTag("Common", "CopyableText")

@Composable
fun CopyableText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontFamily: FontFamily? = null,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    // Match the icon to the text it labels. Deliberately no IconButton: its
    // minimumInteractiveComponentSize() would impose a 48dp box, padding the row.
    // Long-pressing the text to select is the accessible path to copying.
    val iconSize = with(LocalDensity.current) {
        if (style.fontSize.isSp) style.fontSize.toDp() else 12.dp
    }
    val interactionSource = remember { MutableInteractionSource() }

    // clickable() otherwise expands its hit area to minimumTouchTargetSize (48dp),
    // which would reach back over the trailing text and swallow taps meant to start
    // a selection. Scoped to the icon so nothing else loses touch-target expansion.
    val viewConfiguration = LocalViewConfiguration.current
    val exactHitArea = remember(viewConfiguration) {
        object : ViewConfiguration by viewConfiguration {
            override val minimumTouchTargetSize: DpSize = DpSize.Zero
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionContainer(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = text,
                style = style,
                color = color,
                fontFamily = fontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CompositionLocalProvider(LocalViewConfiguration provides exactHitArea) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = stringResource(R.string.general_copy_action),
                tint = color,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(iconSize)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = false, radius = iconSize),
                        role = Role.Button,
                    ) {
                        scope.launch {
                            val copied = try {
                                clipboard.setClipEntry(
                                    ClipEntry(ClipData.newPlainText(context.getString(R.string.app_name), text))
                                )
                                true
                            } catch (e: Exception) {
                                log(TAG, WARN) { "Failed to copy to clipboard: ${e.asLog()}" }
                                false
                            }
                            // Android 13+ shows its own system clipboard confirmation
                            if (copied && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                Toast.makeText(context, R.string.general_copied_to_clipboard_msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
            )
        }
    }
}

@Preview2
@Composable
private fun CopyableTextPreview() = PreviewWrapper {
    CopyableText(text = "eu.darken.myperm")
}

@Preview2
@Composable
private fun CopyableTextLongPreview() = PreviewWrapper {
    CopyableText(text = "android.permission.ACCESS_BACKGROUND_LOCATION_AND_MORE_VERY_LONG_IDENTIFIER")
}
