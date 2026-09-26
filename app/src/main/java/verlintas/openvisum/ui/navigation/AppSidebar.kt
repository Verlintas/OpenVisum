package verlintas.openvisum.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
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

data class SidebarController(
    val enabled: Boolean = false,
    val open: () -> Unit = {},
)

val LocalSidebarController = androidx.compose.runtime.staticCompositionLocalOf { SidebarController() }

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
                BrandMark(size = 44.dp)
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

            SidebarSectionLabel(stringResource(R.string.sidebar_nav))
            sidebarDestinations.forEach { destination ->
                NavigationDrawerItem(
                    label = { Text(stringResource(destination.labelRes)) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    selected = selectedRoute == destination.route,
                    onClick = { onNavigate(destination.route) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }

            SidebarSectionLabel(stringResource(R.string.sidebar_shortcuts))
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.sidebar_open_file)) },
                icon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                selected = false,
                onClick = onOpenFile,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.sidebar_search)) },
                icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                selected = false,
                onClick = onOpenSearch,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.library_continue_watching)) },
                icon = { Icon(Icons.Filled.History, contentDescription = null) },
                selected = false,
                onClick = onContinueWatching,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )

            val hasLocations = data.safFolders.isNotEmpty() ||
                data.folders.isNotEmpty() ||
                data.networkSourceCount > 0
            if (hasLocations) {
                SidebarSectionLabel(stringResource(R.string.sidebar_locations))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    data.safFolders.forEach { folder ->
                        NavigationDrawerItem(
                            label = {
                                Text(folder.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                            selected = false,
                            onClick = { onOpenSafFolder(folder) },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        )
                    }
                    data.folders.forEach { folder ->
                        NavigationDrawerItem(
                            label = {
                                Text(folder.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            icon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                            selected = false,
                            badge = {
                                Text(
                                    text = "${folder.itemCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            onClick = { onOpenFolder(folder) },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        )
                    }
                    if (data.networkSourceCount > 0) {
                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.network_title)) },
                            icon = { Icon(Icons.Filled.Cloud, contentDescription = null) },
                            selected = false,
                            badge = {
                                Text(
                                    text = "${data.networkSourceCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            onClick = onOpenNetwork,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                Text(
                    text = stringResource(R.string.sidebar_status),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.6.sp,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            R.string.sidebar_stats,
                            data.totalCount,
                            sidebarFormatBytes(data.totalSizeBytes),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            NavigationDrawerItem(
                label = { Text(stringResource(R.string.about_link_website)) },
                icon = { Icon(Icons.Filled.Public, contentDescription = null) },
                selected = false,
                onClick = onOpenWebsite,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun SidebarSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 28.dp, top = 14.dp, bottom = 6.dp),
    )
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
