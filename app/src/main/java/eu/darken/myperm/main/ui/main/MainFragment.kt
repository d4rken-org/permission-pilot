package eu.darken.myperm.main.ui.main

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.R
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.colorString
import eu.darken.myperm.common.navigation.doNavigate
import eu.darken.myperm.common.uix.Fragment3
import eu.darken.myperm.common.upgrade.UpgradeRepo
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
                    R.id.menu_item_settings -> {
                        doNavigate(MainFragmentDirections.actionMainFragmentToSettingsContainerFragment())
                        true
                    }
                    R.id.menu_item_donate -> {
                        vm.onUpgrade()
                        true
                    }
                    R.id.menu_item_upgrade -> {
                        vm.onUpgrade()
                        true
                    }
                    else -> super.onOptionsItemSelected(it)
                }
            }
            subtitle = BuildConfigWrap.VERSION_DESCRIPTION
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

        vm.state.observe2(ui) { state ->
//            bottomNavigation.getOrCreateBadge(R.id.page_apps).apply {
//                isVisible = true
//                number = state.appCount
//            }
//            bottomNavigation.getOrCreateBadge(R.id.page_permissions).apply {
//                isVisible = true
//                number = state.permissionCount
//            }
        }

        vm.upgradeInfo.observe2(ui) { info ->
            val gplay = toolbar.menu.findItem(R.id.menu_item_upgrade)
            val donate = toolbar.menu.findItem(R.id.menu_item_donate)
            // TODO currently there are no pro features
            val nope = false
            gplay.isVisible = info.type == UpgradeRepo.Type.GPLAY && !info.isPro && nope
            donate.isVisible = info.type == UpgradeRepo.Type.FOSS && !info.isPro && nope

            val baseTitle = when (info.type) {
                UpgradeRepo.Type.GPLAY -> getString(if (info.isPro) R.string.app_name_pro else R.string.app_name)
                UpgradeRepo.Type.FOSS -> getString(if (info.isPro) R.string.app_name_pro else R.string.app_name)
            }.split(" ".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()

            toolbar.title = if (baseTitle.size == 3) {
                val builder = SpannableStringBuilder("${baseTitle[0]} ${baseTitle[1]} ")
                val color = when (info.type) {
                    UpgradeRepo.Type.FOSS -> R.color.positive_highlight
                    UpgradeRepo.Type.GPLAY -> R.color.positive_highlight
                }
                builder.append(baseTitle[2].colorString(requireContext(), color))
            } else {
                getString(R.string.app_name)
            }
        }

        vm.launchUpgradeFlow.observe2 { it(requireActivity()) }
        super.onViewCreated(view, savedInstanceState)
    }
}
