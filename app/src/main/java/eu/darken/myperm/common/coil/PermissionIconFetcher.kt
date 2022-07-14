package eu.darken.myperm.common.coil

import android.content.pm.PackageManager
import android.content.pm.PermissionGroupInfo
import android.content.pm.PermissionInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import eu.darken.myperm.permissions.core.PermissionId
import javax.inject.Inject


class PermissionIconFetcher @Inject constructor(
    private val packageManager: PackageManager,
    private val data: PermissionId,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val drawable = data.getIcon() ?: ColorDrawable(Color.TRANSPARENT)
        return DrawableResult(
            drawable = drawable,
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    private fun PermissionId.getIcon(): Drawable? {
        return try {
            val permissionInfo: PermissionInfo = packageManager.getPermissionInfo(value, 0)

            if (permissionInfo.group == null) {
//                return packageManager.getResourcesForApplication("android").getDrawable(permissionInfo.icon)
                return permissionInfo.loadUnbadgedIcon(packageManager)
            }

            val groupInfo: PermissionGroupInfo = packageManager.getPermissionGroupInfo(permissionInfo.group!!, 0)
            val resources = packageManager.getResourcesForApplication("android")
            ResourcesCompat.getDrawable(
                resources,
                groupInfo.icon,
                resources.newTheme()
            )
        } catch (e: Exception) {
            null
        }
    }

    class Factory @Inject constructor(
        private val packageManager: PackageManager,
    ) : Fetcher.Factory<PermissionId> {

        override fun create(
            data: PermissionId,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = PermissionIconFetcher(packageManager, data, options)
    }
}

