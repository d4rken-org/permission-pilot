package eu.darken.myperm.permissions.ui.list

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
import eu.darken.myperm.databinding.PermissionsFragmentBinding
import javax.inject.Inject

@AndroidEntryPoint
class PermissionsFragment : Fragment3(R.layout.permissions_fragment) {

    override val vm: PermissionsFragmentVM by viewModels()
    override val ui: PermissionsFragmentBinding by viewBinding()

    @Inject lateinit var permissionsAdapter: PermissionsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        customNavController = requireActivity().findNavController(R.id.nav_host_main_activity)
        ui.list.setupDefaults(permissionsAdapter)

        vm.listData.observe2(this, ui) {
            permissionsAdapter.update(it)
        }
        super.onViewCreated(view, savedInstanceState)
    }
}
