package eu.darken.myperm.common.upgrade.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.twotone.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.darken.myperm.R
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.NavigationEventHandler
import java.time.Instant
import java.util.Date

@Composable
fun UpgradeScreenHost(
    manage: Boolean,
    vm: UpgradeViewModel = hiltViewModel(),
) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    LaunchedEffect(manage) { vm.init(manage) }

    val snackbarHostState = remember { SnackbarHostState() }
    val returnedEarlyMessage = stringResource(R.string.upgrade_foss_sponsor_returned_early)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        vm.sponsorEvents.collect { event ->
            when (event) {
                UpgradeViewModel.SponsorEvent.ReturnedTooEarly -> snackbarHostState.showSnackbar(returnedEarlyMessage)
            }
        }
    }

    val state by vm.state.collectAsState()

    UpgradeScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateUp = { vm.navUp() },
        onSponsor = { vm.sponsor() },
        onSponsorAgain = { vm.openSponsorPage() },
    )
}

@Composable
fun UpgradeScreen(
    state: UpgradeViewModel.State,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateUp: () -> Unit,
    onSponsor: () -> Unit,
    onSponsorAgain: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                UpgradeMascotHeader(
                    circleColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    suffixColor = MaterialTheme.colorScheme.secondary,
                    titleColor = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(24.dp))

                when (state.view) {
                    null -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    FossUpgradeView.PITCH -> PitchContent(onSponsor = onSponsor)
                    FossUpgradeView.STATUS_FREE -> FreeStatusContent(onSponsor = onSponsor)
                    FossUpgradeView.STATUS_UPGRADED -> UpgradedStatusContent(
                        upgradedAt = state.upgradedAt,
                        onSponsorAgain = onSponsorAgain,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            IconButton(onClick = onNavigateUp, modifier = Modifier.padding(4.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.TwoTone.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun PitchContent(onSponsor: () -> Unit) {
    UpgradePreambleCard(
        text = stringResource(R.string.upgrade_foss_preamble),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
    )
    Spacer(modifier = Modifier.height(16.dp))
    UpgradeBenefitsCard(
        chipColor = MaterialTheme.colorScheme.secondaryContainer,
        chipContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    Spacer(modifier = Modifier.height(16.dp))
    SponsorButton(labelRes = R.string.upgrade_foss_sponsor_action, onClick = onSponsor)
    Text(
        text = stringResource(R.string.upgrade_foss_sponsor_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun FreeStatusContent(onSponsor: () -> Unit) {
    StatusCard(
        titleRes = R.string.upgrade_screen_status_free_title,
        bodyRes = R.string.upgrade_screen_status_free_body,
    )
    Spacer(modifier = Modifier.height(16.dp))
    UpgradeBenefitsCard(
        chipColor = MaterialTheme.colorScheme.secondaryContainer,
        chipContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    Spacer(modifier = Modifier.height(16.dp))
    SponsorButton(labelRes = R.string.upgrade_screen_status_free_action, onClick = onSponsor)
}

@Composable
private fun UpgradedStatusContent(
    upgradedAt: Instant?,
    onSponsorAgain: () -> Unit,
) {
    val context = LocalContext.current
    val sinceText = upgradedAt?.let {
        DateFormat.getMediumDateFormat(context).format(Date(it.toEpochMilli()))
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.upgrade_screen_status_upgraded_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.upgrade_screen_status_upgraded_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (sinceText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.upgrade_screen_supporter_since, sinceText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    StatusCard(
        titleRes = R.string.upgrade_screen_recurring_title,
        bodyRes = R.string.upgrade_screen_recurring_body,
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedButton(
        onClick = onSponsorAgain,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(Icons.TwoTone.Favorite, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.upgrade_screen_recurring_action))
    }
}

@Composable
private fun StatusCard(titleRes: Int, bodyRes: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(bodyRes), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SponsorButton(labelRes: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(Icons.TwoTone.Favorite, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.titleMedium)
    }
}
