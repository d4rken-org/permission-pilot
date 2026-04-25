package eu.darken.myperm.apps.core.manifest

import androidx.annotation.VisibleForTesting
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.manifest.binaryxml.BinaryXmlException
import eu.darken.myperm.apps.core.manifest.binaryxml.BinaryXmlStreamer
import eu.darken.myperm.apps.core.manifest.binaryxml.CompositeVisitor
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streaming APK manifest reader.
 *
 * Two entry points:
 * - [readQueries] — lightweight path for the hint scanner; runs a single streamer pass with
 *   [QueriesExtractor] only. No XML is rendered, no resource resolution.
 * - [readFullManifest] — viewer path; composite visitor produces both per-section pretty
 *   XML (via [ManifestSectionVisitor]) and the [QueriesInfo] projection in a single parse.
 *
 * `resources.arsc` is never read. Symbolic resource names for the viewer are resolved through
 * [ResourceNameResolver], which delegates to `PackageManager.getResourcesForApplication` (public
 * API, zero Java heap cost).
 *
 * Memory safety: each parse allocates ~150-200 KB transient (manifest bytes + string pool).
 * No preflight heap budgeting — actual OOM during `ByteArray` allocation or parse is caught and
 * classified as [UnavailableReason.LOW_MEMORY]. Real file errors (not found, unreadable, zip
 * corruption) are classified up front.
 */
@Singleton
class ApkManifestReader @Inject constructor(
    private val resourceNameResolver: ResourceNameResolver,
) {

    /** Scanner path. Returns the `<queries>` projection. */
    fun readQueries(apkPath: String): QueriesOutcome = withManifestBytes(
        apkPath = apkPath,
        onUnavailable = { QueriesOutcome.Unavailable(it) },
    ) { bytes ->
        try {
            val extractor = QueriesExtractor()
            BinaryXmlStreamer().parse(bytes, extractor)
            QueriesOutcome.Success(extractor.result())
        } catch (oom: OutOfMemoryError) {
            log(TAG, WARN) { "OOM during manifest parse: $oom" }
            QueriesOutcome.Unavailable(UnavailableReason.LOW_MEMORY)
        } catch (e: BinaryXmlException) {
            // Structural rejection from the streaming parser is stable across attempts —
            // classify as MALFORMED_APK so the hint scanner caches the negative outcome
            // instead of re-parsing the same broken APK on every run.
            log(TAG, WARN) { "Binary XML parse failed (treating as malformed): $e" }
            QueriesOutcome.Unavailable(UnavailableReason.MALFORMED_APK)
        } catch (e: Exception) {
            log(TAG, WARN) { "Unexpected parse error: $e" }
            QueriesOutcome.Failure(e)
        }
    }

    /** Viewer path. Returns per-section pretty XML and queries from a single streamer pass. */
    fun readFullManifest(apkPath: String, pkgName: Pkg.Name): ManifestData = withManifestBytes(
        apkPath = apkPath,
        onUnavailable = { reason ->
            ManifestData(
                sections = SectionsResult.Unavailable(reason),
                queries = QueriesOutcome.Unavailable(reason),
            )
        },
    ) { bytes ->
        try {
            val extractor = QueriesExtractor()
            val sectionVisitor = ManifestSectionVisitor(resourceNameResolver.forPackage(pkgName))
            BinaryXmlStreamer().parse(bytes, CompositeVisitor(listOf(sectionVisitor, extractor)))
            ManifestData(
                sections = SectionsResult.Success(sectionVisitor.result()),
                queries = QueriesOutcome.Success(extractor.result()),
            )
        } catch (oom: OutOfMemoryError) {
            log(TAG, WARN) { "OOM during manifest parse: $oom" }
            ManifestData(
                sections = SectionsResult.Unavailable(UnavailableReason.LOW_MEMORY),
                queries = QueriesOutcome.Unavailable(UnavailableReason.LOW_MEMORY),
            )
        } catch (e: BinaryXmlException) {
            // Structural rejection from the streaming parser is stable across attempts —
            // classify as MALFORMED_APK rather than as a transient Error/Failure so callers
            // surface a cleaner "cannot read manifest" message and the hint scanner caches
            // the negative outcome instead of re-parsing the same broken APK on every run.
            log(TAG, WARN) { "Binary XML parse failed (treating as malformed): $e" }
            ManifestData(
                sections = SectionsResult.Unavailable(UnavailableReason.MALFORMED_APK),
                queries = QueriesOutcome.Unavailable(UnavailableReason.MALFORMED_APK),
            )
        } catch (e: Exception) {
            log(TAG, WARN) { "Unexpected parse error: $e" }
            ManifestData(sections = SectionsResult.Error(e), queries = QueriesOutcome.Failure(e))
        }
    }

    /**
     * Shared preflight + bytes-read preamble for [readQueries] and [readFullManifest].
     * Classifies file/zip-level failures into [UnavailableReason] via [onUnavailable];
     * hands valid manifest bytes to [body] for parser-time handling.
     *
     * The [SecurityException] arm closes the read-time race window: [openApk]'s preflight
     * classifies a SecurityException at `ZipFile()` construction, but the APK could become
     * unreadable between the preflight check and the actual byte read.
     */
    private inline fun <T> withManifestBytes(
        apkPath: String,
        onUnavailable: (UnavailableReason) -> T,
        body: (ByteArray) -> T,
    ): T {
        val preflight = openApk(apkPath)
        if (preflight is Preflight.Failed) return onUnavailable(preflight.reason)
        val ok = preflight as Preflight.Ok
        return ok.zip.use { zip ->
            val bytes = try {
                readManifestBytes(zip, ok.manifestEntry)
            } catch (e: LowMemoryException) {
                log(TAG, WARN) { "OOM reading manifest bytes: ${e.cause}" }
                return@use onUnavailable(UnavailableReason.LOW_MEMORY)
            } catch (e: MalformedApkException) {
                log(TAG, WARN) { "Malformed manifest entry: $e" }
                return@use onUnavailable(UnavailableReason.MALFORMED_APK)
            } catch (e: SecurityException) {
                log(TAG, WARN) { "SecurityException reading manifest: $e" }
                return@use onUnavailable(UnavailableReason.APK_NOT_READABLE)
            }
            body(bytes)
        }
    }

    private fun openApk(apkPath: String): Preflight {
        val apkFile = File(apkPath)
        if (!apkFile.exists()) return Preflight.Failed(UnavailableReason.APK_NOT_FOUND)
        if (!apkFile.canRead()) return Preflight.Failed(UnavailableReason.APK_NOT_READABLE)

        val zip = try {
            ZipFile(apkFile)
        } catch (_: FileNotFoundException) {
            return Preflight.Failed(UnavailableReason.APK_NOT_FOUND)
        } catch (_: SecurityException) {
            return Preflight.Failed(UnavailableReason.APK_NOT_READABLE)
        } catch (e: ZipException) {
            log(TAG, WARN) { "Zip open failed: $e" }
            return Preflight.Failed(UnavailableReason.MALFORMED_APK)
        } catch (e: IOException) {
            log(TAG, WARN) { "IO error opening APK: $e" }
            return Preflight.Failed(UnavailableReason.MALFORMED_APK)
        }

        val manifestEntry = zip.getEntry(MANIFEST_ENTRY)
        if (manifestEntry == null || manifestEntry.size < 0L) {
            zip.close()
            return Preflight.Failed(UnavailableReason.MALFORMED_APK)
        }

        return Preflight.Ok(zip, manifestEntry)
    }

    @VisibleForTesting
    internal fun readManifestBytes(zip: ZipFile, entry: ZipEntry): ByteArray {
        if (entry.isDirectory) throw MalformedApkException("AndroidManifest.xml is a directory entry")
        val size = entry.size
        if (size <= 0 || size > Int.MAX_VALUE) throw MalformedApkException("bad manifest size: $size")
        val buf = try {
            ByteArray(size.toInt())
        } catch (oom: OutOfMemoryError) {
            throw LowMemoryException(oom)
        }
        try {
            zip.getInputStream(entry).use { input ->
                var read = 0
                while (read < buf.size) {
                    val n = input.read(buf, read, buf.size - read)
                    if (n < 0) throw MalformedApkException("short read at $read/${buf.size}")
                    read += n
                }
            }
        } catch (e: ZipException) {
            // Corrupt DEFLATE stream or CRC mismatch. Surfacing as MalformedApkException lets the
            // existing try/catch classify the outcome; we never want a raw IOException to escape
            // the reader and crash the caller (the hint scanner runs over hundreds of APKs).
            throw MalformedApkException("zip read failed: ${e.message}")
        } catch (e: IOException) {
            throw MalformedApkException("io read failed: ${e.message}")
        }
        return buf
    }

    private sealed class Preflight {
        data class Ok(val zip: ZipFile, val manifestEntry: ZipEntry) : Preflight()
        data class Failed(val reason: UnavailableReason) : Preflight()
    }

    internal class MalformedApkException(message: String) : RuntimeException(message)
    internal class LowMemoryException(cause: Throwable) : RuntimeException(cause)

    companion object {
        private const val MANIFEST_ENTRY = "AndroidManifest.xml"
        private val TAG = logTag("Apps", "Manifest", "Reader")
    }
}

