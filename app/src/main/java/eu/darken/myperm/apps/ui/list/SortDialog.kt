package eu.darken.myperm.apps.ui.list

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.darken.myperm.R
import eu.darken.myperm.common.dialog.BaseDialogBuilder

class SortDialog(private val activity: Activity) : BaseDialogBuilder(activity) {

    fun show(
        options: SortOptions,
        onResult: (SortOptions) -> Unit
    ) {
        val itemLabels = SortOptions.Sort.values().map {
            getString(it.labelRes)
        }.toTypedArray<CharSequence>()

        var checkedItem = SortOptions.Sort.values().indexOf(options.mainSort)

        MaterialAlertDialogBuilder(context).apply {
            setTitle(R.string.general_sort_action)

            setSingleChoiceItems(itemLabels, checkedItem) { dialog, which ->
                val new = options.copy(
                    mainSort = SortOptions.Sort.values()[which]
                )
                onResult(new)
                dialog.dismiss()
            }
        }.show()
    }

}