package eu.darken.myperm.settings.ui.general

import android.os.Bundle
import android.view.View
import androidx.annotation.Keep
import androidx.fragment.app.viewModels
import androidx.preference.CheckBoxPreference
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.R
import eu.darken.myperm.common.uix.PreferenceFragment2
import eu.darken.myperm.settings.core.GeneralSettings
import javax.inject.Inject

@Keep
@AndroidEntryPoint
class GeneralSettingsFragment : PreferenceFragment2() {

    private val vm: GeneralSettingsFragmentVM by viewModels()

    @Inject lateinit var debugSettings: GeneralSettings

    override val settings: GeneralSettings by lazy { debugSettings }
    override val preferenceFile: Int = R.xml.preferences_general

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm.isAutoReporting.observe2 {
            findPreference<CheckBoxPreference>("core.bugtracking.enabled")?.isChecked = it
        }
        super.onViewCreated(view, savedInstanceState)
    }
}