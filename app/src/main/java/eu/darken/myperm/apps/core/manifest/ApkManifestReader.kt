package eu.darken.myperm.apps.core.manifest

import androidx.annotation.VisibleForTesting
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import net.dongliu.apk.parser.ApkFile
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkManifestReader @Inject constructor() {

    internal data class HeapInfo(val maxHeap: Long, val freeHeap: Long)

    @VisibleForTesting
    internal var heapInfoProvider: () -> HeapInfo = {
        val rt = Runtime.getRuntime()
        HeapInfo(maxHeap = rt.maxMemory(), freeHeap = rt.maxMemory() - (rt.totalMemory() - rt.freeMemory()))
    }

    fun readManifest(apkPath: String): ManifestData {
        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            return ManifestData(
                rawXml = RawXmlResult.Unavailable(UnavailableReason.APK_NOT_FOUND),
                queries = QueriesResult.Error(IllegalStateException("APK not found: $apkPath")),
            )
        }
        if (!apkFile.canRead()) {
            return ManifestData(
                rawXml = RawXmlResult.Unavailable(UnavailableReason.APK_NOT_READABLE),
                queries = QueriesResult.Error(IllegalStateException("APK not readable: $apkPath")),
            )
        }

        // Entry gate: require 20% of max heap free. Reactive — the real protection is the
        // post-preflight byte budget below, which sizes against actual parse cost.
        val entryHeap = heapInfoProvider()
        if (entryHeap.freeHeap < entryHeap.maxHeap * FREE_HEAP_ENTRY_RATIO) {
            log(TAG, WARN) { "Skipping manifest parse for $apkPath: low memory (${entryHeap.freeHeap / 1024}KB free)" }
            return ManifestData(
                rawXml = RawXmlResult.Unavailable(UnavailableReason.LOW_MEMORY),
                queries = QueriesResult.Error(IllegalStateException("Low memory")),
            )
        }

        // Preflight: size the zip entries the parser will read. apk-parser loads both
        // resources.arsc and AndroidManifest.xml via a ByteArrayOutputStream.grow cycle,
        // which peaks around 3× the growing buffer (old + new + JVM overhead).
        val (arscSize, manifestSize) = try {
            ZipFile(apkFile).use { zip ->
                val arscEntry = zip.getEntry("resources.arsc")
                val manifestEntry = zip.getEntry("AndroidManifest.xml")
                val arscSize: Long = arscEntry?.size?.takeIf { it >= 0L } ?: -1L
                val manifestSize: Long = manifestEntry?.size?.takeIf { it >= 0L } ?: -1L
                arscSize to manifestSize
            }
        } catch (e: Exception) {
            log(TAG, WARN) { "Zip preflight failed for $apkPath: $e" }
            return ManifestData(
                rawXml = RawXmlResult.Unavailable(UnavailableReason.MALFORMED_APK),
                queries = QueriesResult.Error(e),
            )
        }

        if (arscSize < 0L || manifestSize < 0L) {
            log(TAG, WARN) { "Missing/unknown zip entry sizes for $apkPath (arsc=$arscSize, manifest=$manifestSize)" }
            return ManifestData(
                rawXml = RawXmlResult.Unavailable(UnavailableReason.MALFORMED_APK),
                queries = QueriesResult.Error(IllegalStateException("Malformed APK: missing entry sizes")),
            )
        }

        val estimatedPeak: Long = arscSize * 3L + manifestSize * 2L + PARSE_SLACK_BYTES
        val postPreflightHeap = heapInfoProvider()
        val budgetCeiling: Long = (postPreflightHeap.maxHeap * BUDGET_MAX_HEAP_RATIO).toLong()
        if (estimatedPeak > budgetCeiling) {
            log(TAG, WARN) {
                "Skipping oversized APK $apkPath: estimatedPeak=${estimatedPeak / 1024}KB > budget=${budgetCeiling / 1024}KB"
            }
            return ManifestData(
                rawXml = RawXmlResult.Unavailable(UnavailableReason.APK_TOO_LARGE),
                queries = QueriesResult.Error(IllegalStateException("APK too large: estimatedPeak=$estimatedPeak")),
            )
        }

        // Re-check free heap against the sized budget — if something consumed heap between
        // the entry gate and here, bail before we trigger the doubling allocation.
        if (postPreflightHeap.freeHeap < estimatedPeak + PARSE_SLACK_BYTES) {
            log(TAG, WARN) {
                "Insufficient heap for manifest parse $apkPath: free=${postPreflightHeap.freeHeap / 1024}KB, need=${(estimatedPeak + PARSE_SLACK_BYTES) / 1024}KB"
            }
            return ManifestData(
                rawXml = RawXmlResult.Unavailable(UnavailableReason.LOW_MEMORY),
                queries = QueriesResult.Error(IllegalStateException("Insufficient heap for parse")),
            )
        }

        val xml = try {
            ApkFile(apkFile).use { it.manifestXml }
        } catch (e: OutOfMemoryError) {
            log(TAG, WARN) { "OOM reading manifest from $apkPath: $e" }
            return ManifestData(
                rawXml = RawXmlResult.Unavailable(UnavailableReason.LOW_MEMORY),
                queries = QueriesResult.Error(IllegalStateException("OOM during manifest parse", e)),
            )
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to read manifest from $apkPath: $e" }
            return ManifestData(
                rawXml = RawXmlResult.Error(e),
                queries = QueriesResult.Error(e),
            )
        }

        val queriesResult = try {
            QueriesResult.Success(parseQueries(xml))
        } catch (e: OutOfMemoryError) {
            log(TAG, WARN) { "OOM parsing queries from $apkPath: $e" }
            QueriesResult.Error(IllegalStateException("OOM during queries parse", e))
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to parse queries from $apkPath: $e" }
            QueriesResult.Error(e)
        }

        return ManifestData(
            rawXml = RawXmlResult.Success(xml),
            queries = queriesResult,
        )
    }

    private fun parseQueries(xml: String): QueriesInfo {
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var inQueries = false
        var inIntent = false
        val packages = mutableListOf<String>()
        val intents = mutableListOf<QueriesInfo.IntentQuery>()
        val providers = mutableListOf<String>()
        var currentActions = mutableListOf<String>()
        var currentData = mutableListOf<String>()
        var currentCategories = mutableListOf<String>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "queries" -> inQueries = true
                    "package" -> if (inQueries && !inIntent) {
                        getAndroidAttr(parser, "name")?.let { packages.add(it) }
                    }
                    "intent" -> if (inQueries) {
                        inIntent = true
                        currentActions = mutableListOf()
                        currentData = mutableListOf()
                        currentCategories = mutableListOf()
                    }
                    "action" -> if (inIntent) {
                        getAndroidAttr(parser, "name")?.let { currentActions.add(it) }
                    }
                    "data" -> if (inIntent) {
                        val specs = buildList {
                            getAndroidAttr(parser, "scheme")?.let { add("scheme=$it") }
                            getAndroidAttr(parser, "host")?.let { add("host=$it") }
                            getAndroidAttr(parser, "mimeType")?.let { add("mimeType=$it") }
                        }
                        if (specs.isNotEmpty()) currentData.add(specs.joinToString(", "))
                    }
                    "category" -> if (inIntent) {
                        getAndroidAttr(parser, "name")?.let { currentCategories.add(it) }
                    }
                    "provider" -> if (inQueries && !inIntent) {
                        getAndroidAttr(parser, "authorities")?.let { providers.add(it) }
                    }
                }

                XmlPullParser.END_TAG -> when (parser.name) {
                    "queries" -> inQueries = false
                    "intent" -> if (inQueries) {
                        intents.add(QueriesInfo.IntentQuery(currentActions, currentData, currentCategories))
                        inIntent = false
                    }
                }
            }
            eventType = parser.next()
        }

        return QueriesInfo(packages, intents, providers)
    }

    private fun getAndroidAttr(parser: XmlPullParser, name: String): String? {
        return parser.getAttributeValue(ANDROID_NS, name)
            ?: parser.getAttributeValue(null, "android:$name")
    }

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        private const val FREE_HEAP_ENTRY_RATIO = 0.20
        private const val BUDGET_MAX_HEAP_RATIO = 0.25
        private const val PARSE_SLACK_BYTES = 2L * 1024 * 1024 // 2 MB safety margin
        private val TAG = logTag("Apps", "Manifest", "Reader")
    }
}
