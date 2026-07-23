package eu.darken.myperm.common.upgrade.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.OpenInNew
import androidx.compose.material.icons.twotone.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.darken.myperm.R

@Composable
fun OwnershipContent(
    state: UpgradeUiState.Loaded,
    onSubscribe: () -> Unit,
    onBuyIap: () -> Unit,
    onSwitchToIap: () -> Unit,
    onRestore: () -> Unit,
    onManageSubscription: () -> Unit,
    onContactSupport: () -> Unit,
) {
    if (state.gracePeriod) {
        GraceCard(
            showDiagnostics = state.graceHint?.showDiagnostics == true,
            restoreInProgress = state.restoreInProgress,
            enabled = state.isSettled && !state.actionBusy,
            onRestore = onRestore,
            onContactSupport = onContactSupport,
        )
        // Surface the offers even in grace, so a genuinely lapsed subscriber can re-subscribe or
        // switch to the one-time purchase immediately instead of waiting the window out.
        Spacer(modifier = Modifier.height(24.dp))
        OffersCard(
            pricing = state.pricing,
            enabled = state.isSettled && !state.actionBusy,
            onSubscribe = onSubscribe,
            onBuyIap = onBuyIap,
        )
        return
    }

    CongratsHero(state.ownership)
    Spacer(modifier = Modifier.height(16.dp))

    val sub = state.ownership.subscription
    if (sub != null) {
        OwnedSubscriptionCard(
            isAutoRenewing = sub.isAutoRenewing,
            alsoOwnsIap = state.ownership.hasIap,
            onManageSubscription = onManageSubscription,
        )
        // Only offer the switch when the sub is the sole entitlement; if they already own the IAP
        // there is nothing to switch to.
        if (!state.ownership.hasIap) {
            Spacer(modifier = Modifier.height(16.dp))
            SwitchOfferCard(
                iapPrice = state.pricing.iapPrice,
                switchUnlocked = state.switchUnlocked,
                verificationInProgress = state.verificationInProgress,
                enabled = state.isSettled && !state.actionBusy,
                onSwitchToIap = onSwitchToIap,
            )
        }
    } else if (state.ownership.hasIap) {
        OwnedIapCard()
    }

    Spacer(modifier = Modifier.height(24.dp))
    RestoreSection(
        restoreInProgress = state.restoreInProgress,
        enabled = state.isSettled && !state.actionBusy,
        onRestore = onRestore,
    )
}

@Composable
private fun CongratsHero(ownership: Ownership) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.upgrade_screen_owned_hero_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            val bodyRes = if (ownership.hasIap) {
                R.string.upgrade_screen_owned_hero_iap_body
            } else {
                R.string.upgrade_screen_owned_hero_sub_body
            }
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun OwnedSubscriptionCard(
    isAutoRenewing: Boolean,
    alsoOwnsIap: Boolean,
    onManageSubscription: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.upgrade_screen_owned_sub_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            val bodyRes = when {
                alsoOwnsIap -> R.string.upgrade_screen_owned_both_warning
                isAutoRenewing -> R.string.upgrade_screen_owned_sub_renewing_body
                else -> R.string.upgrade_screen_owned_sub_not_renewing_body
            }
            Text(text = stringResource(bodyRes), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onManageSubscription, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.TwoTone.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.upgrade_screen_manage_subscription_action))
            }
        }
    }
}

@Composable
private fun OwnedIapCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.upgrade_screen_owned_iap_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.upgrade_screen_owned_iap_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SwitchOfferCard(
    iapPrice: String?,
    switchUnlocked: Boolean,
    verificationInProgress: Boolean,
    enabled: Boolean,
    onSwitchToIap: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.upgrade_screen_switch_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (switchUnlocked) R.string.upgrade_screen_switch_purchase_note
                    else R.string.upgrade_screen_switch_locked_note,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSwitchToIap,
                enabled = switchUnlocked && enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (verificationInProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                val label = if (iapPrice != null) {
                    stringResource(R.string.upgrade_screen_iap_action_hint, iapPrice)
                } else {
                    stringResource(R.string.upgrade_screen_iap_action)
                }
                Text(label)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.upgrade_screen_limitation_disclosure),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GraceCard(
    showDiagnostics: Boolean,
    restoreInProgress: Boolean,
    enabled: Boolean,
    onRestore: () -> Unit,
    onContactSupport: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.upgrade_screen_grace_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (showDiagnostics) R.string.upgrade_screen_grace_diagnostics_body
                    else R.string.upgrade_screen_grace_body,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (showDiagnostics) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRestore, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    if (restoreInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.upgrade_screen_restore_purchase_action))
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onContactSupport, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.contact_support_label))
                }
            }
        }
    }
}

@Composable
fun OffersCard(
    pricing: Pricing,
    enabled: Boolean,
    onSubscribe: () -> Unit,
    onBuyIap: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (pricing.subAvailable) {
            Button(
                onClick = onSubscribe,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.TwoTone.Stars, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (pricing.subscriptionAction == SubscriptionAction.TRIAL) {
                            R.string.upgrade_screen_subscription_trial_action
                        } else {
                            R.string.upgrade_screen_subscription_action
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (pricing.subPrice != null) {
                Text(
                    text = stringResource(R.string.upgrade_screen_subscription_action_hint_yearly, pricing.subPrice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (pricing.iapAvailable) {
            FilledTonalButton(
                onClick = onBuyIap,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.upgrade_screen_iap_action),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (pricing.iapPrice != null) {
                Text(
                    text = stringResource(R.string.upgrade_screen_iap_action_hint, pricing.iapPrice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.upgrade_screen_options_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun RestoreSection(
    restoreInProgress: Boolean,
    enabled: Boolean,
    onRestore: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.upgrade_screen_restore_status_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.upgrade_screen_restore_status_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onRestore,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            if (restoreInProgress) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(stringResource(R.string.upgrade_screen_restore_purchase_action))
        }
    }
}

@Composable
fun RestoreBanner(
    onRestore: () -> Unit,
    restoreInProgress: Boolean,
    enabled: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.upgrade_screen_restore_banner_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.upgrade_screen_restore_banner_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRestore,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (restoreInProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.upgrade_screen_restore_purchase_action))
            }
        }
    }
}

@Composable
fun FailedRestoreDialog(
    onContactSupport: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.upgrade_screen_restore_status_title)) },
        text = {
            Column {
                Text(stringResource(R.string.upgrade_screen_restore_purchase_message))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.upgrade_screen_restore_webinstall_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onContactSupport() }) {
                Text(stringResource(R.string.contact_support_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.general_cancel_action)) }
        },
    )
}
