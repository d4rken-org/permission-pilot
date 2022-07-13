package eu.darken.myperm.common.coil

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.debug.logging.Logging
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import javax.inject.Provider
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class CoilModule {

    @Provides
    fun imageLoader(
        @ApplicationContext context: Context,
        appIconFetcherFactory: AppIconFetcher.Factory,
    ): ImageLoader = ImageLoader.Builder(context).apply {

        if (BuildConfigWrap.DEBUG) {
            val logger = object : Logger {
                override var level: Int = Log.VERBOSE
                override fun log(tag: String, priority: Int, message: String?, throwable: Throwable?) {
                    log("Coil:$tag", Logging.Priority.fromAndroid(priority)) { "$message ${throwable?.asLog()}" }
                }
            }
            logger(logger)
        }
        components {
            add(appIconFetcherFactory)
        }
    }.build()

    @Singleton
    @Provides
    fun imageLoaderFactory(imageLoaderSource: Provider<ImageLoader>): ImageLoaderFactory = ImageLoaderFactory {
        log(TAG) { "Preparing imageloader factory" }
        imageLoaderSource.get()
    }

    companion object {
        private val TAG = logTag("Coil", "Module")
    }
}
