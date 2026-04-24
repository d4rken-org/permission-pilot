package eu.darken.myperm.apps.core.manifest

import eu.darken.myperm.apps.core.manifest.binaryxml.BinaryXmlAttribute
import eu.darken.myperm.apps.core.manifest.binaryxml.BinaryXmlVisitor
import eu.darken.myperm.apps.core.manifest.binaryxml.TypedValue

/**
 * Visitor that extracts the `<queries>` projection used by the hint scanner.
 *
 * Depth-aware: only direct children of top-level `<queries>` contribute to the package/intent/provider
 * lists. Nested `<package>` inside `<intent>` is intentionally ignored to match the current semantics.
 * Multiple `<queries>` blocks at depth 1 are merged — the extractor stays active for the whole parse.
 */
class QueriesExtractor : BinaryXmlVisitor {

    private val packages = mutableListOf<String>()
    private val intents = mutableListOf<QueriesInfo.IntentQuery>()
    private val providers = mutableListOf<String>()

    private var depth = 0
    private var queriesDepth = -1
    private var intentDepth = -1
    private var currentActions: MutableList<String> = mutableListOf()
    private var currentData: MutableList<String> = mutableListOf()
    private var currentCategories: MutableList<String> = mutableListOf()

    fun result(): QueriesInfo = QueriesInfo(
        packageQueries = packages.toList(),
        intentQueries = intents.toList(),
        providerQueries = providers.toList(),
    )

    override fun onStartElement(
        namespace: String?,
        prefix: String?,
        name: String,
        attributes: List<BinaryXmlAttribute>,
        lineNumber: Int,
    ) {
        depth++
        when {
            name == QUERIES_TAG && queriesDepth == -1 && depth == 2 -> {
                queriesDepth = depth
            }

            queriesDepth != -1 && depth == queriesDepth + 1 -> when (name) {
                PACKAGE_TAG -> androidAttrString(attributes, ATTR_NAME)?.let { packages.add(it) }
                INTENT_TAG -> {
                    intentDepth = depth
                    currentActions = mutableListOf()
                    currentData = mutableListOf()
                    currentCategories = mutableListOf()
                }
                PROVIDER_TAG -> androidAttrString(attributes, ATTR_AUTHORITIES)?.let { providers.add(it) }
            }

            intentDepth != -1 && depth == intentDepth + 1 -> when (name) {
                ACTION_TAG -> androidAttrString(attributes, ATTR_NAME)?.let { currentActions.add(it) }
                CATEGORY_TAG -> androidAttrString(attributes, ATTR_NAME)?.let { currentCategories.add(it) }
                DATA_TAG -> {
                    val specs = buildList {
                        androidAttrString(attributes, ATTR_SCHEME)?.let { add("scheme=$it") }
                        androidAttrString(attributes, ATTR_HOST)?.let { add("host=$it") }
                        androidAttrString(attributes, ATTR_MIMETYPE)?.let { add("mimeType=$it") }
                    }
                    if (specs.isNotEmpty()) currentData.add(specs.joinToString(", "))
                }
            }
        }
    }

    override fun onEndElement(namespace: String?, prefix: String?, name: String, lineNumber: Int) {
        if (intentDepth != -1 && depth == intentDepth && name == INTENT_TAG) {
            intents.add(
                QueriesInfo.IntentQuery(
                    actions = currentActions.toList(),
                    dataSpecs = currentData.toList(),
                    categories = currentCategories.toList(),
                )
            )
            intentDepth = -1
        } else if (queriesDepth != -1 && depth == queriesDepth && name == QUERIES_TAG) {
            queriesDepth = -1
            // If a malformed manifest opens <queries><intent> without closing <intent>, clear
            // intent state so it doesn't leak into elements after </queries>.
            intentDepth = -1
        }
        depth--
    }

    private fun androidAttrString(attrs: List<BinaryXmlAttribute>, name: String): String? {
        val attr = attrs.firstOrNull { it.name == name && (it.namespace == ANDROID_NS || it.namespace == null) }
            ?: return null
        return when (val v = attr.typedValue) {
            is TypedValue.Str -> v.value
            else -> null
        }
    }

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        private const val QUERIES_TAG = "queries"
        private const val PACKAGE_TAG = "package"
        private const val INTENT_TAG = "intent"
        private const val PROVIDER_TAG = "provider"
        private const val ACTION_TAG = "action"
        private const val CATEGORY_TAG = "category"
        private const val DATA_TAG = "data"
        private const val ATTR_NAME = "name"
        private const val ATTR_AUTHORITIES = "authorities"
        private const val ATTR_SCHEME = "scheme"
        private const val ATTR_HOST = "host"
        private const val ATTR_MIMETYPE = "mimeType"
    }
}
