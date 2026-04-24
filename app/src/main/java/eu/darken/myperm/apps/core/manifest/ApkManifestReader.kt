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
 *   [QueriesExtractor] only. No rawXml is built, no resource resolution.
 * - [readFullManifest] — viewer path; composite visitor produces both rawXml (via
 *   [ManifestTextRenderer]) and [QueriesInfo] in a single parse.
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
    fun readQueries(apkPath: String): QueriesReadResult {
        val preflight = openApk(apkPath)
        if (preflight is Preflight.Failed) return QueriesReadResult.Unavailable(preflight.reason)
        val ok = preflight as Preflight.Ok
        ok.zip.use { zip ->
            val bytes = try {
                readManifestBytes(zip, ok.manifestEntry)
            } catch (e: LowMemoryException) {
                log(TAG, WARN) { "OOM reading manifest bytes: ${e.cause}" }
                return QueriesReadResult.Unavailable(UnavailableReason.LOW_MEMORY)
            } catch (e: MalformedApkException) {
                log(TAG, WARN) { "Malformed manifest entry: $e" }
                return QueriesReadResult.Unavailable(UnavailableReason.MALFORMED_APK)
            }
            return try {
                val extractor = QueriesExtractor()
                BinaryXmlStreamer().parse(bytes, extractor)
                QueriesReadResult.Success(extractor.result())
            } catch (oom: OutOfMemoryError) {
                log(TAG, WARN) { "OOM during manifest parse: $oom" }
                QueriesReadResult.Unavailable(UnavailableReason.LOW_MEMORY)
            } catch (e: BinaryXmlException) {
                log(TAG, WARN) { "Binary XML parse failed: $e" }
                QueriesReadResult.Error(e)
            } catch (e: Exception) {
                log(TAG, WARN) { "Unexpected parse error: $e" }
                QueriesReadResult.Error(e)
            }
        }
    }

    /** Viewer path. Returns rawXml and queries from a single streamer pass. */
    fun readFullManifest(apkPath: String, pkgName: Pkg.Name): ManifestData {
        val preflight = openApk(apkPath)
        if (preflight is Preflight.Failed) return ManifestData(
            rawXml = RawXmlResult.Unavailable(preflight.reason),
            queries = QueriesResult.Error(IllegalStateException(preflight.reason.name)),
        )
        val ok = preflight as Preflight.Ok
        ok.zip.use { zip ->
            val bytes = try {
                readManifestBytes(zip, ok.manifestEntry)
            } catch (e: LowMemoryException) {
                log(TAG, WARN) { "OOM reading manifest bytes: ${e.cause}" }
                return ManifestData(
                    rawXml = RawXmlResult.Unavailable(UnavailableReason.LOW_MEMORY),
                    queries = QueriesResult.Error(IllegalStateException("OOM reading bytes", e)),
                )
            } catch (e: MalformedApkException) {
                log(TAG, WARN) { "Malformed manifest entry: $e" }
                return ManifestData(
                    rawXml = RawXmlResult.Unavailable(UnavailableReason.MALFORMED_APK),
                    queries = QueriesResult.Error(e),
                )
            }
            return try {
                val extractor = QueriesExtractor()
                val renderer = ManifestTextRenderer(resourceNameResolver.forPackage(pkgName))
                BinaryXmlStreamer().parse(bytes, CompositeVisitor(listOf(renderer, extractor)))
                ManifestData(
                    rawXml = RawXmlResult.Success(renderer.result()),
                    queries = QueriesResult.Success(extractor.result()),
                )
            } catch (oom: OutOfMemoryError) {
                log(TAG, WARN) { "OOM during manifest parse: $oom" }
                ManifestData(
                    rawXml = RawXmlResult.Unavailable(UnavailableReason.LOW_MEMORY),
                    queries = QueriesResult.Error(IllegalStateException("OOM during parse", oom)),
                )
            } catch (e: BinaryXmlException) {
                log(TAG, WARN) { "Binary XML parse failed: $e" }
                ManifestData(rawXml = RawXmlResult.Error(e), queries = QueriesResult.Error(e))
            } catch (e: Exception) {
                log(TAG, WARN) { "Unexpected parse error: $e" }
                ManifestData(rawXml = RawXmlResult.Error(e), queries = QueriesResult.Error(e))
            }
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
        zip.getInputStream(entry).use { input ->
            var read = 0
            while (read < buf.size) {
                val n = input.read(buf, read, buf.size - read)
                if (n < 0) throw MalformedApkException("short read at $read/${buf.size}")
                read += n
            }
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

sealed class QueriesReadResult {
    data class Success(val info: QueriesInfo) : QueriesReadResult()
    data class Unavailable(val reason: UnavailableReason) : QueriesReadResult()
    data class Error(val error: Throwable) : QueriesReadResult()
}
