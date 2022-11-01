package eu.darken.myperm.common.coil

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.pxOrElse
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.Installed
import eu.darken.myperm.common.IPCFunnel
import eu.darken.myperm.common.dpToPx
import eu.darken.myperm.common.getColorForAttr
import javax.inject.Inject

class AppIconFetcher @Inject constructor(
    private val ipcFunnel: IPCFunnel,
    private val data: Pkg,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val baseIcon = ipcFunnel.execute { data.getIcon(options.context) } ?: ColorDrawable(Color.TRANSPARENT)

        var isSampled = false

        val finalIcon = when {
            data is Installed && data.isSystemApp -> {
                isSampled = true

                val badgeIcon = ContextCompat.getDrawable(options.context, R.drawable.ic_baseline_shield_24)!!.apply {
                    setTint(options.context.getColorForAttr(R.attr.colorError))
                }

                LayerDrawable(arrayOf(baseIcon, badgeIcon)).apply {
                    val fallbackSize = options.context.dpToPx(44f)
                    val targetSize = options.size

                    setLayerSize(
                        0,
                        targetSize.height.pxOrElse { fallbackSize },
                        targetSize.width.pxOrElse { fallbackSize }
                    )

                    val badgeSize = targetSize.width.pxOrElse { fallbackSize } * 0.15f
                    val badgeSizeDp = options.context.dpToPx(badgeSize)
                    setLayerSize(1, badgeSizeDp, badgeSizeDp)
                    setLayerGravity(1, GravityCompat.START or Gravity.TOP)
                }
            }
            else -> baseIcon
        }

        return DrawableResult(
            drawable = finalIcon,
            isSampled = isSampled,
            dataSource = DataSource.DISK
        )
    }

    class Factory @Inject constructor(
        private val ipcFunnel: IPCFunnel,
    ) : Fetcher.Factory<Pkg> {

        override fun create(
            data: Pkg,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = AppIconFetcher(ipcFunnel, data, options)
    }
}

