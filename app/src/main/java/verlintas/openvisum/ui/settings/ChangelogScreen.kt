package verlintas.openvisum.ui.settings

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import verlintas.openvisum.R
import verlintas.openvisum.ui.components.staggeredEntrance

private data class ChangelogEntry(
    val version: String,
    val date: String?,
    val sections: List<ChangelogSection>,
)

private data class ChangelogSection(
    val title: String,
    val items: List<String>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<ChangelogEntry>?>(null) }

    LaunchedEffect(Unit) {
        entries = withContext(Dispatchers.IO) {
            runCatching { parseChangelog(readChangelogAsset(context)) }.getOrDefault(emptyList())
        }
    }

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.changelog_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val loaded = entries
            if (loaded == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (loaded.isEmpty()) {
                Text(
                    text = stringResource(R.string.changelog_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(loaded, key = { _, entry -> entry.version }) { index, entry ->
                        ChangelogCard(entry = entry, index = index)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangelogCard(entry: ChangelogEntry, index: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredEntrance(index),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "v${entry.version}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                entry.date?.let { date ->
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            entry.sections.forEach { section ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                section.items.forEach { item ->
                    Row(modifier = Modifier.padding(top = 6.dp)) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

private fun readChangelogAsset(context: Context): String =
    context.assets.open("CHANGELOG.md").bufferedReader().use { it.readText() }

private fun parseChangelog(markdown: String): List<ChangelogEntry> {
    val entries = mutableListOf<ChangelogEntry>()
    var currentVersion: String? = null
    var currentDate: String? = null
    val sections = mutableListOf<ChangelogSection>()
    var currentSectionTitle: String? = null
    val sectionItems = mutableListOf<String>()
    val ungroupedItems = mutableListOf<String>()

    fun flushSection() {
        val title = currentSectionTitle
        if (title != null && sectionItems.isNotEmpty()) {
            sections += ChangelogSection(title, sectionItems.toList())
        }
        sectionItems.clear()
    }

    fun flushEntry() {
        flushSection()
        if (currentSectionTitle == null && ungroupedItems.isNotEmpty()) {
            sections += ChangelogSection("", ungroupedItems.toList())
        }
        ungroupedItems.clear()
        val version = currentVersion ?: return
        entries += ChangelogEntry(version, currentDate, sections.toList())
        sections.clear()
        currentVersion = null
        currentDate = null
        currentSectionTitle = null
    }

    markdown.lineSequence().forEach { rawLine ->
        val line = rawLine.trimEnd()
        when {
            line.startsWith("## ") -> {
                flushEntry()
                val header = line.removePrefix("## ").trim()
                val match = Regex("""\[?([0-9][0-9.]*)\]?(?:\s*-\s*(\S+))?""").find(header)
                currentVersion = match?.groupValues?.get(1) ?: header
                currentDate = match?.groupValues?.getOrNull(2)?.takeIf { it.isNotBlank() }
            }

            line.startsWith("### ") -> {
                flushSection()
                currentSectionTitle = line.removePrefix("### ").trim()
            }

            line.startsWith("- ") -> {
                val item = line.removePrefix("- ").trim()
                if (currentSectionTitle != null) {
                    sectionItems += item
                } else {
                    ungroupedItems += item
                }
            }

            line.startsWith("  ") && line.isNotBlank() -> {
                val continuation = line.trim()
                if (currentSectionTitle != null && sectionItems.isNotEmpty()) {
                    sectionItems[sectionItems.lastIndex] =
                        sectionItems.last() + " " + continuation
                } else if (ungroupedItems.isNotEmpty()) {
                    ungroupedItems[ungroupedItems.lastIndex] =
                        ungroupedItems.last() + " " + continuation
                }
            }
        }
    }
    flushEntry()
    return entries
}
