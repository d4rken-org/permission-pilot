package eu.darken.myperm.apps.core.queries

import eu.darken.myperm.apps.core.features.QueriesInfo
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
class ApkParserManifestParser @Inject constructor() : ManifestParser {

    override fun parseQueries(apkPath: String): ManifestParser.QueriesResult {
        val apkFile = File(apkPath)
        if (!apkFile.exists() || !apkFile.canRead()) {
            return ManifestParser.QueriesResult.Unavailable("APK not accessible: $apkPath")
        }

        return try {
            val manifestXml = ApkFile(apkFile).use { it.manifestXml }
            val queriesInfo = parseQueriesFromXml(manifestXml)
            ManifestParser.QueriesResult.Success(queriesInfo)
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to parse manifest from $apkPath: $e" }
            ManifestParser.QueriesResult.ParseError(e)
        }
    }

    private fun parseQueriesFromXml(xml: String): QueriesInfo {
        val packageQueries = mutableListOf<String>()
        val intentQueries = mutableListOf<QueriesInfo.IntentQuery>()
        val providerQueries = mutableListOf<String>()

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var insideQueries = false
        var insideIntent = false
        var currentActions = mutableListOf<String>()
        var currentDataSpecs = mutableListOf<String>()
        var currentCategories = mutableListOf<String>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "queries" -> insideQueries = true
                        "package" -> if (insideQueries) {
                            val name = parser.getAndroidAttr("name")
                            if (name != null) packageQueries.add(name)
                        }
                        "provider" -> if (insideQueries) {
                            val authorities = parser.getAndroidAttr("authorities")
                            if (authorities != null) providerQueries.add(authorities)
                        }
                        "intent" -> if (insideQueries) {
                            insideIntent = true
                            currentActions = mutableListOf()
                            currentDataSpecs = mutableListOf()
                            currentCategories = mutableListOf()
                        }
                        "action" -> if (insideQueries && insideIntent) {
                            parser.getAndroidAttr("name")?.let { currentActions.add(it) }
                        }
                        "data" -> if (insideQueries && insideIntent) {
                            currentDataSpecs.add(buildDataString(parser))
                        }
                        "category" -> if (insideQueries && insideIntent) {
                            parser.getAndroidAttr("name")?.let { currentCategories.add(it) }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "queries" -> insideQueries = false
                        "intent" -> if (insideQueries && insideIntent) {
                            intentQueries.add(
                                QueriesInfo.IntentQuery(
                                    actions = currentActions,
                                    dataSpecs = currentDataSpecs,
                                    categories = currentCategories,
                                )
                            )
                            insideIntent = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return QueriesInfo(
            packageQueries = packageQueries,
            intentQueries = intentQueries,
            providerQueries = providerQueries,
        )
    }

    private fun XmlPullParser.getAndroidAttr(name: String): String? {
        return getAttributeValue(ANDROID_NS, name)
            ?: getAttributeValue(null, "android:$name")
    }

    private fun buildDataString(parser: XmlPullParser): String {
        val scheme = parser.getAndroidAttr("scheme")
        val host = parser.getAndroidAttr("host")
        val mimeType = parser.getAndroidAttr("mimeType")

        return buildString {
            if (scheme != null) {
                append(scheme)
                if (host != null) append("://$host")
            }
            if (mimeType != null) {
                if (isNotEmpty()) append(" ")
                append("($mimeType)")
            }
            if (isEmpty()) append("*")
        }
    }

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        private val TAG = logTag("Apps", "Queries", "ManifestParser")
    }
}
