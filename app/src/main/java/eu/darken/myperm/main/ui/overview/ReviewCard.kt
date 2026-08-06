package eu.darken.myperm.main.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.PermPilotMascot
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper

@Composable
internal fun ReviewCard(
    onReview: () -> Unit,
    onDismiss: () -> Unit,
    reviewEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    // The card only disappears with the next state emission, so the tap targets need a latch. It is
    // asymmetric on purpose: the harmful orderings are a dismiss after a review (which overwrites
    // the review bookkeeping with a snooze) and a review after a dismiss. A repeated review tap is
    // harmless, the tool's single-flight lock absorbs it, and blocking it here would leave a dead
    // card whenever a Play request fails and nothing gets persisted.
    var dismissLocked by rememberSaveable { mutableStateOf(false) }
    var fullyLatched by rememberSaveable { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PermPilotMascot(size = 40.dp)
                Text(
                    text = stringResource(R.string.review_app_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(
                    onClick = {
                        if (!fullyLatched && !dismissLocked) {
                            dismissLocked = true
                            fullyLatched = true
                            onDismiss()
                        }
                    },
                    enabled = !fullyLatched && !dismissLocked,
                ) {
                    Text(text = stringResource(R.string.review_app_dismiss_action))
                }
                Button(
                    onClick = {
                        if (!fullyLatched && reviewEnabled) {
                            dismissLocked = true
                            onReview()
                        }
                    },
                    enabled = reviewEnabled && !fullyLatched,
                ) {
                    Text(text = stringResource(R.string.review_app_review_action))
                }
            }
        }
    }
}

@Preview2
@Composable
private fun ReviewCardPreview() = PreviewWrapper {
    ReviewCard(
        onReview = {},
        onDismiss = {},
    )
}
