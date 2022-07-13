package eu.darken.myperm.settings.ui.general

import androidx.annotation.Keep
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.myperm.R
import eu.darken.myperm.common.uix.PreferenceFragment2
import eu.darken.myperm.settings.GeneralSettings
import javax.inject.Inject

@Keep
@AndroidEntryPoint
class GeneralSettingsFragment : PreferenceFragment2() {

    private val vdc: GeneralSettingsFragmentVM by viewModels()

    @Inject lateinit var debugSettings: GeneralSettings

    override val settings: GeneralSettings by lazy { debugSettings }
    override val preferenceFile: Int = R.xml.preferences_general


}