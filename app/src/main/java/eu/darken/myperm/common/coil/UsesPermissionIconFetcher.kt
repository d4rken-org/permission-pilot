package eu.darken.myperm.common.coil

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.permissions.core.PermissionRepo
import kotlinx.coroutines.flow.first
import javax.inject.Inject


class UsesPermissionIconFetcher @Inject constructor(
    private val permissionRepo: PermissionRepo,
    private val data: UsesPermission,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val permission = permissionRepo.permissions.first().singleOrNull { it.id == data.id }
        return DrawableResult(
            drawable = permission?.getIcon(options.context) ?: ColorDrawable(Color.TRANSPARENT),
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory @Inject constructor(
        private val permissionRepo: PermissionRepo,
    ) : Fetcher.Factory<UsesPermission> {

        override fun create(
            data: UsesPermission,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = UsesPermissionIconFetcher(permissionRepo, data, options)
    }
}

