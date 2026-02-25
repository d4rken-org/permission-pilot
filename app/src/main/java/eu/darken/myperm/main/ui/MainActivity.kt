package eu.darken.myperm.main.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.LocalNavigationController
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavigationController
import eu.darken.myperm.common.navigation.NavigationEntry
import eu.darken.myperm.common.theming.PermPilotTheme
import eu.darken.myperm.common.uix.Activity2
import eu.darken.myperm.settings.core.GeneralSettings
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : Activity2() {

    private val vm: MainActivityVM by viewModels()

    @Inject lateinit var navCtrl: NavigationController
    @Inject lateinit var navigationEntries: Set<@JvmSuppressWildcards NavigationEntry>
    @Inject lateinit var generalSettings: GeneralSettings

    private var showSplashScreen by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        splashScreen.setKeepVisibleCondition { showSplashScreen && savedInstanceState == null }

        vm.increaseLaunchCount()

        val startDestination: NavKey = if (generalSettings.isOnboardingFinished.value) {
            Nav.Tab.Apps
        } else {
            Nav.Main.Onboarding
        }

        setContent {
            val readyState by vm.readyState.collectAsState()
            if (readyState) showSplashScreen = false

            val upgradeNag by vm.upgradeNag.collectAsState(initial = null)

            val backStack = rememberNavBackStack(startDestination)
            navCtrl.setup(backStack)

            PermPilotTheme {
                val backgroundColor = MaterialTheme.colorScheme.background
                val useDarkIcons = backgroundColor.luminance() > 0.5f
                SideEffect {
                    window.decorView.setBackgroundColor(backgroundColor.toArgb())
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.isAppearanceLightStatusBars = useDarkIcons
                    insetsController.isAppearanceLightNavigationBars = useDarkIcons
                }

                CompositionLocalProvider(LocalNavigationController provides navCtrl) {
                    MainScreen(
                        backStack = backStack,
                        navCtrl = navCtrl,
                        navigationEntries = navigationEntries,
                        onUpgradeNag = upgradeNag,
                    )
                }
            }
        }
    }

    companion object {
        private val TAG = logTag("MainActivity")
    }
}
