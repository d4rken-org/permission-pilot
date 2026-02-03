package eu.darken.myperm.apps.ui.details

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.getSettingsIntent
import eu.darken.myperm.common.EdgeToEdgeHelper
import eu.darken.myperm.common.error.asErrorDialogBuilder
import eu.darken.myperm.common.lists.differ.update
import eu.darken.myperm.common.lists.setupDefaults
import eu.darken.myperm.common.uix.Fragment3
import eu.darken.myperm.common.viewbinding.viewBinding
import eu.darken.myperm.databinding.AppsDetailsFragmentBinding
import javax.inject.Inject

@AndroidEntryPoint
class AppDetailsFragment : Fragment3(R.layout.apps_details_fragment) {

    override val vm: AppDetailsFragmentVM by viewModels()
    override val ui: AppsDetailsFragmentBinding by viewBinding()

    @Inject lateinit var detailsAdapter: AppDetailsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        EdgeToEdgeHelper(requireActivity()).apply {
            insetsPadding(ui.root, left = true, right = true)
            insetsPadding(ui.toolbar, top = true)
            insetsPadding(ui.list, bottom = true)
        }
        ui.toolbar.apply {
            setupWithNavController(findNavController())
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_item_filter -> {
                        vm.showFilterDialog()
                        true
                    }

                    R.id.menu_item_settings -> {
                        vm.onGoSettings()
                        true
                    }

                    else -> super.onOptionsItemSelected(it)
                }
            }
        }

        vm.events.observe2(ui) { event ->
            when (event) {
                is AppDetailsEvents.ShowAppSystemDetails -> {
                    try {
                        startActivity(event.pkg.getSettingsIntent(requireContext()))
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_SHORT).show()
                    }
                }

                is AppDetailsEvents.PermissionEvent -> try {
                    event.permAction.execute(requireActivity())
                } catch (e: Exception) {
                    e.asErrorDialogBuilder(requireContext()).show()
                }

                is AppDetailsEvents.ShowFilterDialog -> {
                    AppDetailsFilterDialog(requireActivity()).show(event.options) { newOptions ->
                        vm.updateFilterOptions { newOptions }
                    }
                }
            }
        }

        ui.list.setupDefaults(detailsAdapter)
        vm.details.observe2(ui) { details ->
            toolbar.subtitle = details.label

            detailsAdapter.update(details.items)
            list.isVisible = true
            loadingContainer.isGone = details.app != null
            emptyState.isVisible = details.isEmptyDueToFilter
        }
        super.onViewCreated(view, savedInstanceState)
    }
}
