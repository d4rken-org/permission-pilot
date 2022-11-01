package eu.darken.myperm.common.coil

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import eu.darken.myperm.common.IPCFunnel
import eu.darken.myperm.permissions.core.Permission
import javax.inject.Inject


class PermissionIconFetcher @Inject constructor(
    private val ipcFunnel: IPCFunnel,
    private val data: Permission,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult = DrawableResult(
        drawable = ipcFunnel.execute { data.getIcon(options.context) } ?: ColorDrawable(Color.TRANSPARENT),
        isSampled = false,
        dataSource = DataSource.MEMORY
    )

    class Factory @Inject constructor(
        private val ipcFunnel: IPCFunnel,
    ) : Fetcher.Factory<Permission> {

        override fun create(
            data: Permission,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = PermissionIconFetcher(ipcFunnel, data, options)
    }
}

