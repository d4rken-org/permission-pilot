package eu.darken.myperm.apps.core.features

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg

interface AppStore : DisplayablePkg {
    @get:StringRes val labelRes: Int
    @get:DrawableRes val iconRes: Int
        get() = R.drawable.ic_default_app_icon_24

    override fun getLabel(context: Context): String = context.getString(labelRes)

    override fun getIcon(context: Context): Drawable = ContextCompat.getDrawable(context, iconRes)!!

    val urlGenerator: ((Pkg.Id) -> String)?
        get() = null
}