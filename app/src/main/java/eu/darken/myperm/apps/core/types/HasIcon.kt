package eu.darken.myperm.apps.core.types

import android.content.Context
import android.graphics.drawable.Drawable

interface HasIcon {
//    @get:DrawableRes val iconRes: Int
//        get() = R.drawable.ic_default_app_icon_24

    fun getIcon(context: Context): Drawable
}