package eu.darken.myperm.common.upgrade.ui

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.twotone.Code
import androidx.compose.material.icons.twotone.Favorite
import androidx.compose.material.icons.twotone.FileDownload
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material.icons.twotone.Palette
import androidx.compose.material.icons.twotone.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.PermPilotMascot
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
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                UpgradeViewModel.UpgradeEvent.RestoreFailed -> showRestoreFailedDialog = true
                UpgradeViewModel.UpgradeEvent.RestoreSucceeded ->
                    Toast.makeText(context, R.string.upgrade_screen_restore_success_message, Toast.LENGTH_LONG).show()

                UpgradeViewModel.UpgradeEvent.SubscriptionStillRenewing ->
                    Toast.makeText(context, R.string.upgrade_screen_sub_still_renewing_message, Toast.LENGTH_LONG).show()

                UpgradeViewModel.UpgradeEvent.SubscriptionCheckFailed ->
                    Toast.makeText(context, R.string.upgrade_screen_sub_check_failed_message, Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showRestoreFailedDialog) {
        FailedRestoreDialog(
            onContactSupport = { vm.onContactSupport() },
            onDismiss = { showRestoreFailedDialog = false },
        )
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
    )
}

private data class Benefit(val icon: ImageVector, val textRes: Int)

private val upgradeBenefits = listOf(
    Benefit(Icons.TwoTone.Palette, R.string.upgrade_benefit_themes),
    Benefit(Icons.TwoTone.Tune, R.string.upgrade_benefit_filtering),
    Benefit(Icons.TwoTone.FileDownload, R.string.upgrade_benefit_export),
    Benefit(Icons.TwoTone.Notifications, R.string.upgrade_benefit_monitoring),
    Benefit(Icons.TwoTone.Code, R.string.upgrade_benefit_manifest_viewer),
    Benefit(Icons.TwoTone.Favorite, R.string.upgrade_benefit_support),
)

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
) {
    Box(modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            UpgradeHeader()
            Spacer(modifier = Modifier.height(24.dp))

            when (state) {
                UpgradeUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }

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
private fun UpgradeHeader() {
    Box(contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        ) {}
        PermPilotMascot(size = 80.dp)
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = buildAnnotatedString {
            append(stringResource(R.string.upgrade_title_prefix))
            append(" ")
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)) {
                append(stringResource(R.string.upgrade_title_suffix))
            }
        },
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.upgrade_screen_preamble),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
    BenefitsCard()
    Spacer(modifier = Modifier.height(24.dp))

    OffersCard(
        pricing = state.pricing,
        enabled = state.isSettled && !state.actionBusy,
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

@Composable
private fun BenefitsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            upgradeBenefits.forEach { benefit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = benefit.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(benefit.textRes),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
