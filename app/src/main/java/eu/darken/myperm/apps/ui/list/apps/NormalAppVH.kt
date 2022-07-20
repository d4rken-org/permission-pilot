package eu.darken.myperm.apps.ui.list.apps

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import coil.dispose
import coil.load
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.container.BasicPkgContainer
import eu.darken.myperm.apps.core.container.isOrHasProfiles
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.apps.ui.list.AppsAdapter
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.getColorForAttr
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.AppsNormalItemBinding
import eu.darken.myperm.permissions.core.AndroidPermissions
import eu.darken.myperm.permissions.core.Permission

class NormalAppVH(parent: ViewGroup) : AppsAdapter.BaseVH<NormalAppVH.Item, AppsNormalItemBinding>(
    R.layout.apps_normal_item,
    parent
), BindableVH<NormalAppVH.Item, AppsNormalItemBinding> {

    override val viewBinding = lazy { AppsNormalItemBinding.bind(itemView) }

    @ColorInt private val colorGranted = context.getColorForAttr(R.attr.colorPrimary)
    @ColorInt private val colorDenied = context.getColorForAttr(R.attr.colorOnBackground)

    override val onBindData: AppsNormalItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val app = item.app

        packageName.text = app.packageName

        label.apply {
            text = app.getLabel(context)
            isSelected = true
        }

        permissionInfo.apply {
            val grantedCount = app.requestedPermissions.count { it.isGranted }
            val countTotal = app.requestedPermissions.size
            text = getString(R.string.apps_permissions_x_of_x_granted, grantedCount, countTotal)

            val declaredCount = app.declaredPermissions.size
            if (declaredCount > 0) {
                append(" " + getString(R.string.apps_permissions_declares_x, declaredCount))
            }
        }

        icon.apply {
            load(app)
            setOnClickListener { item.onIconClicked(item.app) }
        }

        installerSource.apply {
            val info = app.installerInfo

            if (info.installer != null) {
                load(info.installer)
            } else {
                dispose()
                setImageDrawable(info.getIcon(context))
            }
        }

        itemView.setOnClickListener { item.onRowClicked(item.app) }

        tagSystem.apply {
            isInvisible = !app.isSystemApp
            setOnClickListener {
                Toast.makeText(context, R.string.app_type_system_label, Toast.LENGTH_SHORT).show()
            }
        }

        tagWorkprofile.isInvisible = !app.isOrHasProfiles()

        tagSharedid.isInvisible = app.siblings.isEmpty()

        tagInternet.apply {
            when (app.internetAccess) {
                InternetAccess.DIRECT -> tintIt(colorGranted)
                InternetAccess.INDIRECT -> tintIt(context.getColorForAttr(R.attr.colorTertiary))
                InternetAccess.NONE -> tintIt(colorDenied)
            }
            setupTagClicks(item, AndroidPermissions.INTERNET)
            isInvisible = app.internetAccess == InternetAccess.NONE
        }

        tagStorage.setupAll(item, AndroidPermissions.WRITE_EXTERNAL_STORAGE, AndroidPermissions.READ_EXTERNAL_STORAGE)

        tagBluetooth.setupAll(
            item,
            AndroidPermissions.BLUETOOTH_ADMIN,
            AndroidPermissions.BLUETOOTH,
            AndroidPermissions.BLUETOOTH_CONNECT,
        )

        tagWakelock.setupAll(item, AndroidPermissions.WAKE_LOCK)
        tagVibrate.setupAll(item, AndroidPermissions.VIBRATE)
        tagCamera.setupAll(item, AndroidPermissions.CAMERA)
        tagMicrophone.setupAll(item, AndroidPermissions.RECORD_AUDIO)
        tagContacts.setupAll(item, AndroidPermissions.CONTACTS)

        tagLocation.setupAll(item, AndroidPermissions.LOCATION_FINE, AndroidPermissions.LOCATION_COARSE)

        tagSms.setupAll(item, AndroidPermissions.SMS_READ, AndroidPermissions.SMS_RECEIVE, AndroidPermissions.SMS_SEND)
        tagPhone.setupAll(item, AndroidPermissions.PHONE_CALL, AndroidPermissions.PHONE_STATE)

        tagContainer.isGone = tagContainer.children.all { !it.isVisible }
    }

    private fun ImageView.tintIt(@ColorInt color: Int) {
        setColorFilter(color)
    }

    private fun ImageView.setupAll(
        item: Item,
        vararg permissions: Permission,
        @ColorInt grantedcolor: Int = colorGranted,
        @ColorInt deniedColor: Int = colorDenied,
    ) {
        val perms = permissions.mapNotNull { item.app.getPermission(it.id) }
        val grantedPerm = perms.firstOrNull { it.isGranted }
        isInvisible = when {
            grantedPerm != null -> {
                tintIt(grantedcolor)
                alpha = 1.0f
                false
            }
            perms.isNotEmpty() -> {
                tintIt(deniedColor)
                alpha = 0.4f
                false
            }
            else -> true
        }
        permissions.firstOrNull()?.let { setupTagClicks(item, it) }
    }

    private fun ImageView.setupTagClicks(item: Item, permission: Permission) {
        setOnClickListener {
            log { "Permission tag clicked: $permission ($item)" }
            item.onTagClicked(permission)
        }
        setOnLongClickListener {
            log { "Permission tag long-clicked: $permission ($item)" }
            item.onTagLongClicked(permission)
            true
        }
    }

    data class Item(
        override val app: BasicPkgContainer,
        val onIconClicked: (Pkg) -> Unit,
        val onRowClicked: (Pkg) -> Unit,
        val onTagClicked: (Permission) -> Unit,
        val onTagLongClicked: (Permission) -> Unit,
    ) : AppsAdapter.Item
}