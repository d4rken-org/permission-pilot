package eu.darken.myperm.main.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import eu.darken.myperm.R
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavigationController
import eu.darken.myperm.common.navigation.NavigationEntry

@Composable
fun MainScreen(
    backStack: NavBackStack<NavKey>,
    navCtrl: NavigationController,
    navigationEntries: Set<NavigationEntry>,
    onUpgradeNag: ((Activity) -> Unit)?,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    onUpgradeNag?.let { nag ->
        LaunchedEffect(nag) {
            activity?.let { nag(it) }
        }
    }

    val currentEntry = backStack.lastOrNull()
    val isTabScreen = currentEntry is Nav.Tab

    Box(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (!navCtrl.up()) {
                    activity?.finish()
                }
            },
            entryProvider = entryProvider {
                navigationEntries.forEach { entry ->
                    entry.apply { setup() }
                }
            },
        )

        if (isTabScreen) {
            NavigationBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            ) {
                NavigationBarItem(
                    selected = currentEntry is Nav.Tab.Overview,
                    onClick = { navCtrl.replace(Nav.Tab.Overview) },
                    icon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null) },
                    label = { Text(stringResource(R.string.overview_page_label)) },
                )
                NavigationBarItem(
                    selected = currentEntry is Nav.Tab.Apps,
                    onClick = { navCtrl.replace(Nav.Tab.Apps) },
                    icon = { Icon(Icons.Filled.Apps, contentDescription = null) },
                    label = { Text(stringResource(R.string.apps_page_label)) },
                )
                NavigationBarItem(
                    selected = currentEntry is Nav.Tab.Permissions,
                    onClick = { navCtrl.replace(Nav.Tab.Permissions) },
                    icon = { Icon(Icons.Filled.Security, contentDescription = null) },
                    label = { Text(stringResource(R.string.permissions_page_label)) },
                )
            }
        }
    }
}
