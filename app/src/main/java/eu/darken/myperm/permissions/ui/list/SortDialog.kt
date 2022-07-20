package eu.darken.myperm.permissions.ui.list

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.darken.myperm.R
import eu.darken.myperm.common.dialog.BaseDialogBuilder

class SortDialog(private val activity: Activity) : BaseDialogBuilder(activity) {

    fun show(
        options: PermsSortOptions,
        onResult: (PermsSortOptions) -> Unit
    ) {
        val itemLabels = PermsSortOptions.Sort.values().map {
            getString(it.labelRes)
        }.toTypedArray<CharSequence>()

        var checkedItem = PermsSortOptions.Sort.values().indexOf(options.mainSort)

        MaterialAlertDialogBuilder(context).apply {
            setTitle(R.string.general_sort_action)

            setSingleChoiceItems(itemLabels, checkedItem) { dialog, which ->
                val new = options.copy(
                    mainSort = PermsSortOptions.Sort.values()[which]
                )
                onResult(new)
                dialog.dismiss()
            }
        }.show()
    }

}