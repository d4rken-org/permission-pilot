package eu.darken.myperm.common.debug.logging

import android.annotation.SuppressLint
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.time.Instant


@SuppressLint("LogNotTimber")
class FileLogger(private val logFile: File) : Logging.Logger {
    private var logWriter: OutputStreamWriter? = null

    /**
     * A failure here used to be swallowed, which left an installed logger writing nowhere: the
     * recording looked like it had started and produced an empty log. The writer is published only
     * once it is actually usable, and anything else is the caller's failure to handle.
     */
    @SuppressLint("SetWorldReadable")
    @Synchronized
    @Throws(IOException::class)
    fun start() {
        if (logWriter != null) return

        logFile.parentFile!!.mkdirs()
        if (logFile.createNewFile()) {
            Log.i(TAG, "File logger writing to " + logFile.path)
        }
        if (logFile.setReadable(true, false)) {
            Log.i(TAG, "Debug run log read permission set")
        }

        var writer: OutputStreamWriter? = null
        try {
            writer = OutputStreamWriter(FileOutputStream(logFile, true))
            writer.write("=== BEGIN ===\n")
            writer.write("Logfile: $logFile\n")
            writer.flush()
        } catch (e: IOException) {
            Log.e(TAG, "File logger failed to start.", e)
            try {
                writer?.close()
            } catch (ignore: IOException) {
            }
            logFile.delete()
            throw e
        }

        logWriter = writer
        Log.i(TAG, "File logger started.")
    }

    @Synchronized
    fun stop() {
        logWriter?.let {
            logWriter = null
            try {
                it.write("=== END ===\n")
                it.close()
            } catch (ignore: IOException) {
            }
            Log.i(TAG, "File logger stopped.")
        }
    }

    override fun log(priority: Logging.Priority, tag: String, message: String, metaData: Map<String, Any>?) {
        logWriter?.let {
            try {
                it.write("${Instant.ofEpochMilli(System.currentTimeMillis())}  ${priority.shortLabel}/$tag: $message\n")
                it.flush()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to write log line.", e)
                try {
                    it.close()
                } catch (ignore: Exception) {
                }
                logWriter = null
            }
        }
    }

    override fun toString(): String = "FileLogger(file=$logFile)"

    companion object {
        private val TAG = logTag("Debug", "FileLogger")
    }
}

