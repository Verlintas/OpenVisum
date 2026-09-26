package verlintas.openvisum.ui.library

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import verlintas.openvisum.R
import verlintas.openvisum.core.common.util.TimeUtils
import verlintas.openvisum.core.data.FolderSummary
import verlintas.openvisum.core.data.db.SafFolderEntity
import verlintas.openvisum.core.data.model.MediaItem
import verlintas.openvisum.core.data.prefs.SortOrder
import verlintas.openvisum.ui.components.MediaGridCard
import verlintas.openvisum.ui.components.MediaRow
import verlintas.openvisum.ui.components.MediaThumbnail
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onPlayUri: (String, String?) -> Unit,
    onOpenFolder: (FolderSummary) -> Unit,
    onOpenSafFolder: (SafFolderEntity) -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var folderToRemove by remember { mutableStateOf<SafFolderEntity?>(null) }
    var hasPermission by remember { mutableStateOf(hasMediaPermission(context)) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted || hasMediaPermission(context)
        if (hasPermission) viewModel.refresh()
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let(viewModel::addSafFolder)
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { onPlayUri(it.toString(), null) }
    }

    LaunchedEffect(Unit) {
        if (hasMediaPermission(context)) {
            viewModel.refresh()
        } else {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (searchActive) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = viewModel::setQuery,
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.library_search_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            searchActive = false
                            viewModel.setQuery("")
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_cancel))
                        }
                    },
                )
            } else {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(onClick = onOpenNetwork) {
                            Icon(Icons.Filled.Cloud, contentDescription = stringResource(R.string.network_title))
                        }
                        IconButton(onClick = { searchActive = true }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.library_search))
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.library_sort))
                            }
                            SortMenu(
                                expanded = showSortMenu,
                                current = state.sortOrder,
                                onDismiss = { showSortMenu = false },
                                onSelect = {
                                    viewModel.setSortOrder(it)
                                    showSortMenu = false
                                },
                            )
                        }
                        Box {
                            IconButton(onClick = { showOverflow = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showOverflow,
                                onDismissRequest = { showOverflow = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.library_refresh)) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Refresh, contentDescription = null)
                                    },
                                    onClick = {
                                        showOverflow = false
                                        if (hasMediaPermission(context)) {
                                            viewModel.refresh()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.library_open_file)) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.FolderOpen, contentDescription = null)
                                    },
                                    onClick = {
                                        showOverflow = false
                                        filePicker.launch(arrayOf("video/*", "audio/*"))
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.library_open_url)) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Link, contentDescription = null)
                                    },
                                    onClick = {
                                        showOverflow = false
                                        showUrlDialog = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.settings_title)) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Settings, contentDescription = null)
                                    },
                                    onClick = {
                                        showOverflow = false
                                        onOpenSettings()
                                    },
                                )
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                SegmentedButton(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) {
                    Text(stringResource(R.string.library_tab_library))
                }
                SegmentedButton(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) {
                    Text(stringResource(R.string.library_tab_folders))
                }
            }

            Crossfade(
                targetState = tab,
                label = "libraryTab",
            ) { currentTab ->
                when (currentTab) {
                    0 -> LibraryContent(
                        state = state,
                        hasPermission = hasPermission,
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                        },
                        onPlay = { item -> onPlayUri(item.uri, item.title) },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onFilterChange = viewModel::setFilter,
                    )

                    else -> FoldersContent(
                        state = state,
                        onAddFolder = { folderPicker.launch(null) },
                        onOpenFolder = onOpenFolder,
                        onOpenSafFolder = onOpenSafFolder,
                        onRemoveSafFolder = { folderToRemove = it },
                    )
                }
            }
        }
    }

    folderToRemove?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToRemove = null },
            title = { Text(stringResource(R.string.library_remove_folder_title)) },
            text = { Text(stringResource(R.string.library_remove_folder_message, folder.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeSafFolder(folder.treeUri)
                        folderToRemove = null
                    },
                ) {
                    Text(stringResource(R.string.common_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToRemove = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showUrlDialog) {
        UrlDialog(
            onDismiss = { showUrlDialog = false },
            onConfirm = { url ->
                showUrlDialog = false
                onPlayUri(url, null)
            },
        )
    }
}

private enum class DateGroup { TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, EARLIER }

private data class MediaSection(
    val group: DateGroup?,
    val items: List<MediaItem>,
)

private fun buildSections(items: List<MediaItem>, sortOrder: SortOrder): List<MediaSection> {
    if (sortOrder != SortOrder.DATE_DESC) {
        return listOf(MediaSection(group = null, items = items))
    }
    val today = LocalDate.now()
    val grouped = items.groupBy { item -> dateGroupOf(item, today) }
    return DateGroup.entries.mapNotNull { group ->
        grouped[group]?.let { MediaSection(group = group, items = it) }
    }
}

private fun dateGroupOf(item: MediaItem, today: LocalDate): DateGroup {
    val date = runCatching {
        Instant.ofEpochSecond(item.dateAddedSeconds).atZone(ZoneId.systemDefault()).toLocalDate()
    }.getOrDefault(LocalDate.MIN)
    return when {
        !date.isBefore(today) -> DateGroup.TODAY
        date == today.minusDays(1) -> DateGroup.YESTERDAY
        !date.isBefore(today.minusDays(7)) -> DateGroup.THIS_WEEK
        !date.isBefore(today.minusDays(31)) -> DateGroup.THIS_MONTH
        else -> DateGroup.EARLIER
    }
}

@Composable
private fun LibraryContent(
    state: LibraryUiState,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onPlay: (MediaItem) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onFilterChange: (LibraryFilter) -> Unit,
) {
    Crossfade(
        targetState = when {
            !hasPermission -> LibraryStage.PERMISSION
            state.isLoading -> LibraryStage.LOADING
            else -> LibraryStage.CONTENT
        },
        label = "libraryStage",
    ) { stage ->
        when (stage) {
            LibraryStage.PERMISSION -> PermissionCard(onRequestPermission)

            LibraryStage.LOADING -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            LibraryStage.CONTENT -> {
                val sections = remember(state.items, state.sortOrder) {
                    buildSections(state.items, state.sortOrder)
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.query.isBlank() && state.continueWatching.isNotEmpty()) {
                        item(
                            key = "continue-title",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + expandVertically(),
                            ) {
                                SectionTitle(stringResource(R.string.library_continue_watching))
                            }
                        }
                        item(
                            key = "continue-row",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(state.continueWatching, key = { it.uri }) { item ->
                                    ContinueWatchingCard(
                                        item = item,
                                        onClick = { onPlay(item) },
                                    )
                                }
                            }
                        }
                    }

                    item(
                        key = "filters",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilterChip(
                                selected = state.filter == LibraryFilter.ALL,
                                onClick = { onFilterChange(LibraryFilter.ALL) },
                                label = { Text(stringResource(R.string.library_filter_all)) },
                            )
                            Spacer(Modifier.width(8.dp))
                            FilterChip(
                                selected = state.filter == LibraryFilter.FAVORITES,
                                onClick = { onFilterChange(LibraryFilter.FAVORITES) },
                                label = { Text(stringResource(R.string.library_filter_favorites)) },
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = stringResource(R.string.library_items_count, state.items.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (state.items.isEmpty()) {
                        item(
                            key = "empty",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            EmptyLibraryHint()
                        }
                    } else {
                        sections.forEach { section ->
                            section.group?.let { group ->
                                item(
                                    key = "header-${group.name}",
                                    span = { GridItemSpan(maxLineSpan) },
                                ) {
                                    SectionTitle(dateGroupLabel(group))
                                }
                            }
                            gridItems(
                                items = section.items,
                                key = { it.uri },
                            ) { item ->
                                MediaGridCard(
                                    item = item,
                                    onClick = { onPlay(item) },
                                    onToggleFavorite = { onToggleFavorite(item) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class LibraryStage { PERMISSION, LOADING, CONTENT }

@Composable
private fun dateGroupLabel(group: DateGroup): String = stringResource(
    when (group) {
        DateGroup.TODAY -> R.string.library_date_today
        DateGroup.YESTERDAY -> R.string.library_date_yesterday
        DateGroup.THIS_WEEK -> R.string.library_date_this_week
        DateGroup.THIS_MONTH -> R.string.library_date_this_month
        DateGroup.EARLIER -> R.string.library_date_earlier
    },
)

@Composable
private fun EmptyLibraryHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.VideoLibrary,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.library_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ContinueWatchingCard(
    item: MediaItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick),
    ) {
        Column {
            MediaThumbnail(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp),
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = {
                        (item.playbackPositionMs.toFloat() / item.playbackDurationMs.coerceAtLeast(1L))
                            .coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.library_resume_at,
                        TimeUtils.formatDuration(item.playbackPositionMs),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FoldersContent(
    state: LibraryUiState,
    onAddFolder: () -> Unit,
    onOpenFolder: (FolderSummary) -> Unit,
    onOpenSafFolder: (SafFolderEntity) -> Unit,
    onRemoveSafFolder: (SafFolderEntity) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Button(
                onClick = onAddFolder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.library_add_folder))
            }
        }

        if (state.safFolders.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.library_saf_folders)) }
            items(state.safFolders, key = { it.treeUri }) { folder ->
                ListItem(
                    headlineContent = { Text(folder.name) },
                    leadingContent = { Icon(Icons.Filled.Folder, contentDescription = null) },
                    trailingContent = {
                        IconButton(onClick = { onRemoveSafFolder(folder) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_remove))
                        }
                    },
                    modifier = Modifier
                        .animateItem()
                        .clickable { onOpenSafFolder(folder) },
                )
            }
        }

        if (state.folders.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.library_device_folders)) }
            items(state.folders, key = { it.folderKey }) { folder ->
                ListItem(
                    headlineContent = { Text(folder.folderName) },
                    supportingContent = {
                        Text(stringResource(R.string.library_items_count, folder.itemCount))
                    },
                    leadingContent = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                    modifier = Modifier
                        .animateItem()
                        .clickable { onOpenFolder(folder) },
                )
            }
        }

        if (state.safFolders.isEmpty() && state.folders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.library_folder_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.library_permission_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.library_permission_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onRequestPermission) {
                Text(stringResource(R.string.library_permission_grant))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun SortMenu(
    expanded: Boolean,
    current: SortOrder,
    onDismiss: () -> Unit,
    onSelect: (SortOrder) -> Unit,
) {
    val options = listOf(
        SortOrder.DATE_DESC to stringResource(R.string.library_sort_date),
        SortOrder.NAME_ASC to stringResource(R.string.library_sort_name),
        SortOrder.SIZE_DESC to stringResource(R.string.library_sort_size),
        SortOrder.DURATION_DESC to stringResource(R.string.library_sort_duration),
    )
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        options.forEach { (order, label) ->
            DropdownMenuItem(
                text = { Text(label) },
                trailingIcon = if (order == current) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else {
                    null
                },
                onClick = { onSelect(order) },
            )
        }
    }
}

@Composable
private fun UrlDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_url_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.home_url_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_url_support_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = url.trim()
                    if (trimmed.isNotEmpty()) onConfirm(trimmed)
                },
            ) {
                Text(stringResource(R.string.common_play))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

private fun hasMediaPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) ==
        PackageManager.PERMISSION_GRANTED
