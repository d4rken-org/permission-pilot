package eu.darken.myperm.apps.ui.details.items

import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import coil.load
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.tryLabel
import eu.darken.myperm.apps.core.types.NormalApp
import eu.darken.myperm.apps.ui.details.AppDetailsAdapter
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.AppsDetailsOverviewItemBinding

class AppOverviewVH(parent: ViewGroup) : AppDetailsAdapter.BaseVH<AppOverviewVH.Item, AppsDetailsOverviewItemBinding>(
    R.layout.apps_details_overview_item,
    parent
), BindableVH<AppOverviewVH.Item, AppsDetailsOverviewItemBinding> {

    override val viewBinding = lazy { AppsDetailsOverviewItemBinding.bind(itemView) }

    override val onBindData: AppsDetailsOverviewItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val app = item.app

        icon.load(app)
        label.text = app.label
        identifier.text = app.id.toString()

        description.apply {
            val countTotal = app.requestedPermissions.size
            val grantedCount = app.requestedPermissions.count { it.isGranted }
            text = "$grantedCount of $countTotal permissions granted."
        }

        installerInfo.apply {
            val info = app.installerInfo

            installerIcon.setImageDrawable(info.getIcon(context))

            if (info.allInstallers.isEmpty()) {
                text = info.getLabel(context)
            } else {
                val ssb = SpannableStringBuilder().apply {
                    info.allInstallers.forEach { pkg ->
                        val onClick = object : ClickableSpan() {
                            override fun onClick(widget: View) = item.onInstallerClicked(pkg)
                        }
                        var _label = pkg.tryLabel(context) ?: pkg.id.toString()
                        if (pkg != info.allInstallers.last()) _label += "\n"
                        append(_label, onClick, 0)
                    }
                }
                movementMethod = LinkMovementMethod.getInstance()
                setText(ssb, TextView.BufferType.SPANNABLE)
            }
        }

        tagSystem.isInvisible = !app.isSystemApp
        tagContainer.isGone = tagContainer.children.all { !it.isVisible }
    }

    data class Item(
        val app: NormalApp,
        val onInstallerClicked: (Pkg) -> Unit
    ) : AppDetailsAdapter.Item {
        override val stableId: Long
            get() = Item::class.hashCode().toLong()
    }

}