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
import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.apps.core.features.SecondaryPkg
import eu.darken.myperm.apps.ui.details.AppDetailsAdapter
import eu.darken.myperm.common.AndroidVersionCodes
import eu.darken.myperm.common.DividerItemDecorator2
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.AppsDetailsOverviewItemBinding
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class AppOverviewVH(parent: ViewGroup) : AppDetailsAdapter.BaseVH<AppOverviewVH.Item, AppsDetailsOverviewItemBinding>(
    R.layout.apps_details_overview_item,
    parent
), BindableVH<AppOverviewVH.Item, AppsDetailsOverviewItemBinding>, DividerItemDecorator2.SkipDivider {

    override val viewBinding = lazy { AppsDetailsOverviewItemBinding.bind(itemView) }

    private val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    override val onBindData: AppsDetailsOverviewItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val app = item.app

        icon.apply {
            load(app)
            setOnClickListener { item.onGoToSettings(item.app) }
        }

        label.text = app.getLabel(context)
        identifier.text = app.id.toString()

        version.text = "${app.versionName} (${app.versionCode})"

        description.apply {
            val countTotal = app.requestedPermissions.size
            text = if (app is SecondaryPkg) {
                getString(R.string.apps_details_description_secondary_description, countTotal) + "\n" +
                        getString(R.string.apps_details_description_restrictions_caveat_description)
            } else {
                val grantedCount = app.requestedPermissions.count { it.isGranted }
                getString(R.string.apps_details_description_primary_description, grantedCount, countTotal)
            }
        }

        updatedAt.apply {
            text = app.updatedAt?.let { getString(R.string.updated_at_x, dateFormatter.format(it)) }
            isGone = text.isEmpty()
        }

        installedAt.apply {
            text = app.installedAt?.let { getString(R.string.installed_at_x, dateFormatter.format(it)) }
            isGone = text.isEmpty()
        }

        apiTargetLevel.apply {
            text = app.apiTargetLevel?.let { level ->
                val hr = AndroidVersionCodes.values().singleOrNull { it.apiLevel == level }?.longFormat
                    ?: "? (?) [$level]"
                getString(R.string.api_target_level_x, hr)
            }
            isGone = text.isEmpty()
        }

        apiMinimumLevel.apply {
            text = app.apiMinimumLevel?.let { level ->
                val hr = AndroidVersionCodes.values().singleOrNull { it.apiLevel == level }?.longFormat
                    ?: "? (?) [$level]"
                getString(R.string.api_minimum_level_x, hr)
            }
            isGone = text.isEmpty()
        }

        apiBuildLevel.apply {
            text = app.apiCompileLevel?.let { level ->
                val hr = AndroidVersionCodes.values().singleOrNull { it.apiLevel == level }?.longFormat
                    ?: "? (?) [$level]"
                getString(R.string.api_build_level_x, hr)
            }
            isGone = text.isEmpty()
        }

        installerInfo.apply {
            val info = app.installerInfo
            log { "Showing installerInfo=$info" }

            installerIcon.apply {
                setImageDrawable(info.getIcon(context))
                info.installer?.let { pkg -> setOnClickListener { item.onInstallerIconClicked(pkg) } }
            }

            if (info.allInstallers.isEmpty()) {
                text = info.getLabel(context)
            } else {
                val ssb = SpannableStringBuilder().apply {
                    info.allInstallers.toSet().forEach { pkg ->
                        val onClick = object : ClickableSpan() {
                            override fun onClick(widget: View) = item.onInstallerTextClicked(pkg)
                        }
                        var _label = pkg.getLabel(context) ?: pkg.id.toString()
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
        val app: BasePkg,
        val onGoToSettings: (Pkg) -> Unit,
        val onInstallerIconClicked: (Pkg) -> Unit,
        val onInstallerTextClicked: (Pkg) -> Unit,
    ) : AppDetailsAdapter.Item {
        override val stableId: Long
            get() = Item::class.hashCode().toLong()
    }

}