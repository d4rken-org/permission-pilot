package eu.darken.myperm.main.ui.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.R
import eu.darken.myperm.common.navigation.doNavigate
import eu.darken.myperm.common.uix.Fragment3
import eu.darken.myperm.common.viewbinding.viewBinding
import eu.darken.myperm.databinding.MainFragmentBinding


@AndroidEntryPoint
class MainFragment : Fragment3(R.layout.main_fragment) {

    override val vm: MainFragmentVM by viewModels()
    override val ui: MainFragmentBinding by viewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui.toolbar.apply {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_settings -> {
                        doNavigate(MainFragmentDirections.actionMainFragmentToSettingsContainerFragment())
                        true
                    }
                    else -> super.onOptionsItemSelected(it)
                }
            }
        }
        val navController: NavController = ui.bottomNavHost.getFragment<NavHostFragment>().navController
        setupWithNavController(ui.bottomNavigation, navController)

        ui.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.page_apps -> {
                    navController.navigate(R.id.action_global_appsFragment)
                    true
                }
                R.id.page_permissions -> {
                    navController.navigate(R.id.action_global_permissionsFragment)
                    true
                }
                else -> false
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }
}
