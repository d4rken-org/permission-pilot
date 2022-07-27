package eu.darken.myperm.main.ui.overview

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.R
import eu.darken.myperm.common.lists.differ.update
import eu.darken.myperm.common.lists.setupDefaults
import eu.darken.myperm.common.observe2
import eu.darken.myperm.common.uix.Fragment3
import eu.darken.myperm.common.viewbinding.viewBinding
import eu.darken.myperm.databinding.OverviewFragmentBinding
import javax.inject.Inject

@AndroidEntryPoint
class OverviewFragment : Fragment3(R.layout.overview_fragment) {

    override val vm: OverviewFragmentVM by viewModels()
    override val ui: OverviewFragmentBinding by viewBinding()

    @Inject lateinit var overviewAdapter: OverviewAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        customNavController = requireActivity().findNavController(R.id.nav_host_main_activity)


        ui.list.setupDefaults(overviewAdapter)
        vm.listData.observe2(this@OverviewFragment, ui) { state ->
            overviewAdapter.update(state.items)
            list.isInvisible = state.isLoading
            loadingContainer.isGone = !state.isLoading
        }

        super.onViewCreated(view, savedInstanceState)
    }
}
