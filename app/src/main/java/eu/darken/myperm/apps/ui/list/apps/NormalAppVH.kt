package eu.darken.myperm.apps.ui.list.apps

import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import coil.load
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Installers
import eu.darken.myperm.apps.core.InternetAccess
import eu.darken.myperm.apps.core.types.BaseApp
import eu.darken.myperm.apps.core.types.NormalApp
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

    private val defaultTagColor = context.getColorForAttr(R.attr.colorOnBackground)

    private var permissionNavListener: ((Permission) -> Unit)? = null

    override val onBindData: AppsNormalItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        permissionNavListener = item.onShowPermission
        val app = item.app

        packageName.text = app.packageName

        label.apply {
            text = app.label
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

        icon.load(app.id)

        installerSource.apply {
            val installerPkg = app.installerInfo?.initiatingPkg
            if (installerPkg == null) {
                setImageResource(R.drawable.ic_baseline_user_24)
                return@apply
            }

            val knownInstaller = Installers.values().singleOrNull { it.id == installerPkg.id }
            if (knownInstaller?.iconRes != null) {
                setImageResource(knownInstaller.iconRes)
            } else {
                load(installerPkg.id)
            }
        }

        itemView.setOnClickListener { item.onClickAction(item) }

        tagBluetooth.setupAll(
            app,
            AndroidPermissions.BLUETOOTH_ADMIN,
            AndroidPermissions.BLUETOOTH,
            AndroidPermissions.BLUETOOTH_CONNECT,
        )

        tagInternet.apply {
            when (app.internetAccess) {
                InternetAccess.DIRECT -> {
                    setImageResource(R.drawable.ic_baseline_signal_wifi_4_bar_24)
                    tintIt(getColor(R.color.status_positive_1))
                }
                InternetAccess.INDIRECT -> {
                    setImageResource(R.drawable.ic_baseline_signal_wifi_statusbar_connected_no_internet_4_24)
                    tintIt(getColor(R.color.status_negative_1))
                }
                InternetAccess.NONE -> {
                    setImageResource(R.drawable.ic_baseline_signal_wifi_connected_no_internet_4_24)
                    tintIt(defaultTagColor)
                }
            }
            setUpInfoSnackbar(AndroidPermissions.INTERNET)
            isInvisible = app.internetAccess == InternetAccess.NONE
        }

        tagStorage.setupAll(app, AndroidPermissions.WRITE_EXTERNAL_STORAGE, AndroidPermissions.READ_EXTERNAL_STORAGE)

        tagWakelock.setupAll(app, AndroidPermissions.WAKE_LOCK)
        tagVibrate.setupAll(app, AndroidPermissions.VIBRATE)
        tagCamera.setupAll(app, AndroidPermissions.CAMERA)
        tagMicrophone.setupAll(app, AndroidPermissions.RECORD_AUDIO)
        tagContacts.setupAll(app, AndroidPermissions.CONTACTS)

        tagLocation.setupAll(app, AndroidPermissions.LOCATION_FINE, AndroidPermissions.LOCATION_COARSE)

        tagSms.setupAll(app, AndroidPermissions.SMS_READ, AndroidPermissions.SMS_RECEIVE, AndroidPermissions.SMS_SEND)
        tagPhone.setupAll(app, AndroidPermissions.PHONE_CALL, AndroidPermissions.PHONE_STATE)

        tagContainer.isGone = tagContainer.children.all { !it.isVisible }
    }

    private fun ImageView.tintIt(@ColorInt color: Int) {
        setColorFilter(color)
    }

    private fun ImageView.setupAll(
        app: BaseApp,
        vararg permissions: Permission,
        @ColorInt colorGranted: Int = ContextCompat.getColor(context, R.color.status_positive_1),
        @ColorInt colorDenied: Int = ContextCompat.getColor(context, R.color.status_negative_1),
    ) {
        val perms = permissions.map { app.getPermission(it.id) }.filterNotNull()
        val grantedPerm = perms.firstOrNull { it.isGranted }
        isInvisible = when {
            grantedPerm != null -> tintIt(colorGranted).let { false }
            perms.isNotEmpty() -> tintIt(colorDenied).let { false }
            else -> true
        }
        grantedPerm?.let { setUpInfoSnackbar(it) }
    }

    private fun ImageView.setUpInfoSnackbar(permission: Permission) {
        setOnClickListener {
            log { "Permission tag clicked: $permission" }
            permissionNavListener?.invoke(permission)
        }
    }

    data class Item(
        override val app: NormalApp,
        val onClickAction: (Item) -> Unit,
        val onShowPermission: ((Permission) -> Unit),
    ) : AppsAdapter.Item
}