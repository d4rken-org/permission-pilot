package eu.darken.myperm.apps.ui.details.items

import android.view.ViewGroup
import androidx.core.view.isGone
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.container.BasicPkgContainer
import eu.darken.myperm.apps.core.getSettingsIntent
import eu.darken.myperm.apps.ui.details.AppDetailsAdapter
import eu.darken.myperm.common.DividerItemDecorator2
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.AppsDetailsSiblingsItemBinding
import eu.darken.myperm.databinding.AppsDetailsSiblingsItemSiblingBinding

class AppSiblingsVH(parent: ViewGroup) : AppDetailsAdapter.BaseVH<AppSiblingsVH.Item, AppsDetailsSiblingsItemBinding>(
    R.layout.apps_details_siblings_item,
    parent
), BindableVH<AppSiblingsVH.Item, AppsDetailsSiblingsItemBinding>, DividerItemDecorator2.SkipDivider {

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

        siblingsContainer.removeAllViews()
        app.siblings.forEach { sibling ->
            AppsDetailsSiblingsItemSiblingBinding.inflate(layoutInflater, siblingsContainer, false).apply {
                icon.apply {
                    setImageDrawable(sibling.getIcon(context))
                    setOnClickListener {
                        try {
                            context.startActivity(sibling.getSettingsIntent(context))
                        } catch (_: Exception) {
                        }
                    }
                }

                label.text = sibling.getLabel(context)
                identifier.text = sibling.id.pkgName

                this.root.setOnClickListener { item.onSiblingClicked(sibling) }

                siblingsContainer.addView(this.root)
            }
        }

        siblingsContainer.isGone = app.siblings.size > 5 || app.siblings.isEmpty()

        collapseToggle.apply {
            setOnClickListener {
                siblingsContainer.isGone = !siblingsContainer.isGone
                collapseToggle.setIconResource(
                    if (siblingsContainer.isGone) R.drawable.ic_baseline_expand_more_24 else R.drawable.ic_baseline_expand_less_24
                )
            }
            setIconResource(
                if (siblingsContainer.isGone) R.drawable.ic_baseline_expand_more_24 else R.drawable.ic_baseline_expand_less_24
            )
            isGone = item.app.siblings.isEmpty()
        }

    }

    data class Item(
        val app: BasicPkgContainer,
        val onSiblingClicked: (Pkg) -> Unit,
    ) : AppDetailsAdapter.Item {
        override val stableId: Long
            get() = Item::class.hashCode().toLong()
    }

}