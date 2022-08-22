package eu.darken.myperm.main.ui.overview

import android.content.Context
import eu.darken.myperm.R
import eu.darken.myperm.common.getQuantityString

data class PkgCount(
    val user: Int,
    val system: Int,
) {
    fun getHR(c: Context): String {
        val sumText = c.getQuantityString(R.plurals.generic_x_apps_label, user + system)
        val userText = c.getQuantityString(R.plurals.generic_x_apps_user_label, user)
        val systemText = c.getQuantityString(R.plurals.generic_x_apps_system_label, system)
        return "$sumText ($userText, $systemText)"
    }
}

