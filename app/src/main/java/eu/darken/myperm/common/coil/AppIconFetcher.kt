package eu.darken.myperm.common.coil

import android.content.pm.PackageManager
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import eu.darken.myperm.apps.core.getPackageInfo2
import eu.darken.myperm.apps.core.types.Pkg
import javax.inject.Inject

class AppIconFetcher @Inject constructor(
    private val packageManager: PackageManager,
    private val data: Pkg.Id,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val packageInfo = packageManager.getPackageInfo2(data.value)
            ?: throw IllegalArgumentException("Not found $data")
        val drawable = packageInfo.applicationInfo?.loadIcon(packageManager)
            ?: throw IllegalArgumentException("Has no icon $data")
        return DrawableResult(
            drawable = drawable,
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory @Inject constructor(
        private val packageManager: PackageManager,
    ) : Fetcher.Factory<Pkg.Id> {

        override fun create(
            data: Pkg.Id,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = AppIconFetcher(packageManager, data, options)
    }
}

