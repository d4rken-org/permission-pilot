package eu.darken.myperm.common.coil

import android.view.View
import android.widget.ImageView
import androidx.core.view.isInvisible
import coil.imageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.permissions.core.container.BasePermission

fun ImageRequest.Builder.loadingView(
    imageView: View,
    loadingView: View
) {
    listener(
        onStart = {
            loadingView.isInvisible = false
            imageView.isInvisible = true
        },
        onSuccess = { _, _ ->
            loadingView.isInvisible = true
            imageView.isInvisible = false
        }
    )
}

fun ImageView.loadAppIcon(pkg: Pkg): Disposable? {
    val current = tag as? Pkg
    if (current?.id == pkg.id) return null
    tag = pkg

    val request = ImageRequest.Builder(context).apply {
        data(pkg)
        target(this@loadAppIcon)
    }.build()

    return context.imageLoader.enqueue(request)
}

fun ImageView.loadPermissionIcon(permission: UsesPermission): Disposable? {
    val current = tag as? UsesPermission
    if (current?.id == permission.id) return null
    tag = permission

    val request = ImageRequest.Builder(context).apply {
        data(permission)
        target(this@loadPermissionIcon)
    }.build()

    return context.imageLoader.enqueue(request)
}

fun ImageView.loadPermissionIcon(permission: BasePermission): Disposable? {
    val current = tag as? BasePermission
    if (current?.id == permission.id) return null
    tag = permission

    val request = ImageRequest.Builder(context).apply {
        data(permission)
        target(this@loadPermissionIcon)
    }.build()

    return context.imageLoader.enqueue(request)
}