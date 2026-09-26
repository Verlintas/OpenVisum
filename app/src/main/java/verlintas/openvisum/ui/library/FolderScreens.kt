package verlintas.openvisum.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import verlintas.openvisum.R
import verlintas.openvisum.core.common.util.FileSizeUtils
import verlintas.openvisum.ui.components.MediaRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseFolderScreen(
    folderName: String,
    viewModel: FolderBrowseViewModel,
    onPlayUri: (String, String?) -> Unit,
    onBack: () -> Unit,
) {
    val items by viewModel.items.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = folderName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            itemsIndexed(items, key = { _, item -> item.uri }) { index, item ->
                MediaRow(
                    item = item,
                    onClick = { onPlayUri(item.uri, item.title) },
                    onToggleFavorite = { viewModel.toggleFavorite(item) },
                    entranceIndex = index,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafBrowserScreen(
    viewModel: SafBrowserViewModel,
    onPlayUri: (String, String?) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.currentName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { if (state.canNavigateUp) viewModel.navigateUp() else onBack() },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.isLoading && state.entries.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.entries, key = { it.uri }) { entry ->
                        ListItem(
                            headlineContent = {
                                Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = if (!entry.isDirectory && entry.sizeBytes > 0) {
                                { Text(FileSizeUtils.formatSize(entry.sizeBytes)) }
                            } else {
                                null
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = when {
                                        entry.isDirectory -> Icons.Filled.Folder
                                        entry.isSubtitle -> Icons.Filled.Subtitles
                                        else -> Icons.Filled.Movie
                                    },
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.clickable(enabled = entry.isDirectory || entry.isPlayable) {
                                if (entry.isDirectory) {
                                    viewModel.open(entry)
                                } else if (entry.isPlayable) {
                                    onPlayUri(entry.uri, entry.name)
                                }
                            },
                        )
                    }
                    if (state.entries.isEmpty() && !state.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
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
        }
    }
}
