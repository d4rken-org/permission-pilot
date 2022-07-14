package eu.darken.myperm.apps.ui.list

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.R
import eu.darken.myperm.common.lists.differ.update
import eu.darken.myperm.common.lists.setupDefaults
import eu.darken.myperm.common.observe2
import eu.darken.myperm.common.uix.Fragment3
import eu.darken.myperm.common.viewbinding.viewBinding
import eu.darken.myperm.databinding.AppsFragmentBinding
import javax.inject.Inject

@AndroidEntryPoint
class AppsFragment : Fragment3(R.layout.apps_fragment) {

    override val vm: AppsFragmentVM by viewModels()
    override val ui: AppsFragmentBinding by viewBinding()

    @Inject lateinit var appsAdapter: AppsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        customNavController = requireActivity().findNavController(R.id.nav_host_main_activity)

        ui.list.setupDefaults(appsAdapter)

        vm.listData.observe2(this@AppsFragment, ui) {
            appsAdapter.update(it)
        }

        super.onViewCreated(view, savedInstanceState)
    }
}
