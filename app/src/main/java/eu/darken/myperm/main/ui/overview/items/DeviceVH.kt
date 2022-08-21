package eu.darken.myperm.main.ui.overview.items

import android.view.ViewGroup
import androidx.core.view.isGone
import eu.darken.myperm.R
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.OverviewItemDeviceBinding
import eu.darken.myperm.main.ui.overview.OverviewAdapter

class DeviceVH(parent: ViewGroup) : OverviewAdapter.BaseVH<DeviceVH.Item, OverviewItemDeviceBinding>(
    R.layout.overview_item_device,
    parent
), BindableVH<DeviceVH.Item, OverviewItemDeviceBinding> {

    override val viewBinding = lazy { OverviewItemDeviceBinding.bind(itemView) }

    override val onBindData: OverviewItemDeviceBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = binder@{ item, _ ->
        loadingContainer.isGone = item !is Item.Loading
        if (item !is Item.Content) return@binder

        deviceName.text = item.deviceName
        androidVersion.text = item.androidVersion
        patchLevel.text = item.patchLevel
    }

    sealed class Item : OverviewAdapter.Item {
        override val stableId: Long
            get() = Item::class.java.hashCode().toLong()

        object Loading : Item()

        data class Content(
            val deviceName: String,
            val androidVersion: String,
            val patchLevel: String,
        ) : Item()
    }
}