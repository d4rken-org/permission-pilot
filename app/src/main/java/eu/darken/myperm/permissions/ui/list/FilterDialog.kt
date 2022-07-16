package eu.darken.myperm.permissions.ui.list

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.darken.myperm.R
import eu.darken.myperm.common.dialog.BaseDialogBuilder

class FilterDialog(private val activity: Activity) : BaseDialogBuilder(activity) {

    fun show(
        options: FilterOptions,
        onResult: (FilterOptions) -> Unit
    ) {
        val itemLabels = FilterOptions.Filter.values().map {
            getString(it.labelRes)
        }.toTypedArray<CharSequence>()

        val checkedItems = FilterOptions.Filter.values().map {
            options.keys.contains(it)
        }.toBooleanArray()

        MaterialAlertDialogBuilder(context).apply {
            setTitle(R.string.general_filter_action)
            setNegativeButton(R.string.general_cancel_action) { _, _ -> }

            setPositiveButton(android.R.string.ok) { _, _ ->
                val new = options.copy(
                    keys = FilterOptions.Filter.values().filterIndexed { index, item ->
                        checkedItems[index]
                    }.toSet()
                )
                onResult(new)
            }

            setMultiChoiceItems(itemLabels, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
        }.show()
    }

}