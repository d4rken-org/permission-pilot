package eu.darken.myperm.common.debug.logging

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

/**
 * A file logger that cannot open its writer used to swallow the failure, which left it installed
 * and silently writing nowhere: the recording looked like it had started and produced an empty log.
 */
class FileLoggerTest {

    @TempDir
    lateinit var sessionDir: File

    @Test
    fun `a log file that cannot be opened fails the start`() {
        // core.log occupied by a directory: the writer cannot be opened.
        val logFile = File(sessionDir, "core.log").also { it.mkdirs() }

        val logger = FileLogger(logFile)

        shouldThrow<IOException> { logger.start() }

        // Inert rather than half-started: nothing was published, so neither writing nor stopping
        // does anything.
        logger.log(Logging.Priority.INFO, "tag", "dropped", null)
        logger.stop()
    }

    @Test
    fun `a failed start leaves the logger startable`() {
        val logFile = File(sessionDir, "core.log").also { it.mkdirs() }
        val logger = FileLogger(logFile)
        shouldThrow<IOException> { logger.start() }

        // With the obstruction gone the same logger has to start for real: the failed attempt must
        // not have left a writer reference behind that makes the retry a no-op.
        logFile.deleteRecursively()
        logger.start()
        logger.log(Logging.Priority.INFO, "tag", "recorded", null)
        logger.stop()

        logFile.readText() shouldContain "recorded"
    }

    /**
     * A resumed session appends to the log file of the recording it continues. Cleaning up after a
     * failed open deleted that file unconditionally, so a resume that could not append (a full disk)
     * destroyed the recording the user was about to send.
     */
    @Test
    fun `a failed start keeps a log file it did not create`() {
        val logFile = File(sessionDir, "core.log")
        logFile.writeText("=== BEGIN ===\nprevious recording\n")
        // Read-only: the append cannot be opened, but the file itself is perfectly deletable.
        logFile.setWritable(false, false)
        assumeTrue(!logFile.canWrite(), "The read-only bit is not enforced for this user")

        try {
            val logger = FileLogger(logFile)

            shouldThrow<IOException> { logger.start() }

            // Only a file THIS attempt created may be cleaned up.
            logFile.exists() shouldBe true
            logFile.readText() shouldContain "previous recording"
        } finally {
            logFile.setWritable(true, true)
        }
    }
}
