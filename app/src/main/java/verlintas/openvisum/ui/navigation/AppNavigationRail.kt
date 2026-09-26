package verlintas.openvisum.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import verlintas.openvisum.R

private data class RailDestination(
    val icon: ImageVector,
    val labelRes: Int,
    val route: String,
)

@Composable
fun AppNavigationRail(
    selectedIndex: Int,
    onNavigate: (index: Int, route: String) -> Unit,
    onOpenWebsite: () -> Unit,
    versionName: String,
) {
    val destinations = listOf(
        RailDestination(Icons.Filled.Home, R.string.nav_home, "home"),
        RailDestination(Icons.Filled.VideoLibrary, R.string.library_tab_library, "library"),
        RailDestination(Icons.Filled.Cloud, R.string.network_title, "network"),
        RailDestination(Icons.Filled.Settings, R.string.settings_title, "settings"),
    )

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxHeight()
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing),
        header = {
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        },
    ) {
        destinations.forEachIndexed { index, destination ->
            NavigationRailItem(
                selected = selectedIndex == index,
                onClick = { onNavigate(index, destination.route) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes),
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                },
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        NavigationRailItem(
            selected = false,
            onClick = onOpenWebsite,
            icon = { Icon(Icons.Filled.Public, contentDescription = null) },
            label = {
                Text(
                    text = stringResource(R.string.about_link_website),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            },
        )
        Text(
            text = versionName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}
