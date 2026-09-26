package verlintas.openvisum.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import verlintas.openvisum.R
import verlintas.openvisum.ui.components.BrandMark

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
            .width(88.dp)
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandMark(size = 40.dp, modifier = Modifier.padding(top = 14.dp, bottom = 12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                sidebarDestinations.forEach { destination ->
                    RailNavItem(
                        icon = destination.icon,
                        label = stringResource(destination.labelRes),
                        selected = selectedRoute == destination.route,
                        onClick = { onNavigate(destination.route) },
                    )
                }

                RailDivider()
                RailSectionLabel(stringResource(R.string.sidebar_shortcuts))
                RailCompactItem(
                    icon = Icons.Filled.FolderOpen,
                    label = stringResource(R.string.sidebar_open_file),
                    onClick = onOpenFile,
                )
                RailCompactItem(
                    icon = Icons.Filled.Search,
                    label = stringResource(R.string.sidebar_search),
                    onClick = onOpenSearch,
                )
                RailCompactItem(
                    icon = Icons.Filled.History,
                    label = stringResource(R.string.library_continue_watching),
                    onClick = onContinueWatching,
                )

                val hasLocations = data.safFolders.isNotEmpty() ||
                    data.folders.isNotEmpty() ||
                    data.networkSourceCount > 0
                if (hasLocations) {
                    RailSectionLabel(stringResource(R.string.sidebar_locations))
                    data.safFolders.forEach { folder ->
                        RailCompactItem(
                            icon = Icons.Filled.Folder,
                            label = folder.title,
                            onClick = { onOpenSafFolder(folder) },
                        )
                    }
                    data.folders.forEach { folder ->
                        RailCompactItem(
                            icon = Icons.Filled.FolderOpen,
                            label = folder.title,
                            onClick = { onOpenFolder(folder) },
                        )
                    }
                    if (data.networkSourceCount > 0) {
                        RailCompactItem(
                            icon = Icons.Filled.Cloud,
                            label = stringResource(R.string.network_title),
                            onClick = onOpenNetwork,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            RailDivider()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${data.totalCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = sidebarFormatBytes(data.totalSizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            RailCompactItem(
                icon = Icons.Filled.Public,
                label = stringResource(R.string.about_link_website),
                onClick = onOpenWebsite,
            )
            Text(
                text = versionName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun RailNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .widthIn(max = 72.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(30.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RailCompactItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 76.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun RailSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        maxLines = 1,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
    )
}

@Composable
private fun RailDivider() {
    Spacer(Modifier.height(6.dp))
    HorizontalDivider(
        modifier = Modifier.width(32.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
    Spacer(Modifier.height(6.dp))
}
