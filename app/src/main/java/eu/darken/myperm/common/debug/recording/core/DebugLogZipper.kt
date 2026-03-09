package eu.darken.myperm.common.debug.recording.core

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.compression.Zipper
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import java.io.File
import javax.inject.Inject

@Reusable
class DebugLogZipper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun zip(logDir: File): File {
        val logFiles = logDir.listFiles()?.filter { it.isFile }?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("No log files in $logDir")

        val zipFile = File(logDir.parentFile, "${logDir.name}.zip")
        val tempFile = File(logDir.parentFile, "${logDir.name}.zip.tmp")

        try {
            Zipper().zip(logFiles.map { it.path }, tempFile.path)
            if (!tempFile.renameTo(zipFile)) {
                tempFile.copyTo(zipFile, overwrite = true)
                tempFile.delete()
            }
            log(TAG) { "Zipped ${logFiles.size} files to $zipFile" }
        } catch (e: Exception) {
            tempFile.delete()
            zipFile.delete()
            throw e
        }

        return zipFile
    }

    fun zipAndGetUri(logDir: File): Uri {
        val zipFile = zip(logDir)
        return getUriForZip(zipFile)
    }

    fun getUriForZip(zipFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            BuildConfigWrap.APPLICATION_ID + ".provider",
            zipFile,
        )
    }

    companion object {
        private val TAG = logTag("Debug", "Log", "Zipper")
    }
}
