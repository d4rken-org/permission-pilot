package eu.darken.myperm.common.upgrade.ui

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.twotone.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import eu.darken.myperm.R
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.NavigationEventHandler

@Composable
fun UpgradeScreenHost(
    manage: Boolean,
    vm: UpgradeViewModel = hiltViewModel(),
) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val context = LocalContext.current
    val activity = context as? Activity
    if (activity == null) Log.w("UpgradeScreen", "Context is not an Activity: $context")

    LaunchedEffect(manage) { vm.init(manage) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state by vm.state.collectAsState()

    var showRestoreFailedDialog by remember { mutableStateOf(false) }
    var showStillRenewingDialog by remember { mutableStateOf(false) }
    var showCheckFailedDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                UpgradeViewModel.UpgradeEvent.RestoreFailed -> showRestoreFailedDialog = true
                UpgradeViewModel.UpgradeEvent.RestoreSucceeded ->
                    Toast.makeText(context, R.string.upgrade_screen_restore_success_message, Toast.LENGTH_LONG).show()

                UpgradeViewModel.UpgradeEvent.SubscriptionStillRenewing -> showStillRenewingDialog = true
                UpgradeViewModel.UpgradeEvent.SubscriptionCheckFailed -> showCheckFailedDialog = true
            }
        }
    }

    if (showRestoreFailedDialog) {
        FailedRestoreDialog(
            onContactSupport = { vm.onContactSupport() },
            onDismiss = { showRestoreFailedDialog = false },
        )
    }
    if (showStillRenewingDialog) {
        SubscriptionStillRenewingDialog(
            onManageSubscription = { vm.onManageSubscription() },
            onDismiss = { showStillRenewingDialog = false },
        )
    }
    if (showCheckFailedDialog) {
        SubscriptionCheckFailedDialog(onDismiss = { showCheckFailedDialog = false })
    }

    UpgradeScreen(
        state = state,
        onNavigateUp = { vm.navUp() },
        onSubscribe = { activity?.let { vm.onSubscribe(it) } },
        onBuyIap = { activity?.let { vm.onBuyIap(it) } },
        onSwitchToIap = { activity?.let { vm.onSwitchToIap(it) } },
        onRestore = { vm.onRestore() },
        onManageSubscription = { vm.onManageSubscription() },
        onContactSupport = { vm.onContactSupport() },
        onRetry = { vm.retrySkuQuery() },
    )
}

@Composable
fun UpgradeScreen(
    state: UpgradeUiState,
    onNavigateUp: () -> Unit,
    onSubscribe: () -> Unit,
    onBuyIap: () -> Unit,
    onSwitchToIap: () -> Unit,
    onRestore: () -> Unit,
    onManageSubscription: () -> Unit,
    onContactSupport: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            UpgradeMascotHeader(
                circleColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                suffixColor = MaterialTheme.colorScheme.tertiary,
                titleColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))

            when (state) {
                UpgradeUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }

                is UpgradeUiState.Unavailable -> UnavailableContent(onRetry = onRetry)

                is UpgradeUiState.Loaded -> {
                    if (state.showOwnership) {
                        OwnershipContent(
                            state = state,
                            onSubscribe = onSubscribe,
                            onBuyIap = onBuyIap,
                            onSwitchToIap = onSwitchToIap,
                            onRestore = onRestore,
                            onManageSubscription = onManageSubscription,
                            onContactSupport = onContactSupport,
                        )
                    } else {
                        SalesContent(
                            state = state,
                            onSubscribe = onSubscribe,
                            onBuyIap = onBuyIap,
                            onRestore = onRestore,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        IconButton(
            onClick = onNavigateUp,
            modifier = Modifier.padding(4.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.TwoTone.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SalesContent(
    state: UpgradeUiState.Loaded,
    onSubscribe: () -> Unit,
    onBuyIap: () -> Unit,
    onRestore: () -> Unit,
) {
    if (state.wasPreviouslyPro) {
        RestoreBanner(
            onRestore = onRestore,
            restoreInProgress = state.restoreInProgress,
            enabled = state.isSettled && !state.actionBusy,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }

    SalesPitch()
    Spacer(modifier = Modifier.height(24.dp))

    OffersCard(
        pricing = state.pricing,
        enabled = state.isSettled && !state.actionBusy,
        verificationInProgress = state.verificationInProgress,
        onSubscribe = onSubscribe,
        onBuyIap = onBuyIap,
    )

    Spacer(modifier = Modifier.height(24.dp))
    RestoreSection(
        restoreInProgress = state.restoreInProgress,
        enabled = state.isSettled && !state.actionBusy,
        onRestore = onRestore,
    )
}

// The purchase options couldn't be loaded and the user isn't Pro. The pitch still renders (nothing is
// owned), with an inline warning + retry in place of the offers — and no restore section, matching the
// "nothing to reconcile yet" framing.
@Composable
private fun UnavailableContent(onRetry: () -> Unit) {
    SalesPitch()
    Spacer(modifier = Modifier.height(24.dp))
    UpgradeInlineStateCard(
        title = stringResource(R.string.upgrade_screen_offers_unavailable_title),
        body = stringResource(R.string.upgrade_screen_offers_unavailable_message),
        onRetry = onRetry,
    )
}

// Shared preamble + benefits pitch, rendered on both the sales and the offers-unavailable screens.
@Composable
private fun SalesPitch() {
    UpgradePreambleCard(
        text = stringResource(R.string.upgrade_screen_preamble),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
    )
    Spacer(modifier = Modifier.height(24.dp))
    UpgradeBenefitsCard(
        chipColor = MaterialTheme.colorScheme.primaryContainer,
        chipContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

@Composable
private fun UpgradeInlineStateCard(
    title: String,
    body: String,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.TwoTone.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.general_retry_action))
            }
        }
    }
}
