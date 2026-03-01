package eu.darken.myperm.common.support

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.compression.Zipper
import java.io.File
import javax.inject.Inject

@Reusable
class DebugLogZipper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun zipAndGetUri(logFile: File): Uri {
        require(logFile.exists()) { "Log file does not exist: $logFile" }

        val zipPath = logFile.path + ".zip"
        Zipper().zip(arrayOf(logFile.path), zipPath)

        return FileProvider.getUriForFile(
            context,
            BuildConfigWrap.APPLICATION_ID + ".provider",
            File(zipPath)
        )
    }
}
