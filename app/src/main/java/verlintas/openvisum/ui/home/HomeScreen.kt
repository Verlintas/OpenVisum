package verlintas.openvisum.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.OutlinedButton
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import verlintas.openvisum.BuildConfig
import verlintas.openvisum.R
import verlintas.openvisum.core.common.util.FileSizeUtils
import verlintas.openvisum.core.common.util.TimeUtils
import verlintas.openvisum.core.data.FolderSummary
import verlintas.openvisum.core.data.NetworkSource
import verlintas.openvisum.core.data.db.SafFolderEntity
import verlintas.openvisum.core.data.model.MediaItem
import verlintas.openvisum.ui.components.MediaThumbnail
import verlintas.openvisum.ui.components.floatingIcon
import verlintas.openvisum.ui.components.pressScaleClickable
import verlintas.openvisum.ui.components.staggeredEntrance

private const val TAB_LIBRARY = 0
private const val TAB_FOLDERS = 1

private data class DrawerDestination(
    val icon: ImageVector,
    val labelRes: Int,
    val onClick: () -> Unit,
    val selected: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlayUri: (String, String?, Boolean) -> Unit,
    onOpenLibrary: (tab: Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFolder: (FolderSummary) -> Unit,
    onOpenSafFolder: (SafFolderEntity) -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWebsite: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = androidx.compose.material3.rememberDrawerState(
        androidx.compose.material3.DrawerValue.Closed,
    )
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(hasMediaPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted || hasMediaPermission(context)
        if (hasPermission) viewModel.refresh()
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { onPlayUri(it.toString(), null, false) }
    }

    LaunchedEffect(Unit) {
        if (hasMediaPermission(context)) {
            viewModel.refresh()
        } else {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
        }
    }

    val destinations = listOf(
        DrawerDestination(
            icon = Icons.Filled.Home,
            labelRes = R.string.nav_home,
            onClick = {},
            selected = true,
        ),
        DrawerDestination(
            icon = Icons.Filled.VideoLibrary,
            labelRes = R.string.library_tab_library,
            onClick = { onOpenLibrary(TAB_LIBRARY) },
        ),
        DrawerDestination(
            icon = Icons.Filled.Folder,
            labelRes = R.string.library_tab_folders,
            onClick = { onOpenLibrary(TAB_FOLDERS) },
        ),
        DrawerDestination(
            icon = Icons.Filled.Cloud,
            labelRes = R.string.network_title,
            onClick = onOpenNetwork,
        ),
        DrawerDestination(
            icon = Icons.Filled.Settings,
            labelRes = R.string.settings_title,
            onClick = onOpenSettings,
        ),
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wideLayout = maxWidth >= 840.dp
        if (wideLayout) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    HomeContent(
                        state = state,
                        hasPermission = hasPermission,
                        viewModel = viewModel,
                        showMenuButton = false,
                        onOpenDrawer = {},
                        onPlayUri = onPlayUri,
                        onOpenLibrary = onOpenLibrary,
                        onOpenSearch = onOpenSearch,
                        onOpenFile = { filePicker.launch(arrayOf("video/*", "audio/*")) },
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                        },
                        onOpenFolder = onOpenFolder,
                        onOpenSafFolder = onOpenSafFolder,
                        onOpenNetwork = onOpenNetwork,
                        onOpenSettings = onOpenSettings,
                    )
                }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    HomeDrawerContent(
                        destinations = destinations,
                        state = state,
                        versionName = BuildConfig.VERSION_NAME,
                        onContinueWatching = { onOpenLibrary(TAB_LIBRARY) },
                        onOpenFolder = onOpenFolder,
                        onOpenSafFolder = onOpenSafFolder,
                        onOpenNetwork = onOpenNetwork,
                        onOpenWebsite = onOpenWebsite,
                        onNavigate = { action ->
                            scope.launch { drawerState.close() }
                            action()
                        },
                    )
                },
            ) {
                HomeContent(
                    state = state,
                    hasPermission = hasPermission,
                    viewModel = viewModel,
                    showMenuButton = true,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onPlayUri = onPlayUri,
                    onOpenLibrary = onOpenLibrary,
                    onOpenSearch = onOpenSearch,
                    onOpenFile = { filePicker.launch(arrayOf("video/*", "audio/*")) },
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                    },
                    onOpenFolder = onOpenFolder,
                    onOpenSafFolder = onOpenSafFolder,
                    onOpenNetwork = onOpenNetwork,
                    onOpenSettings = onOpenSettings,
                )
            }
        }
    }
}

@Composable
private fun HomeDrawerContent(
    destinations: List<DrawerDestination>,
    state: HomeUiState,
    versionName: String,
    onContinueWatching: () -> Unit,
    onOpenFolder: (FolderSummary) -> Unit,
    onOpenSafFolder: (SafFolderEntity) -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenWebsite: () -> Unit,
    onNavigate: (() -> Unit) -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.settings_version, versionName),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            destinations.forEach { destination ->
                NavigationDrawerItem(
                    label = { Text(stringResource(destination.labelRes)) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    selected = destination.selected,
                    onClick = { onNavigate(destination.onClick) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            DrawerSectionLabel(stringResource(R.string.drawer_shortcuts))
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.library_continue_watching)) },
                icon = { Icon(Icons.Filled.History, contentDescription = null) },
                selected = false,
                onClick = { onNavigate(onContinueWatching) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )

            if (state.safFolders.isNotEmpty() || state.folders.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                DrawerSectionLabel(stringResource(R.string.home_locations))
                state.safFolders.take(4).forEach { folder ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = folder.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                        selected = false,
                        onClick = { onNavigate { onOpenSafFolder(folder) } },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
                state.folders.take(4).forEach { folder ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = folder.folderName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        icon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                        selected = false,
                        onClick = { onNavigate { onOpenFolder(folder) } },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
                if (state.networkSources.isNotEmpty()) {
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.network_title)) },
                        icon = { Icon(Icons.Filled.Cloud, contentDescription = null) },
                        selected = false,
                        badge = { Text("${state.networkSources.size}") },
                        onClick = { onNavigate(onOpenNetwork) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.about_link_website)) },
                icon = { Icon(Icons.Filled.Public, contentDescription = null) },
                selected = false,
                onClick = { onNavigate(onOpenWebsite) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 28.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    hasPermission: Boolean,
    viewModel: HomeViewModel,
    showMenuButton: Boolean,
    onOpenDrawer: () -> Unit,
    onPlayUri: (String, String?, Boolean) -> Unit,
    onOpenLibrary: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFile: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenFolder: (FolderSummary) -> Unit,
    onOpenSafFolder: (SafFolderEntity) -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val heroUri = state.heroItem?.uri

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
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
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(9.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp,
                        )
                    }
                },
                navigationIcon = {
                    if (showMenuButton) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = stringResource(R.string.drawer_open),
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.library_search))
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.library_refresh))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Crossfade(
            targetState = state.isLoading,
            label = "homeLoading",
        ) { loading ->
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0f),
                                ),
                            ),
                        ),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    state.heroItem?.let { hero ->
                        item(key = "hero") {
                            val snackbar = verlintas.openvisum.ui.components.LocalAppSnackbar.current
                            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                            val scope = rememberCoroutineScope()
                            val favoriteMessage = stringResource(
                                if (hero.isFavorite) {
                                    R.string.snack_favorite_removed
                                } else {
                                    R.string.snack_favorite_added
                                },
                            )
                            HeroBanner(
                                item = hero,
                                isResume = state.heroIsResume,
                                onPlay = { onPlayUri(hero.uri, hero.title, false) },
                                onRestart = { onPlayUri(hero.uri, hero.title, true) },
                                onToggleFavorite = {
                                    viewModel.toggleFavorite(hero)
                                    haptic.performHapticFeedback(
                                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
                                    )
                                    scope.launch { snackbar?.showSnackbar(favoriteMessage) }
                                },
                                modifier = Modifier.staggeredEntrance(index = 0),
                            )
                        }
                    }

                    val continueItems = state.continueWatching.filter { it.uri != heroUri }
                    if (continueItems.isNotEmpty()) {
                        item(key = "continue-title") {
                            SectionHeader(
                                title = stringResource(R.string.library_continue_watching),
                                modifier = Modifier.staggeredEntrance(index = 1),
                            )
                        }
                        item(key = "continue-row") {
                            MediaRowSection(
                                items = continueItems,
                                onPlay = { onPlayUri(it.uri, it.title, false) },
                                index = 2,
                            )
                        }
                    }

                    val recentItems = state.recent.filter { it.uri != heroUri }.take(12)
                    if (recentItems.isNotEmpty()) {
                        item(key = "recent-title") {
                            SectionHeader(
                                title = stringResource(R.string.home_recent),
                                action = stringResource(R.string.home_see_all),
                                onAction = { onOpenLibrary(TAB_LIBRARY) },
                                modifier = Modifier.staggeredEntrance(index = 3),
                            )
                        }
                        item(key = "recent-row") {
                            MediaRowSection(
                                items = recentItems,
                                onPlay = { onPlayUri(it.uri, it.title, false) },
                                index = 4,
                            )
                        }
                    }

                    state.folderSections.forEachIndexed { sectionIndex, section ->
                        val items = section.items.filter { it.uri != heroUri }.take(12)
                        if (items.isNotEmpty()) {
                            item(key = "folder-title-${section.folderKey}") {
                                SectionHeader(
                                    title = stringResource(R.string.home_from_folder, section.folderName),
                                    modifier = Modifier.staggeredEntrance(index = 5 + sectionIndex),
                                )
                            }
                            item(key = "folder-row-${section.folderKey}") {
                                MediaRowSection(
                                    items = items,
                                    onPlay = { onPlayUri(it.uri, it.title, false) },
                                    index = 6 + sectionIndex,
                                )
                            }
                        }
                    }

                    val favoriteItems = state.favorites.filter { it.uri != heroUri }.take(12)
                    if (favoriteItems.isNotEmpty()) {
                        item(key = "favorites-title") {
                            SectionHeader(
                                title = stringResource(R.string.home_favorites),
                                action = stringResource(R.string.home_see_all),
                                onAction = { onOpenLibrary(TAB_LIBRARY) },
                                modifier = Modifier.staggeredEntrance(index = 9),
                            )
                        }
                        item(key = "favorites-row") {
                            MediaRowSection(
                                items = favoriteItems,
                                onPlay = { onPlayUri(it.uri, it.title, false) },
                                index = 10,
                            )
                        }
                    }

                    if (!hasPermission) {
                        item(key = "permission") {
                            PermissionCard(
                                onRequest = onRequestPermission,
                                modifier = Modifier
                                    .staggeredEntrance(index = 11)
                                    .padding(top = 12.dp),
                            )
                        }
                    }

                    if (!state.hasAnyContent && hasPermission && state.heroItem == null) {
                        item(key = "empty") {
                            EmptyWorkspaceHint(
                                onOpenFile = onOpenFile,
                                onOpenLibrary = { onOpenLibrary(TAB_LIBRARY) },
                                modifier = Modifier.staggeredEntrance(index = 12),
                            )
                        }
                    }

                    item(key = "utility-title") {
                        SectionHeader(
                            title = stringResource(R.string.drawer_shortcuts),
                            modifier = Modifier.staggeredEntrance(index = 13),
                        )
                    }
                    item(key = "quick") {
                        QuickActionsRow(
                            onOpenFile = onOpenFile,
                            onOpenLibrary = { onOpenLibrary(TAB_LIBRARY) },
                            onOpenNetwork = onOpenNetwork,
                            onOpenSettings = onOpenSettings,
                            modifier = Modifier.staggeredEntrance(index = 14),
                        )
                    }

                    if (state.folders.isNotEmpty() || state.safFolders.isNotEmpty() || state.networkSources.isNotEmpty()) {
                        item(key = "locations-title") {
                            SectionHeader(
                                title = stringResource(R.string.home_locations),
                                modifier = Modifier.staggeredEntrance(index = 15),
                            )
                        }
                        item(key = "locations-row") {
                            LazyRow(
                                modifier = Modifier.staggeredEntrance(16),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(state.safFolders, key = { "saf-${it.treeUri}" }) { folder ->
                                    LocationChip(
                                        icon = Icons.Filled.Folder,
                                        title = folder.name,
                                        subtitle = stringResource(R.string.home_saf_folder),
                                        onClick = { onOpenSafFolder(folder) },
                                    )
                                }
                                items(state.folders, key = { "folder-${it.folderKey}" }) { folder ->
                                    LocationChip(
                                        icon = Icons.Filled.FolderOpen,
                                        title = folder.folderName,
                                        subtitle = stringResource(R.string.library_items_count, folder.itemCount),
                                        onClick = { onOpenFolder(folder) },
                                    )
                                }
                                if (state.networkSources.isNotEmpty()) {
                                    item(key = "network-locations") {
                                        LocationChip(
                                            icon = Icons.Filled.Cloud,
                                            title = stringResource(R.string.network_title),
                                            subtitle = stringResource(
                                                R.string.home_network_count,
                                                state.networkSources.size,
                                            ),
                                            onClick = onOpenNetwork,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "stats") {
                        LibraryStatsCard(
                            totalCount = state.totalCount,
                            totalSizeBytes = state.totalSizeBytes,
                            folderCount = state.folders.size + state.safFolders.size,
                            onOpenLibrary = { onOpenLibrary(TAB_LIBRARY) },
                            modifier = Modifier.staggeredEntrance(index = 17),
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun HeroBanner(
    item: MediaItem,
    isResume: Boolean,
    onPlay: () -> Unit,
    onRestart: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (item.playbackDurationMs > 0L) {
            (item.playbackPositionMs.toFloat() / item.playbackDurationMs).coerceIn(0f, 1f)
        } else {
            0f
        },
        animationSpec = tween(700),
        label = "heroProgress",
    )
    val favoriteScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val cardShape = RoundedCornerShape(26.dp)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        val heroHeight = minOf(maxWidth * 9f / 16f, 420.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .shadow(18.dp, cardShape)
                .clip(cardShape)
                .border(1.dp, Color.White.copy(alpha = 0.12f), cardShape),
        ) {
            MediaThumbnail(
                item = item,
                frameMillis = if (item.durationMs > 0L) {
                    (item.durationMs / 3).coerceIn(8_000L, 180_000L)
                } else {
                    8_000L
                },
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.32f),
                            0.25f to Color.Transparent,
                            0.55f to Color.Black.copy(alpha = 0.35f),
                            1f to Color.Black.copy(alpha = 0.92f),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.62f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(42.dp)
                    .graphicsLayer {
                        scaleX = favoriteScale.value
                        scaleY = favoriteScale.value
                    }
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    .clickable {
                        onToggleFavorite()
                        scope.launch {
                            favoriteScale.snapTo(0.82f)
                            favoriteScale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = 0.4f,
                                    stiffness = 700f,
                                ),
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (item.isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Filled.FavoriteBorder
                    },
                    contentDescription = null,
                    tint = if (item.isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.White
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isResume) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.White.copy(alpha = 0.9f)
                                },
                            ),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = buildString {
                            append(
                                stringResource(
                                    if (isResume) {
                                        R.string.library_continue_watching
                                    } else {
                                        R.string.home_now_playing
                                    },
                                ),
                            )
                            if (isResume && item.playbackDurationMs > item.playbackPositionMs) {
                                append(" · ")
                                append(
                                    stringResource(
                                        R.string.home_remaining,
                                        TimeUtils.formatDuration(
                                            item.playbackDurationMs - item.playbackPositionMs,
                                        ),
                                    ),
                                )
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    heroBadges(item).forEach { badge ->
                        GlassBadge(badge)
                    }
                    Text(
                        text = buildList {
                            item.folderName?.takeIf { it.isNotBlank() }?.let { add(it) }
                            if (item.sizeBytes > 0) add(FileSizeUtils.formatSize(item.sizeBytes))
                        }.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.68f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (isResume && progress > 0f) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.28f),
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onPlay,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isResume) {
                                stringResource(
                                    R.string.home_resume_button,
                                    TimeUtils.formatDuration(item.playbackPositionMs),
                                )
                            } else {
                                stringResource(R.string.common_play)
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    OutlinedButton(
                        onClick = onRestart,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.Filled.Replay, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.home_restart),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassBadge(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

@Composable
private fun QuickActionsRow(
    onOpenFile: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickAction(
                icon = Icons.Filled.FolderOpen,
                label = stringResource(R.string.library_open_file),
                onClick = onOpenFile,
                modifier = Modifier.weight(1f),
            )
            QuickDivider()
            QuickAction(
                icon = Icons.Filled.VideoLibrary,
                label = stringResource(R.string.library_tab_library),
                onClick = onOpenLibrary,
                modifier = Modifier.weight(1f),
            )
            QuickDivider()
            QuickAction(
                icon = Icons.Filled.Link,
                label = stringResource(R.string.network_title),
                onClick = onOpenNetwork,
                modifier = Modifier.weight(1f),
            )
            QuickDivider()
            QuickAction(
                icon = Icons.Filled.Settings,
                label = stringResource(R.string.settings_title),
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    )
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.pressScaleClickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                        ),
                    ),
                ),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (action != null && onAction != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .pressScaleClickable(onClick = onAction)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun MediaRowSection(
    items: List<MediaItem>,
    onPlay: (MediaItem) -> Unit,
    index: Int,
) {
    LazyRow(
        modifier = Modifier.staggeredEntrance(index),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.uri }) { item ->
            PosterCard(item = item, onClick = { onPlay(item) })
        }
    }
}

@Composable
private fun PosterCard(
    item: MediaItem,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val watched = item.playbackDurationMs > 0L &&
        item.playbackPositionMs.toFloat() / item.playbackDurationMs >= 0.95f
    Column(
        modifier = Modifier
            .width(196.dp)
            .pressScaleClickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .shadow(8.dp, shape)
                .clip(shape),
        ) {
            MediaThumbnail(item = item, modifier = Modifier.matchParentSize())
            if (watched) {
                Text(
                    text = stringResource(R.string.library_badge_watched),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            } else if (item.playbackPositionMs > 0L && item.playbackDurationMs > 0L) {
                LinearProgressIndicator(
                    progress = {
                        (item.playbackPositionMs.toFloat() / item.playbackDurationMs)
                            .coerceIn(0f, 1f)
                    },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                resolutionBadge(item)?.let { MiniBadge(it) }
                containerBadge(item)?.let { MiniBadge(it) }
            }
            if (item.durationMs > 0L) {
                Text(
                    text = TimeUtils.formatDuration(item.durationMs),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val subtitle = buildList {
            item.folderName?.takeIf { it.isNotBlank() }?.let { add(it) }
            if (item.sizeBytes > 0) add(FileSizeUtils.formatSize(item.sizeBytes))
        }.joinToString(" · ")
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MiniBadge(text: String) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

@Composable
private fun LocationChip(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(192.dp)
            .pressScaleClickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun LibraryStatsCard(
    totalCount: Int,
    totalSizeBytes: Long,
    folderCount: Int,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f),
                    ),
                ),
            ),
        ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.home_overview),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                StatColumn(
                    value = "$totalCount",
                    label = stringResource(R.string.home_stats_videos),
                )
                StatColumn(
                    value = FileSizeUtils.formatSize(totalSizeBytes),
                    label = stringResource(R.string.home_stats_size),
                )
                StatColumn(
                    value = "$folderCount",
                    label = stringResource(R.string.home_stats_folders),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .pressScaleClickable(onClick = onOpenLibrary)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_open_library),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PermissionCard(
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.library_permission_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.library_permission_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onRequest) {
                Text(stringResource(R.string.library_permission_grant))
            }
        }
    }
}

@Composable
private fun EmptyWorkspaceHint(
    onOpenFile: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.VideoLibrary,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(56.dp)
                .floatingIcon(),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.home_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenFile) {
            Text(stringResource(R.string.library_open_file))
        }
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(onClick = onOpenLibrary) {
            Text(stringResource(R.string.library_tab_library))
        }
    }
}

private fun hasMediaPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) ==
        PackageManager.PERMISSION_GRANTED


private fun resolutionBadge(item: MediaItem): String? = when {
    item.height >= 2000 -> "4K"
    item.height >= 1000 -> "1080P"
    item.height >= 700 -> "720P"
    item.height > 0 -> "${item.height}P"
    else -> null
}

private fun containerBadge(item: MediaItem): String? {
    val mime = item.mimeType.orEmpty()
    return when {
        mime.contains("matroska") -> "MKV"
        mime.contains("mp4") -> "MP4"
        mime.contains("webm") -> "WEBM"
        mime.contains("x-msvideo") || mime.contains("avi") -> "AVI"
        mime.contains("quicktime") -> "MOV"
        mime.contains("mpeg") -> "MPEG"
        else -> item.displayName
            ?.substringAfterLast('.', "")
            ?.uppercase()
            ?.takeIf { it.length in 2..4 }
    }
}

private fun heroBadges(item: MediaItem): List<String> = buildList {
    resolutionBadge(item)?.let { add(it) }
    containerBadge(item)?.let { add(it) }
    if (item.durationMs > 0) add(TimeUtils.formatDuration(item.durationMs))
}
