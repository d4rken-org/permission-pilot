package eu.darken.myperm.export.core

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportWriter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) {

    suspend fun write(uri: Uri, content: String): Result<Unit> = withContext(dispatcherProvider.IO) {
        runCatching {
            log(TAG) { "Writing ${content.length} chars to $uri" }
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw IOException("Could not open output stream for $uri")
            outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
            log(TAG) { "Write complete" }
        }
    }

    companion object {
        private val TAG = logTag("Export", "Writer")
    }
}
