package eu.darken.myperm.debug.manifest

import android.content.Context
import eu.darken.myperm.apps.core.manifest.ApkManifestReader
import eu.darken.myperm.apps.core.manifest.QueriesInfo
import eu.darken.myperm.apps.core.manifest.QueriesReadResult
import eu.darken.myperm.common.debug.logging.Logging.Priority.INFO
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import net.dongliu.apk.parser.ApkFile
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader

/**
 * Debug-only scaffolding. Parses every installed app's manifest with both the old
 * `net.dongliu:apk-parser`-based path and the new streaming parser, diffs the `QueriesInfo`
 * projection, and logs divergences.
 *
 * **Not** wired into any production code path. Triggered via the broadcast receiver in the
 * debug manifest: `adb shell am broadcast -a eu.darken.myperm.DEBUG_PARSER_PARITY`.
 *
 * Remove this file and the broadcast receiver after on-device validation.
 */
class ParserParityChecker(
    private val context: Context,
    private val newReader: ApkManifestReader,
) {
    data class Summary(
        val totalChecked: Int,
        val matches: Int,
        val divergences: Int,
        val oldOnly: Int,
        val newOnly: Int,
        val oldFailed: Int,
        val newFailed: Int,
    )

    fun runAll(): Summary {
        log(TAG, INFO) { "=== ParserParityChecker START ===" }
        val pm = context.packageManager
        val apps = pm.getInstalledPackages(0)

        var matches = 0
        var divergences = 0
        var oldOnly = 0
        var newOnly = 0
        var oldFailed = 0
        var newFailed = 0

        for (pi in apps) {
            val pkgName = pi.packageName ?: continue
            val apkPath = pi.applicationInfo?.sourceDir ?: continue
            if (!File(apkPath).exists()) continue

            val oldQueries = try {
                parseWithOldLibrary(apkPath)
            } catch (e: Throwable) {
                log(TAG, WARN) { "Old parser failed for $pkgName: $e" }
                oldFailed++
                null
            }

            val newResult = try {
                newReader.readQueries(apkPath)
            } catch (e: Throwable) {
                log(TAG, WARN) { "New parser threw for $pkgName: $e" }
                newFailed++
                null
            }
            val newQueries = when (newResult) {
                is QueriesReadResult.Success -> newResult.info
                is QueriesReadResult.Unavailable -> {
                    log(TAG) { "New parser unavailable for $pkgName: ${newResult.reason}" }
                    null
                }
                is QueriesReadResult.Error -> {
                    log(TAG, WARN) { "New parser error for $pkgName: ${newResult.error}" }
                    newFailed++
                    null
                }
                null -> null
            }

            when {
                oldQueries != null && newQueries != null -> {
                    if (queriesMatch(oldQueries, newQueries)) {
                        matches++
                    } else {
                        divergences++
                        logDivergence(pkgName, oldQueries, newQueries)
                    }
                }
                oldQueries != null && newQueries == null -> oldOnly++
                oldQueries == null && newQueries != null -> newOnly++
            }
        }

        val summary = Summary(
            totalChecked = apps.size,
            matches = matches,
            divergences = divergences,
            oldOnly = oldOnly,
            newOnly = newOnly,
            oldFailed = oldFailed,
            newFailed = newFailed,
        )
        log(TAG, INFO) {
            "=== ParserParityChecker DONE === total=${summary.totalChecked} matches=${summary.matches} " +
                "divergences=${summary.divergences} oldOnly=${summary.oldOnly} newOnly=${summary.newOnly} " +
                "oldFailed=${summary.oldFailed} newFailed=${summary.newFailed}"
        }
        return summary
    }

    private fun queriesMatch(a: QueriesInfo, b: QueriesInfo): Boolean {
        return a.packageQueries.toSet() == b.packageQueries.toSet() &&
            a.providerQueries.toSet() == b.providerQueries.toSet() &&
            intentsMatch(a.intentQueries, b.intentQueries)
    }

    private fun intentsMatch(a: List<QueriesInfo.IntentQuery>, b: List<QueriesInfo.IntentQuery>): Boolean {
        if (a.size != b.size) return false
        val aKeys = a.map { intentKey(it) }.toSet()
        val bKeys = b.map { intentKey(it) }.toSet()
        return aKeys == bKeys
    }

    private fun intentKey(q: QueriesInfo.IntentQuery): String =
        "actions=${q.actions.sorted()};data=${q.dataSpecs.sorted()};categories=${q.categories.sorted()}"

    private fun logDivergence(pkgName: String, old: QueriesInfo, new: QueriesInfo) {
        log(TAG, WARN) { "DIVERGENCE for $pkgName" }
        val oldPkgs = old.packageQueries.toSet()
        val newPkgs = new.packageQueries.toSet()
        if (oldPkgs != newPkgs) {
            val oldOnly = oldPkgs - newPkgs
            val newOnly = newPkgs - oldPkgs
            if (oldOnly.isNotEmpty()) log(TAG, WARN) { "  packages old-only: $oldOnly" }
            if (newOnly.isNotEmpty()) log(TAG, WARN) { "  packages new-only: $newOnly" }
        }
        val oldProv = old.providerQueries.toSet()
        val newProv = new.providerQueries.toSet()
        if (oldProv != newProv) {
            val oldOnly = oldProv - newProv
            val newOnly = newProv - oldProv
            if (oldOnly.isNotEmpty()) log(TAG, WARN) { "  providers old-only: $oldOnly" }
            if (newOnly.isNotEmpty()) log(TAG, WARN) { "  providers new-only: $newOnly" }
        }
        if (old.intentQueries.size != new.intentQueries.size) {
            log(TAG, WARN) { "  intents size: old=${old.intentQueries.size} new=${new.intentQueries.size}" }
        }
        val oldKeys = old.intentQueries.map(::intentKey).toSet()
        val newKeys = new.intentQueries.map(::intentKey).toSet()
        val onlyOld = oldKeys - newKeys
        val onlyNew = newKeys - oldKeys
        for (k in onlyOld) log(TAG, WARN) { "    intent old-only: $k" }
        for (k in onlyNew) log(TAG, WARN) { "    intent new-only: $k" }
    }

    /** Mirrors the pre-migration parsing path in the old ApkManifestReader. */
    private fun parseWithOldLibrary(apkPath: String): QueriesInfo {
        val xml = ApkFile(File(apkPath)).use { it.manifestXml }
        return parseQueriesFromXml(xml)
    }

    private fun parseQueriesFromXml(xml: String): QueriesInfo {
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
        private val TAG = logTag("Debug", "ParserParity")
    }
}
