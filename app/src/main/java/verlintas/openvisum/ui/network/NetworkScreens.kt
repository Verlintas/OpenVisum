package verlintas.openvisum.ui.network

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import verlintas.openvisum.R
import verlintas.openvisum.core.common.util.FileSizeUtils
import verlintas.openvisum.core.data.NetworkSource
import verlintas.openvisum.core.data.NetworkSourceType
import verlintas.openvisum.core.data.source.SafFolderRepository
import verlintas.openvisum.ui.components.pressScale
import verlintas.openvisum.ui.components.staggeredEntrance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    viewModel: NetworkViewModel,
    onPlayUri: (String, String?) -> Unit,
    onBrowseSource: (NetworkSource) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var url by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var sourceToDelete by remember { mutableStateOf<NetworkSource?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.network_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEntrance(index = 0),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.network_url_label)) },
                        placeholder = { Text("https://example.com/video.m3u8") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    val playInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            val trimmed = url.trim()
                            if (trimmed.isNotEmpty()) {
                                viewModel.rememberStream(trimmed, null)
                                url = ""
                                onPlayUri(trimmed, null)
                            }
                        },
                        interactionSource = playInteraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(playInteraction),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.common_play))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.network_sources),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.network_add_source))
                }
            }

            if (state.sources.isEmpty()) {
                Text(
                    text = stringResource(R.string.network_no_sources),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(index = 2),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column {
                        state.sources.forEach { source ->
                            ListItem(
                        headlineContent = { Text(source.name) },
                        supportingContent = {
                            Text(
                                text = buildString {
                                    append(source.type.name)
                                    append(" · ")
                                    append(source.host)
                                    if (source.port > 0) append(":${source.port}")
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingContent = { Icon(Icons.Filled.Cloud, contentDescription = null) },
                        trailingContent = {
                            IconButton(onClick = { sourceToDelete = source }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.common_delete),
                                )
                            }
                        },
                                modifier = Modifier.clickableItem { onBrowseSource(source) },
                            )
                        }
                    }
                }
            }

            if (state.history.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.network_recent),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(index = 3),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column {
                        state.history.forEach { item ->
                            ListItem(
                        headlineContent = {
                            Text(
                                text = item.title ?: item.url,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = if (item.title != null) {
                            {
                                Text(
                                    item.url,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        } else {
                            null
                        },
                        leadingContent = { Icon(Icons.Filled.History, contentDescription = null) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.forgetStream(item.url) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.common_delete),
                                )
                            }
                        },
                                modifier = Modifier.clickableItem { onPlayUri(item.url, item.title) },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAddDialog) {
        AddNetworkSourceDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { type, name, host, port, username, password, domain, basePath, useHttps ->
                viewModel.addSource(
                    type = type,
                    name = name,
                    host = host,
                    port = port,
                    username = username,
                    password = password,
                    domain = domain,
                    basePath = basePath,
                    useHttps = useHttps,
                )
                showAddDialog = false
            },
        )
    }

    sourceToDelete?.let { source ->
        AlertDialog(
            onDismissRequest = { sourceToDelete = null },
            title = { Text(stringResource(R.string.network_delete_title)) },
            text = { Text(stringResource(R.string.network_delete_message, source.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSource(source.id)
                        sourceToDelete = null
                    },
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { sourceToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text(stringResource(R.string.network_error_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) {
                    Text(stringResource(R.string.common_ok))
                }
            },
        )
    }
}

private fun Modifier.clickableItem(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkBrowserScreen(
    viewModel: NetworkBrowserViewModel,
    onPlayUri: (String, String?) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.sourceName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.currentPath.isNotBlank()) {
                            Text(
                                text = "/${state.currentPath}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
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
            when {
                state.isLoading && state.entries.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = state.error.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.navigateUp() }) {
                            Text(stringResource(R.string.network_retry))
                        }
                    }
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(state.entries, key = { _, entry -> entry.path }) { index, entry ->
                            val extension = entry.name.substringAfterLast('.', "").lowercase()
                            val playable = !entry.isDirectory &&
                                SafFolderRepository.PLAYABLE_EXTENSIONS.contains(extension)
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
                                            extension in verlintas.openvisum.core.common.util.SubtitleMatcher.SUBTITLE_EXTENSIONS ->
                                                Icons.Filled.Subtitles

                                            else -> Icons.Filled.Movie
                                        },
                                        contentDescription = null,
                                    )
                                },
                                modifier = Modifier
                                    .staggeredEntrance(index)
                                    .clickableItem {
                                        when {
                                            entry.isDirectory -> viewModel.open(entry)
                                            playable -> scope.launch {
                                                viewModel.playbackTarget(entry)
                                                    .onSuccess { target ->
                                                        onPlayUri(target.uri.toString(), target.title)
                                                    }
                                            }
                                        }
                                    },
                            )
                        }
                        if (state.entries.isEmpty() && !state.isLoading) {
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
                    if (state.isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun AddNetworkSourceDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        type: NetworkSourceType,
        name: String,
        host: String,
        port: Int,
        username: String?,
        password: String?,
        domain: String?,
        basePath: String?,
        useHttps: Boolean,
    ) -> Unit,
) {
    var typeIndex by rememberSaveable { mutableIntStateOf(0) }
    val type = if (typeIndex == 0) NetworkSourceType.SMB else NetworkSourceType.WEBDAV
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var basePath by remember { mutableStateOf("") }
    var useHttps by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.network_add_source)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == NetworkSourceType.SMB,
                        onClick = { typeIndex = 0 },
                        label = { Text("SMB") },
                    )
                    FilterChip(
                        selected = type == NetworkSourceType.WEBDAV,
                        onClick = { typeIndex = 1 },
                        label = { Text("WebDAV") },
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.network_field_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(stringResource(R.string.network_field_host)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter(Char::isDigit) },
                    label = {
                        Text(
                            stringResource(
                                if (type == NetworkSourceType.SMB) {
                                    R.string.network_field_port_smb
                                } else {
                                    R.string.network_field_port_webdav
                                },
                            ),
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.network_field_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.network_field_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                if (type == NetworkSourceType.SMB) {
                    OutlinedTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        label = { Text(stringResource(R.string.network_field_domain)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedTextField(
                        value = basePath,
                        onValueChange = { basePath = it },
                        label = { Text(stringResource(R.string.network_field_base_path)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = useHttps,
                            onClick = { useHttps = !useHttps },
                            label = { Text(stringResource(R.string.network_field_https)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (host.isNotBlank()) {
                        val defaultPort = if (type == NetworkSourceType.SMB) 445 else if (useHttps) 443 else 80
                        onConfirm(
                            type,
                            name.ifBlank { host },
                            host.trim(),
                            port.toIntOrNull() ?: defaultPort,
                            username.takeIf { it.isNotBlank() },
                            password.takeIf { it.isNotBlank() },
                            domain.takeIf { it.isNotBlank() },
                            basePath.takeIf { it.isNotBlank() },
                            useHttps,
                        )
                    }
                },
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
