package eu.darken.myperm.common.dialog

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes

abstract class BaseDialogBuilder(private val activity: Activity) {

    fun getString(@StringRes stringRes: Int) = activity.getString(stringRes)

    val context: Context
        get() = activity
}