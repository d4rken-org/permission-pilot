package eu.darken.myperm.apps.ui.list

import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.R
import eu.darken.myperm.common.getQuantityString
import eu.darken.myperm.common.lists.differ.update
import eu.darken.myperm.common.lists.setupDefaults
import eu.darken.myperm.common.observe2
import eu.darken.myperm.common.uix.Fragment3
import eu.darken.myperm.common.viewbinding.viewBinding
import eu.darken.myperm.databinding.AppsFragmentBinding
import eu.darken.myperm.main.ui.main.MainFragmentDirections
import javax.inject.Inject

@AndroidEntryPoint
class AppsFragment : Fragment3(R.layout.apps_fragment) {

    override val vm: AppsFragmentVM by viewModels()
    override val ui: AppsFragmentBinding by viewBinding()

    @Inject lateinit var appsAdapter: AppsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        customNavController = requireActivity().findNavController(R.id.nav_host_main_activity)

        ui.filterAction.setOnClickListener { vm.showFilterDialog() }
        ui.sortAction.setOnClickListener { vm.showSortDialog() }

        vm.events.observe2(ui) { event ->
            when (event) {
                is AppsEvents.ShowFilterDialog -> FilterDialog(requireActivity()).show(event.options) { newOptions ->
                    vm.updateFilterOptions { newOptions }
                }
                is AppsEvents.ShowSortDialog -> SortDialog(requireActivity()).show(event.options) { newOptions ->
                    vm.updateSortOptions { newOptions }
                }
                is AppsEvents.ShowPermissionSnackbar -> {
                    Snackbar.make(ui.root, event.permission.getLabel(requireContext()), Snackbar.LENGTH_SHORT)
                        .setAction(R.string.general_show_action) {
                            MainFragmentDirections.actionMainFragmentToPermissionDetailsFragment(
                                permissionId = event.permission.id
                            ).navigate()
                        }
                        .show()
                }
            }
        }
        ui.searchInput.addTextChangedListener {
            vm.onSearchInputChanged(it?.toString())
        }

        ui.list.setupDefaults(appsAdapter)
        vm.listData.observe2(this@AppsFragment, ui) {
            listCaption.text = requireContext().getQuantityString(R.plurals.generic_x_items_label, it.size)
            appsAdapter.update(it)
        }

        super.onViewCreated(view, savedInstanceState)
    }
}
