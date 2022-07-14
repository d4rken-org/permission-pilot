package eu.darken.myperm.apps.ui.details

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.R
import eu.darken.myperm.common.uix.Fragment3
import eu.darken.myperm.common.viewbinding.viewBinding
import eu.darken.myperm.databinding.AppsDetailsFragmentBinding

@AndroidEntryPoint
class AppDetailsFragment : Fragment3(R.layout.apps_details_fragment) {

    override val vm: AppDetailsFragmentVM by viewModels()
    override val ui: AppsDetailsFragmentBinding by viewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm.details.observe2(ui) { details ->

            toolbar.title = details.label
            toolbar.subtitle = details.app.id
        }
        super.onViewCreated(view, savedInstanceState)
    }
}
