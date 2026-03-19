package eu.darken.myperm.apps.core

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcelable
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import androidx.core.net.toUri
import eu.darken.myperm.apps.core.known.AKnownPkg
import kotlinx.parcelize.Parcelize

interface Pkg {
    val id: Id

    val packageName: Name
        get() = id.pkgName

    fun getLabel(context: Context): String? {
        context.packageManager.getLabel2(id)?.let { return it }

        AKnownPkg.values
            .singleOrNull { it.id == id }
            ?.labelRes
            ?.let { return context.getString(it) }

        return null
    }

    fun getIcon(context: Context): Drawable? {
        context.packageManager.getIcon2(id)?.let { return it }

        return null
    }

    @JvmInline
    @Parcelize
    value class Name(val value: String) : Parcelable {
        override fun toString(): String = value
    }

    @Parcelize
    data class Id(
        val pkgName: Name,
        val userHandle: UserHandle = Process.myUserHandle(),
    ) : Parcelable {
        override fun toString(): String = pkgName.value
    }

    data class Container(override val id: Id) : Pkg
}

fun Pkg.Id.toContainer(): Pkg.Container = Pkg.Container(this)

fun Pkg.Name.toPackageUri(): Uri = "package:${value}".toUri()

fun Pkg.Name.toNotificationId(): Int = value.hashCode()

fun Pkg.getSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = packageName.toPackageUri()
    }