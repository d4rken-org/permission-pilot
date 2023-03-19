package eu.darken.myperm.main.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.R
import eu.darken.myperm.common.PrivacyPolicy
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.uix.Fragment3
import eu.darken.myperm.common.viewbinding.viewBinding
import eu.darken.myperm.databinding.OnboardingFragmentBinding
import javax.inject.Inject


@AndroidEntryPoint
class OnboardingFragment : Fragment3(R.layout.onboarding_fragment) {

    override val vm: OnboardingFragmentVM by viewModels()
    override val ui: OnboardingFragmentBinding by viewBinding()
    @Inject lateinit var webpageTool: WebpageTool

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        customNavController = requireActivity().findNavController(R.id.nav_host_main_activity)
        ui.goPrivacyPolicy.setOnClickListener { webpageTool.open(PrivacyPolicy.URL) }
        ui.continueAction.setOnClickListener { vm.finishOnboarding() }
        super.onViewCreated(view, savedInstanceState)
    }
}
