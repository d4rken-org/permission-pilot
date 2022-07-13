package eu.darken.myperm.common.coil

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import javax.inject.Inject

class AppIconFetcher @Inject constructor(
    private val packageManager: PackageManager,
    private val data: PackageInfo,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val drawable = data.applicationInfo?.loadIcon(packageManager) ?: ColorDrawable(Color.TRANSPARENT)
        return DrawableResult(
            drawable = drawable,
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory @Inject constructor(
        private val packageManager: PackageManager,
    ) : Fetcher.Factory<PackageInfo> {

        override fun create(
            data: PackageInfo,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = AppIconFetcher(packageManager, data, options)
    }
}

