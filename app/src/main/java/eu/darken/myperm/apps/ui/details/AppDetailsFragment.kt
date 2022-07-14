package eu.darken.myperm.apps.ui.details

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.R
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
        ui.list.setupDefaults(detailsAdapter)
        ui.toolbar.setupWithNavController(findNavController())

        vm.details.observe2(ui) { details ->
            toolbar.title = details.label
            toolbar.subtitle = details.app.id

            detailsAdapter.update(details.items)
        }
        super.onViewCreated(view, savedInstanceState)
    }
}
