package eu.darken.myperm.apps.ui.details.items

import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.types.BaseApp
import eu.darken.myperm.apps.core.types.NormalApp
import eu.darken.myperm.apps.ui.details.AppDetailsAdapter
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.AppsDetailsSiblingsItemBinding

class AppSiblingsVH(parent: ViewGroup) : AppDetailsAdapter.BaseVH<AppSiblingsVH.Item, AppsDetailsSiblingsItemBinding>(
    R.layout.apps_details_siblings_item,
    parent
), BindableVH<AppSiblingsVH.Item, AppsDetailsSiblingsItemBinding> {

    override val viewBinding = lazy { AppsDetailsSiblingsItemBinding.bind(itemView) }

    override val onBindData: AppsDetailsSiblingsItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val app = item.app

        val idLabel = app.packageInfo.applicationInfo?.let { appInfo ->
            val labelId = app.packageInfo.sharedUserLabel
            if (labelId == 0) return@let null
            context.packageManager.getText(
                app.packageName,
                labelId,
                appInfo
            )
        }?.toString()

        shareduseridInfo.text = if (idLabel != null) {
            "$idLabel (${app.sharedUserId})"
        } else {
            app.sharedUserId
        }

        siblingsInfo.apply {
            val ssb = SpannableStringBuilder()
            app.siblings.forEach { sibling ->
                var txt = "${sibling.id} (${sibling.label ?: "?"})"
                if (app.siblings.last() != sibling) txt += "\n"

                val onClick = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        item.onSiblingClicked(sibling)
                    }
                }
                ssb.append(txt, onClick, 0)
            }
            movementMethod = LinkMovementMethod.getInstance()
            setText(ssb, TextView.BufferType.SPANNABLE)
        }
    }

    data class Item(
        val app: NormalApp,
        val onSiblingClicked: (BaseApp) -> Unit,
    ) : AppDetailsAdapter.Item {
        override val stableId: Long
            get() = Item::class.hashCode().toLong()
    }

}