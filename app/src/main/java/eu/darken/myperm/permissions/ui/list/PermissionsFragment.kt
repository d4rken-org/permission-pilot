package eu.darken.myperm.permissions.ui.list

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.R
import eu.darken.myperm.common.error.asErrorDialogBuilder
import eu.darken.myperm.common.getQuantityString
import eu.darken.myperm.common.lists.differ.update
import eu.darken.myperm.common.lists.setupDefaults
import eu.darken.myperm.common.observe2
import eu.darken.myperm.common.uix.Fragment3
import eu.darken.myperm.common.viewbinding.viewBinding
import eu.darken.myperm.databinding.PermissionsFragmentBinding
import javax.inject.Inject

@AndroidEntryPoint
class PermissionsFragment : Fragment3(R.layout.permissions_fragment) {

    override val vm: PermissionsFragmentVM by viewModels()
    override val ui: PermissionsFragmentBinding by viewBinding()

    @Inject lateinit var permissionsAdapter: PermissionsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        customNavController = requireActivity().findNavController(R.id.nav_host_main_activity)

        ui.filterAction.setOnClickListener { vm.showFilterDialog() }
        ui.sortAction.setOnClickListener { vm.showSortDialog() }

        vm.events.observe2(ui) { event ->
            when (event) {
                is PermissionListEvent.ShowFilterDialog -> FilterDialog(requireActivity()).show(event.options) { newOptions ->
                    vm.updateFilterOptions { newOptions }
                }
                is PermissionListEvent.ShowSortDialog -> SortDialog(requireActivity()).show(event.options) { newOptions ->
                    vm.updateSortOptions { newOptions }
                }
                is PermissionListEvent.PermissionEvent -> try {
                    event.permAction.execute(requireActivity())
                } catch (e: Exception) {
                    e.asErrorDialogBuilder(requireContext()).show()
                }
            }
        }
        ui.searchInput.addTextChangedListener {
            vm.onSearchInputChanged(it?.toString())
        }

        ui.list.setupDefaults(permissionsAdapter)
        vm.state.observe2(this, ui) { state ->
            list.isInvisible = state is PermissionsFragmentVM.State.Loading
            loadingContainer.isGone = state !is PermissionsFragmentVM.State.Loading

            when (state) {
                is PermissionsFragmentVM.State.Loading -> {
                    listCaption.text = "..."
                }
                is PermissionsFragmentVM.State.Ready -> {
                    val groups = requireContext().getQuantityString(R.plurals.generic_x_groups_label, state.countGroups)
                    val items =
                        requireContext().getQuantityString(R.plurals.generic_x_items_label, state.countPermissions)
                    listCaption.text = "$groups, $items"

                    permissionsAdapter.update(state.listData)
                }
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }
}
