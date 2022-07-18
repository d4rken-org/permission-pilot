package eu.darken.myperm.common.coil

import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import eu.darken.myperm.permissions.core.Permission
import javax.inject.Inject


class PermissionIconFetcher @Inject constructor(
    private val packageManager: PackageManager,
    private val data: Permission,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult = DrawableResult(
        drawable = data.getIcon(options.context) ?: ColorDrawable(Color.TRANSPARENT),
        isSampled = false,
        dataSource = DataSource.MEMORY
    )

    class Factory @Inject constructor(
        private val packageManager: PackageManager,
    ) : Fetcher.Factory<Permission> {

        override fun create(
            data: Permission,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = PermissionIconFetcher(packageManager, data, options)
    }
}

