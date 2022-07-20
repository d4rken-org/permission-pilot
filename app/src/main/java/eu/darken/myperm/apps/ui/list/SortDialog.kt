package eu.darken.myperm.apps.ui.list

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.darken.myperm.R
import eu.darken.myperm.common.dialog.BaseDialogBuilder

class SortDialog(private val activity: Activity) : BaseDialogBuilder(activity) {

    fun show(
        options: AppsSortOptions,
        onResult: (AppsSortOptions) -> Unit
    ) {
        val itemLabels = AppsSortOptions.Sort.values().map {
            getString(it.labelRes)
        }.toTypedArray<CharSequence>()

        var checkedItem = AppsSortOptions.Sort.values().indexOf(options.mainSort)

        MaterialAlertDialogBuilder(context).apply {
            setTitle(R.string.general_sort_action)

            setSingleChoiceItems(itemLabels, checkedItem) { dialog, which ->
                val new = options.copy(
                    mainSort = AppsSortOptions.Sort.values()[which]
                )
                onResult(new)
                dialog.dismiss()
            }
        }.show()
    }

}