package verlintas.openvisum.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
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
import verlintas.openvisum.ui.components.AppIcon

data class SidebarDestination(
    val icon: ImageVector,
    val labelRes: Int,
    val route: String,
)

data class SidebarData(
    val folders: List<SidebarFolder> = emptyList(),
    val safFolders: List<SidebarFolder> = emptyList(),
    val networkSourceCount: Int = 0,
    val totalCount: Int = 0,
    val totalSizeBytes: Long = 0,
    val storageUsedBytes: Long = 0,
    val storageTotalBytes: Long = 0,
)

data class SidebarFolder(
    val title: String,
    val itemCount: Int,
    val key: String,
)

val sidebarDestinations: List<SidebarDestination>
    @Composable get() = listOf(
        SidebarDestination(Icons.Filled.Home, R.string.nav_home, "home"),
        SidebarDestination(Icons.Filled.VideoLibrary, R.string.library_tab_library, "library"),
        SidebarDestination(Icons.Filled.Cloud, R.string.network_title, "network"),
        SidebarDestination(Icons.Filled.Settings, R.string.settings_title, "settings"),
    )

@Composable
fun AppSidebar(
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
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxHeight(),
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(size = 42.dp, modifier = Modifier.clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = stringResource(R.string.settings_version, versionName),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                SidebarSectionLabel(stringResource(R.string.sidebar_nav))
                sidebarDestinations.forEach { destination ->
                    SidebarItem(
                        label = stringResource(destination.labelRes),
                        selected = selectedRoute == destination.route,
                        onClick = { onNavigate(destination.route) },
                    )
                }

                SidebarSectionLabel(stringResource(R.string.sidebar_shortcuts))
                SidebarItem(
                    label = stringResource(R.string.sidebar_open_file),
                    selected = false,
                    onClick = onOpenFile,
                )
                SidebarItem(
                    label = stringResource(R.string.sidebar_search),
                    selected = false,
                    onClick = onOpenSearch,
                )
                SidebarItem(
                    label = stringResource(R.string.library_continue_watching),
                    selected = false,
                    onClick = onContinueWatching,
                )

                val hasLocations = data.safFolders.isNotEmpty() ||
                    data.folders.isNotEmpty() ||
                    data.networkSourceCount > 0
                if (hasLocations) {
                    SidebarSectionLabel(stringResource(R.string.sidebar_locations))
                    data.safFolders.forEach { folder ->
                        SidebarItem(
                            label = folder.title,
                            selected = false,
                            onClick = { onOpenSafFolder(folder) },
                        )
                    }
                    data.folders.forEach { folder ->
                        SidebarItem(
                            label = folder.title,
                            selected = false,
                            trailing = "${folder.itemCount}",
                            onClick = { onOpenFolder(folder) },
                        )
                    }
                    if (data.networkSourceCount > 0) {
                        SidebarItem(
                            label = stringResource(R.string.network_title),
                            selected = false,
                            trailing = "${data.networkSourceCount}",
                            onClick = onOpenNetwork,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            SidebarStorage(
                data = data,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.about_link_website),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenWebsite)
                        .padding(vertical = 2.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = versionName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SidebarItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: String? = null,
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        selected = selected,
        onClick = onClick,
        badge = trailing?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 1.dp),
    )
}

@Composable
private fun SidebarSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 28.dp, top = 14.dp, bottom = 4.dp),
    )
}

@Composable
fun SidebarStorage(
    data: SidebarData,
    modifier: Modifier = Modifier,
) {
    val fraction = if (data.storageTotalBytes > 0L) {
        (data.storageUsedBytes.toFloat() / data.storageTotalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.sidebar_storage),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${(fraction * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.sidebar_storage_used,
                sidebarFormatBytes(data.storageUsedBytes),
                sidebarFormatBytes(data.storageTotalBytes),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(
                R.string.sidebar_storage_library,
                sidebarFormatBytes(data.totalSizeBytes),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = stringResource(
                R.string.sidebar_storage_counts,
                data.totalCount,
                data.folders.size + data.safFolders.size,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun sidebarFormatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return if (value >= 100 || index == 0) {
        "${value.toLong()} ${units[index]}"
    } else {
        "%.1f %s".format(value, units[index])
    }
}
