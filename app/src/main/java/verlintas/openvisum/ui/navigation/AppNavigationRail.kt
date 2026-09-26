package verlintas.openvisum.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import verlintas.openvisum.R
import verlintas.openvisum.ui.components.AppIcon

@Composable
fun AppNavigationRail(
    selectedRoute: String?,
    data: SidebarData,
    versionName: String,
    onNavigate: (String) -> Unit,
    onOpenFile: () -> Unit,
    onOpenSearch: () -> Unit,
    onContinueWatching: () -> Unit,
    onOpenSafFolder: (SidebarFolder) -> Unit,
    onOpenFolder: (SidebarFolder) -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenWebsite: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxHeight()
            .width(148.dp)
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppIcon(
                size = 38.dp,
                modifier = Modifier
                    .padding(start = 18.dp, top = 16.dp, bottom = 10.dp)
                    .clip(RoundedCornerShape(11.dp)),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                RailSectionLabel(stringResource(R.string.sidebar_nav))
                sidebarDestinations.forEach { destination ->
                    RailTextItem(
                        label = stringResource(destination.labelRes),
                        selected = selectedRoute == destination.route,
                        onClick = { onNavigate(destination.route) },
                    )
                }

                RailSectionLabel(stringResource(R.string.sidebar_shortcuts))
                RailTextItem(
                    label = stringResource(R.string.sidebar_open_file),
                    selected = false,
                    onClick = onOpenFile,
                )
                RailTextItem(
                    label = stringResource(R.string.sidebar_search),
                    selected = false,
                    onClick = onOpenSearch,
                )
                RailTextItem(
                    label = stringResource(R.string.library_continue_watching),
                    selected = false,
                    onClick = onContinueWatching,
                )

                val hasLocations = data.safFolders.isNotEmpty() ||
                    data.folders.isNotEmpty() ||
                    data.networkSourceCount > 0
                if (hasLocations) {
                    RailSectionLabel(stringResource(R.string.sidebar_locations))
                    data.safFolders.forEach { folder ->
                        RailTextItem(
                            label = folder.title,
                            selected = false,
                            onClick = { onOpenSafFolder(folder) },
                        )
                    }
                    data.folders.forEach { folder ->
                        RailTextItem(
                            label = folder.title,
                            selected = false,
                            trailing = "${folder.itemCount}",
                            onClick = { onOpenFolder(folder) },
                        )
                    }
                    if (data.networkSourceCount > 0) {
                        RailTextItem(
                            label = stringResource(R.string.network_title),
                            selected = false,
                            trailing = "${data.networkSourceCount}",
                            onClick = onOpenNetwork,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            SidebarStorage(
                data = data,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.about_link_website),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenWebsite)
                    .padding(vertical = 4.dp),
            )
            Text(
                text = versionName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 18.dp, top = 2.dp, bottom = 14.dp),
            )
        }
    }
}

@Composable
private fun RailTextItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: String? = null,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RailSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        maxLines = 1,
        modifier = Modifier.padding(start = 22.dp, top = 14.dp, bottom = 4.dp),
    )
}
