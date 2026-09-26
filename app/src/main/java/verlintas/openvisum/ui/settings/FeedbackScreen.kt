package verlintas.openvisum.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import verlintas.openvisum.BuildConfig
import verlintas.openvisum.R
import verlintas.openvisum.ui.components.SettingsGroup
import verlintas.openvisum.ui.components.SettingsGroupLabel
import verlintas.openvisum.ui.components.SettingsHintText
import verlintas.openvisum.ui.components.staggeredEntrance

private const val FEEDBACK_EMAIL = "ulv777777@gmail.com"
private const val ISSUE_URL = "https://github.com/Verlintas/OpenVisum/issues/new"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var categoryIndex by remember { mutableIntStateOf(0) }
    var content by remember { mutableStateOf("") }

    val categories = listOf(
        stringResource(R.string.feedback_category_bug),
        stringResource(R.string.feedback_category_feature),
        stringResource(R.string.feedback_category_other),
    )
    val deviceInfo = remember { buildDeviceInfo(context) }

    fun fullReport(): String = buildString {
        appendLine(content.trim())
        appendLine()
        appendLine("----")
        appendLine(deviceInfo)
    }

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.feedback_title),
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            SettingsHintText(stringResource(R.string.feedback_intro))

            SettingsGroupLabel(stringResource(R.string.feedback_category))
            SettingsGroup(modifier = Modifier.staggeredEntrance(index = 0)) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = categoryIndex == 0,
                        onClick = { categoryIndex = 0 },
                        label = { Text(categories[0]) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.BugReport,
                                contentDescription = null,
                                modifier = Modifier.height(18.dp),
                            )
                        },
                    )
                    FilterChip(
                        selected = categoryIndex == 1,
                        onClick = { categoryIndex = 1 },
                        label = { Text(categories[1]) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.height(18.dp),
                            )
                        },
                    )
                    FilterChip(
                        selected = categoryIndex == 2,
                        onClick = { categoryIndex = 2 },
                        label = { Text(categories[2]) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.MoreHoriz,
                                contentDescription = null,
                                modifier = Modifier.height(18.dp),
                            )
                        },
                    )
                }
            }

            SettingsGroupLabel(stringResource(R.string.feedback_content))
            SettingsGroup(modifier = Modifier.staggeredEntrance(index = 1)) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text(stringResource(R.string.feedback_content_hint)) },
                    minLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                )
            }

            SettingsGroupLabel(stringResource(R.string.feedback_device_info))
            SettingsGroup(modifier = Modifier.staggeredEntrance(index = 2)) {
                Text(
                    text = deviceInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            "[OpenVisum] ${categories[categoryIndex]}",
                        )
                        putExtra(Intent.EXTRA_TEXT, fullReport())
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(intent, null))
                    }.onFailure {
                        scope.launch {
                            snackbar.showSnackbar(context.getString(R.string.feedback_no_email_app))
                        }
                    }
                },
                enabled = content.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text(
                    text = stringResource(R.string.feedback_send_email),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("OpenVisum feedback", fullReport()))
                    scope.launch { snackbar.showSnackbar(context.getString(R.string.feedback_copied)) }
                },
                enabled = content.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Text(
                    text = stringResource(R.string.feedback_copy),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val url = Uri.parse(ISSUE_URL).buildUpon()
                        .appendQueryParameter(
                            "title",
                            "[${categories[categoryIndex]}] ",
                        )
                        .appendQueryParameter("body", fullReport())
                        .build()
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, url)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
                enabled = content.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Email, contentDescription = null)
                Text(
                    text = stringResource(R.string.feedback_open_issue),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            SettingsHintText(stringResource(R.string.feedback_issue_hint))
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun buildDeviceInfo(context: Context): String = buildString {
    appendLine("OpenVisum ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
    appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    appendLine("ABI: ${Build.SUPPORTED_ABIS.firstOrNull().orEmpty()}")
}.trim()
