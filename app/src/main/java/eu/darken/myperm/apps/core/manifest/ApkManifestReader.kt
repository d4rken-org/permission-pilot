package eu.darken.myperm.apps.core.manifest

import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import net.dongliu.apk.parser.ApkFile
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkManifestReader @Inject constructor() {

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

        val xml = try {
            ApkFile(apkFile).use { it.manifestXml }
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to read manifest from $apkPath: $e" }
            return ManifestData(
                rawXml = RawXmlResult.Error(e),
                queries = QueriesResult.Error(e),
            )
        }

        val queriesResult = try {
            QueriesResult.Success(parseQueries(xml))
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
        private val TAG = logTag("Apps", "Manifest", "Reader")
    }
}
