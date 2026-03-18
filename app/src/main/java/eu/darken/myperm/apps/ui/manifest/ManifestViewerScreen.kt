package eu.darken.myperm.apps.ui.manifest

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.LoadingContent
import eu.darken.myperm.common.compose.SearchTextField
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavigationEventHandler

@Composable
fun ManifestViewerScreenHost(
    route: Nav.Details.AppManifest,
    vm: ManifestViewerViewModel = hiltViewModel(),
) {
    LaunchedEffect(route) { vm.init(route) }

    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val state by vm.state.collectAsState()

    ManifestViewerScreen(
        state = state,
        onBack = { vm.navUp() },
        onSearchChanged = { vm.onSearchChanged(it) },
    )
}

@Composable
fun ManifestViewerScreen(
    state: ManifestViewerViewModel.State,
    onBack: () -> Unit,
    onSearchChanged: (String) -> Unit,
) {
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var localQuery by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.apps_manifest_viewer_label)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (showSearch) {
                SearchTextField(
                    query = localQuery,
                    onQueryChanged = {
                        localQuery = it
                        onSearchChanged(it)
                    },
                    placeholder = stringResource(R.string.apps_manifest_viewer_search_hint),
                )
                if (localQuery.length >= 2 && state.matchCount > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.apps_manifest_viewer_matches,
                            state.matchCount,
                            state.matchCount,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    )
                }
            }

            when {
                state.isLoading -> {
                    LoadingContent(modifier = Modifier.fillMaxSize())
                }
                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                state.rawXml != null -> {
                    val annotatedXml = highlightMatches(state.rawXml, state.matchRanges)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = annotatedXml,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun highlightMatches(
    xml: String,
    matchRanges: List<ManifestViewerViewModel.MatchRange>,
): AnnotatedString {
    if (matchRanges.isEmpty()) return AnnotatedString(xml)

    val highlightColor = MaterialTheme.colorScheme.tertiaryContainer
    val highlightTextColor = MaterialTheme.colorScheme.onTertiaryContainer

    return buildAnnotatedString {
        append(xml)
        matchRanges.forEach { range ->
            addStyle(
                SpanStyle(background = highlightColor, color = highlightTextColor),
                start = range.start,
                end = range.end,
            )
        }
    }
}
