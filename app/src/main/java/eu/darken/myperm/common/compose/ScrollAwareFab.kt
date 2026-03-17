package eu.darken.myperm.common.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

@Composable
fun rememberFabVisibility(threshold: Float = 10f): Pair<Boolean, NestedScrollConnection> {
    var visible by remember { mutableStateOf(true) }
    var cumDelta by remember { mutableFloatStateOf(0f) }

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                cumDelta += available.y
                if (cumDelta > threshold) {
                    visible = true
                    cumDelta = 0f
                } else if (cumDelta < -threshold) {
                    visible = false
                    cumDelta = 0f
                }
                return Offset.Zero
            }
        }
    }

    return visible to connection
}
