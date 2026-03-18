package eu.darken.myperm.apps.ui.manifest

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.manifest.SectionType
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
        onNextMatch = { vm.onNextMatch() },
        onPrevMatch = { vm.onPrevMatch() },
        onToggleSection = { vm.onToggleSection(it) },
    )
}

@Composable
fun ManifestViewerScreen(
    state: ManifestViewerViewModel.State,
    onBack: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
    onToggleSection: (SectionType) -> Unit,
) {
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var localQuery by rememberSaveable { mutableStateOf("") }

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.scrollToItem) {
        state.scrollToItem?.let { listState.animateScrollToItem(it.listItemIndex) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.apps_manifest_viewer_label))
                        state.appLabel?.let { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) {
                            localQuery = ""
                            onSearchChanged("")
                        }
                    }) {
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
                    modifier = Modifier.focusRequester(focusRequester),
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                SearchNavBar(
                    totalMatchCount = state.totalMatchCount,
                    currentMatchIndex = state.currentMatchIndex,
                    onNextMatch = {
                        keyboardController?.hide()
                        onNextMatch()
                    },
                    onPrevMatch = {
                        keyboardController?.hide()
                        onPrevMatch()
                    },
                )
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

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        state.sections.forEach { section ->
                            item(key = "${section.type}_header") {
                                SectionHeader(
                                    section = section,
                                    onToggle = { onToggleSection(section.type) },
                                )
                            }
                            if (section.isExpanded) {
                                item(key = "${section.type}_content") {
                                    SectionContent(section = section)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchNavBar(
    totalMatchCount: Int,
    currentMatchIndex: Int,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (totalMatchCount > 0) {
            Text(
                text = stringResource(
                    R.string.apps_manifest_search_counter,
                    currentMatchIndex + 1,
                    totalMatchCount,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Row {
            IconButton(
                onClick = onPrevMatch,
                enabled = totalMatchCount > 0,
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = onNextMatch,
                enabled = totalMatchCount > 0,
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    section: ManifestViewerViewModel.SectionUiModel,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (section.isExpanded) 0f else -90f,
        label = "chevron",
    )
    val color = if (section.isFlagged) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .rotate(rotation),
            tint = color,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(section.type.titleRes()),
            style = MaterialTheme.typography.titleSmall,
            color = color,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${section.elementCount}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        if (section.isFlagged) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SectionContent(
    section: ManifestViewerViewModel.SectionUiModel,
) {
    val annotatedXml = highlightMatches(
        xml = section.prettyXml,
        matchRanges = section.matchRanges,
        activeMatchRange = section.activeMatchRange,
    )

    SelectionContainer {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
        ) {
            Text(
                text = annotatedXml,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = if (section.isFlagged) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun highlightMatches(
    xml: String,
    matchRanges: List<ManifestViewerViewModel.MatchRange>,
    activeMatchRange: ManifestViewerViewModel.MatchRange?,
): AnnotatedString {
    if (matchRanges.isEmpty()) return AnnotatedString(xml)

    val highlightColor = MaterialTheme.colorScheme.tertiaryContainer
    val highlightTextColor = MaterialTheme.colorScheme.onTertiaryContainer
    val activeColor = MaterialTheme.colorScheme.primaryContainer
    val activeTextColor = MaterialTheme.colorScheme.onPrimaryContainer

    return buildAnnotatedString {
        append(xml)
        matchRanges.forEach { range ->
            val isActive = activeMatchRange != null &&
                range.start == activeMatchRange.start &&
                range.endExclusive == activeMatchRange.endExclusive
            addStyle(
                SpanStyle(
                    background = if (isActive) activeColor else highlightColor,
                    color = if (isActive) activeTextColor else highlightTextColor,
                ),
                start = range.start,
                end = range.endExclusive,
            )
        }
    }
}

@StringRes
private fun SectionType.titleRes(): Int = when (this) {
    SectionType.USES_PERMISSION -> R.string.apps_manifest_section_uses_permission
    SectionType.PERMISSION -> R.string.apps_manifest_section_permission
    SectionType.QUERIES -> R.string.apps_manifest_section_queries
    SectionType.ACTIVITIES -> R.string.apps_manifest_section_activities
    SectionType.SERVICES -> R.string.apps_manifest_section_services
    SectionType.RECEIVERS -> R.string.apps_manifest_section_receivers
    SectionType.PROVIDERS -> R.string.apps_manifest_section_providers
    SectionType.META_DATA -> R.string.apps_manifest_section_meta_data
    SectionType.OTHER -> R.string.apps_manifest_section_other
}
