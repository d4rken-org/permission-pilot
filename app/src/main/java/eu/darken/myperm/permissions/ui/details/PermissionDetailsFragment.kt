package eu.darken.myperm.permissions.ui.details

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.R
import eu.darken.myperm.common.uix.Fragment3
import eu.darken.myperm.common.viewbinding.viewBinding
import eu.darken.myperm.databinding.PermissionsDetailsFragmentBinding

@AndroidEntryPoint
class PermissionDetailsFragment : Fragment3(R.layout.permissions_details_fragment) {

    override val vm: PermissionDetailsFragmentVM by viewModels()
    override val ui: PermissionsDetailsFragmentBinding by viewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm.details.observe2(ui) { details ->

            toolbar.title = details.label
            toolbar.subtitle = details.perm.id
        }
        super.onViewCreated(view, savedInstanceState)
    }
}
