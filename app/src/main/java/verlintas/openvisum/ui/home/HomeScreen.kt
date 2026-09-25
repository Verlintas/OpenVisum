package verlintas.openvisum.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import verlintas.openvisum.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenMedia: (android.net.Uri, String?) -> Unit,
) {
    var showUrlDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onOpenMedia(uri, null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { filePicker.launch(arrayOf("video/*", "audio/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_open_file))
            }
            FilledTonalButton(
                onClick = { showUrlDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Link, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_open_url))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_library_coming_soon),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showUrlDialog) {
        UrlDialog(
            onDismiss = { showUrlDialog = false },
            onConfirm = { url ->
                showUrlDialog = false
                onOpenMedia(android.net.Uri.parse(url), null)
            },
        )
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.home_url_support_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
