package eu.darken.myperm.permissions.core

import android.app.Activity
import android.widget.Toast
import eu.darken.myperm.R

sealed class PermissionAction {

    abstract val permission: Permission

    data class None(override val permission: Permission) : PermissionAction() {
        fun showInfo(activity: Activity) {
            Toast.makeText(activity, R.string.permissions_action_none_msg, Toast.LENGTH_SHORT).show()
        }
    }
}