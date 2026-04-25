package eu.darken.myperm.apps.ui.manifest

import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.apps.core.manifest.ManifestHintScanner
import eu.darken.myperm.apps.core.manifest.ManifestRepo
import eu.darken.myperm.apps.core.manifest.ManifestSection
import eu.darken.myperm.apps.core.manifest.QueriesOutcome
import eu.darken.myperm.apps.core.manifest.SectionType
import eu.darken.myperm.apps.core.manifest.SectionsResult
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.uix.ViewModel4
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class ManifestViewerViewModel @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val manifestRepo: ManifestRepo,
    private val hintScanner: ManifestHintScanner,
) : ViewModel4(dispatcherProvider = dispatcherProvider) {

    private var appLabel: String? = null
    private var pkgName: Pkg.Name = Pkg.Name("")

    private val _sections = MutableStateFlow<List<ManifestSection>>(emptyList())
    private val _manualExpanded = MutableStateFlow<Set<SectionType>>(emptySet())
    private val _manualCollapsed = MutableStateFlow<Set<SectionType>>(emptySet())
    private val _searchQuery = MutableStateFlow("")
    private val _currentMatchIdx = MutableStateFlow(-1)
    private val _loadError = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)

    private val scrollSerial = AtomicInteger(0)

    data class MatchRange(val start: Int, val endExclusive: Int)

    data class State(
        val appLabel: String? = null,
        val isLoading: Boolean = true,
        val error: String? = null,
        val sections: List<SectionUiModel> = emptyList(),
        val searchQuery: String = "",
        val totalMatchCount: Int = 0,
        val currentMatchIndex: Int = -1,
        val scrollToItem: ScrollTarget? = null,
    )

    data class SectionUiModel(
        val type: SectionType,
        val elementCount: Int,
        val isFlagged: Boolean,
        val isExpanded: Boolean,
        val prettyXml: String,
        val matchRanges: List<MatchRange>,
        val activeMatchRange: MatchRange?,
    )

    data class ScrollTarget(val listItemIndex: Int, val serial: Int)

    private data class GlobalMatch(
        val sectionType: SectionType,
        val rangeIndex: Int,
    )

    private data class LoadState(
        val sections: List<ManifestSection> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null,
    )

    private val _loadState = MutableStateFlow(LoadState())

    fun init(route: Nav.Details.AppManifest) {
        if (pkgName.value == route.pkgName) return
        pkgName = Pkg.Name(route.pkgName)
        appLabel = route.appLabel
        loadManifest()
    }

    private fun loadManifest() = vmScope.launch {
        _loadState.value = LoadState(isLoading = true)
        try {
            val data = withContext(dispatcherProvider.IO) { manifestRepo.getManifest(pkgName) }
            val rawSections = when (val result = data.sections) {
                is SectionsResult.Success -> result.sections
                is SectionsResult.Unavailable -> throw IllegalStateException(result.reason.name)
                is SectionsResult.Error -> throw result.error
            }

            val flags = when (val q = data.queries) {
                is QueriesOutcome.Success -> hintScanner.evaluate(q.info)
                is QueriesOutcome.Failure -> {
                    log(TAG, WARN) { "Queries parsing failed, flags defaulted: ${q.error}" }
                    defaultHintFlags()
                }
                is QueriesOutcome.Unavailable -> {
                    // Unreachable in the viewer flow — sections.Unavailable is thrown above before
                    // reaching this branch — but keep an exhaustive arm so the when stays total.
                    log(TAG, WARN) { "Queries unavailable, flags defaulted: ${q.reason}" }
                    defaultHintFlags()
                }
            }

            // isFlagged is recomputed from the live queries projection on every load — never
            // taken from the cache, so threshold tuning takes effect immediately.
            val sections = rawSections.map { section ->
                section.copy(isFlagged = isSectionFlagged(section.type, flags))
            }
            _manualExpanded.value = sections.filter { it.isFlagged }.map { it.type }.toSet()
            _loadState.value = LoadState(sections = sections, isLoading = false)
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to load manifest: $e" }
            _loadState.value = LoadState(isLoading = false, error = e.message ?: "Unknown error")
        }
    }

    private fun isSectionFlagged(type: SectionType, flags: ManifestHintScanner.Flags): Boolean = when (type) {
        SectionType.QUERIES -> flags.hasActionMainQuery || flags.packageQueryCount > ManifestHintScanner.EXCESSIVE_THRESHOLD
        else -> false
    }

    @OptIn(FlowPreview::class)
    val state: StateFlow<State> by lazy {
        val debouncedSearch = _searchQuery.debounce(300)

        combine(
            _loadState,
            _manualExpanded,
            _manualCollapsed,
            debouncedSearch,
            _currentMatchIdx,
        ) { loadState, manualExpanded, manualCollapsed, query, requestedMatchIdx ->
            if (loadState.isLoading || loadState.error != null) {
                return@combine State(
                    appLabel = appLabel,
                    isLoading = loadState.isLoading,
                    error = loadState.error,
                )
            }

            val sections = loadState.sections

            // Compute per-section matches
            val sectionMatches = mutableMapOf<SectionType, List<MatchRange>>()
            val allMatches = mutableListOf<GlobalMatch>()

            if (query.length >= MIN_SEARCH_LENGTH) {
                for (section in sections) {
                    val matches = findMatches(section.prettyXml, query)
                    if (matches.isNotEmpty()) {
                        sectionMatches[section.type] = matches
                        matches.forEachIndexed { index, _ ->
                            allMatches.add(GlobalMatch(section.type, index))
                        }
                    }
                }
            }

            val totalMatchCount = allMatches.size
            val clampedMatchIdx = when {
                totalMatchCount == 0 -> -1
                requestedMatchIdx < 0 -> 0
                requestedMatchIdx >= totalMatchCount -> totalMatchCount - 1
                else -> requestedMatchIdx
            }

            val activeMatch = if (clampedMatchIdx >= 0) allMatches[clampedMatchIdx] else null

            // Auto-expand: sections with matches, plus the active match section always
            val autoExpanded = sectionMatches.keys
            val activeMatchSection = activeMatch?.sectionType?.let { setOf(it) } ?: emptySet()
            val expandedSet = (manualExpanded + autoExpanded + activeMatchSection) - manualCollapsed + activeMatchSection

            // Build section UI models and compute scroll target
            var scrollTarget: ScrollTarget? = null
            var listItemIndex = 0
            val sectionUiModels = sections.map { section ->
                val matches = sectionMatches[section.type] ?: emptyList()
                val isExpanded = section.type in expandedSet
                val activeRange = if (activeMatch?.sectionType == section.type) {
                    matches.getOrNull(activeMatch.rangeIndex)
                } else {
                    null
                }

                if (activeMatch?.sectionType == section.type && scrollTarget == null) {
                    scrollTarget = if (isExpanded) {
                        ScrollTarget(listItemIndex + 1, scrollSerial.getAndIncrement())
                    } else {
                        ScrollTarget(listItemIndex, scrollSerial.getAndIncrement())
                    }
                }

                val uiModel = SectionUiModel(
                    type = section.type,
                    elementCount = section.elementCount,
                    isFlagged = section.isFlagged,
                    isExpanded = isExpanded,
                    prettyXml = section.prettyXml,
                    matchRanges = matches,
                    activeMatchRange = activeRange,
                )

                listItemIndex++ // header
                if (isExpanded) listItemIndex++ // content

                uiModel
            }

            State(
                appLabel = appLabel,
                isLoading = false,
                sections = sectionUiModels,
                searchQuery = query,
                totalMatchCount = totalMatchCount,
                currentMatchIndex = clampedMatchIdx,
                scrollToItem = scrollTarget,
            )
        }
            .stateIn(vmScope, SharingStarted.WhileSubscribed(5000), State(appLabel = appLabel))
    }

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
        // Reset manual collapse when query changes so auto-expand can work
        _manualCollapsed.value = emptySet()
    }

    fun onNextMatch() {
        val total = state.value.totalMatchCount
        if (total <= 0) return
        // Step from the clamped value the UI is showing, not the raw flow's value. With the
        // initial -1 in `_currentMatchIdx`, the combine clamps to 0 for display; stepping the raw
        // value would land on 0 again, so the first tap would appear to no-op.
        val current = state.value.currentMatchIndex.coerceAtLeast(0)
        _currentMatchIdx.value = (current + 1) % total
    }

    fun onPrevMatch() {
        val total = state.value.totalMatchCount
        if (total <= 0) return
        val current = state.value.currentMatchIndex.coerceAtLeast(0)
        _currentMatchIdx.value = (current - 1 + total) % total
    }

    fun onToggleSection(type: SectionType) {
        val isCurrentlyExpanded = state.value.sections.find { it.type == type }?.isExpanded ?: return
        if (isCurrentlyExpanded) {
            _manualExpanded.update { it - type }
            _manualCollapsed.update { it + type }
        } else {
            _manualExpanded.update { it + type }
            _manualCollapsed.update { it - type }
        }
    }

    private fun defaultHintFlags() = ManifestHintScanner.Flags(
        hasActionMainQuery = false,
        packageQueryCount = 0,
        intentQueryCount = 0,
        providerQueryCount = 0,
    )

    companion object {
        private val TAG = logTag("Apps", "Manifest", "Viewer", "VM")
        const val MIN_SEARCH_LENGTH = 3

        fun findMatches(xml: String, query: String): List<MatchRange> {
            if (query.isEmpty()) return emptyList()
            val lowerXml = xml.lowercase()
            val lowerQuery = query.lowercase()
            val matches = mutableListOf<MatchRange>()
            var start = 0
            while (true) {
                val index = lowerXml.indexOf(lowerQuery, start)
                if (index < 0) break
                matches.add(MatchRange(index, index + query.length))
                start = index + 1
            }
            return matches
        }
    }
}
