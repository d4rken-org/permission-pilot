package eu.darken.myperm.apps.ui.manifest

import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.apps.core.manifest.ManifestRepo
import eu.darken.myperm.apps.core.manifest.RawXmlResult
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.uix.ViewModel4
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ManifestViewerViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val manifestRepo: ManifestRepo,
) : ViewModel4(dispatcherProvider = dispatcherProvider) {

    private var pkgName: String = ""

    private val searchQuery = MutableStateFlow("")

    fun init(route: Nav.Details.AppManifest) {
        pkgName = route.pkgName
    }

    data class MatchRange(val start: Int, val end: Int)

    data class State(
        val rawXml: String? = null,
        val isLoading: Boolean = true,
        val error: String? = null,
        val searchQuery: String = "",
        val matchCount: Int = 0,
        val matchRanges: List<MatchRange> = emptyList(),
    )

    val state: StateFlow<State> by lazy {
        val xmlFlow = flow {
            val data = manifestRepo.getManifest(pkgName)
            when (val result = data.rawXml) {
                is RawXmlResult.Success -> emit(result.xml)
                is RawXmlResult.Unavailable -> throw IllegalStateException(result.reason.name)
                is RawXmlResult.Error -> throw result.error
            }
        }.catch { e ->
            log(TAG) { "Failed to load manifest: $e" }
        }

        @OptIn(FlowPreview::class)
        val debouncedSearch = searchQuery.debounce(300)

        combine(xmlFlow, debouncedSearch) { xml, query ->
            val matches = if (query.length >= 2) {
                findMatches(xml, query)
            } else {
                emptyList()
            }
            State(
                rawXml = xml,
                isLoading = false,
                searchQuery = query,
                matchCount = matches.size,
                matchRanges = matches,
            )
        }
            .catch { e -> emit(State(isLoading = false, error = e.message)) }
            .stateIn(vmScope, SharingStarted.WhileSubscribed(5000), State())
    }

    fun onSearchChanged(query: String) {
        searchQuery.value = query
    }

    companion object {
        private val TAG = logTag("Apps", "Manifest", "Viewer", "VM")

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
