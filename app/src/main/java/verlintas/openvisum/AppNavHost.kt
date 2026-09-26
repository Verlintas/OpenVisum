package verlintas.openvisum

import android.net.Uri

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import verlintas.openvisum.navigation.Routes
import verlintas.openvisum.ui.home.HomeScreen
import verlintas.openvisum.ui.home.HomeViewModel
import verlintas.openvisum.ui.library.BrowseFolderScreen
import verlintas.openvisum.ui.library.FolderBrowseViewModel
import verlintas.openvisum.ui.library.LibraryScreen
import verlintas.openvisum.ui.library.LibraryViewModel
import verlintas.openvisum.ui.library.SafBrowserScreen
import verlintas.openvisum.ui.library.SafBrowserViewModel
import verlintas.openvisum.ui.network.NetworkBrowserScreen
import verlintas.openvisum.ui.network.NetworkBrowserViewModel
import verlintas.openvisum.ui.network.NetworkScreen
import verlintas.openvisum.ui.network.NetworkViewModel
import verlintas.openvisum.ui.player.PlayerScreen
import verlintas.openvisum.ui.settings.AboutSettingsScreen
import verlintas.openvisum.ui.settings.AppearanceSettingsScreen
import verlintas.openvisum.ui.settings.ChangelogScreen
import verlintas.openvisum.ui.settings.FeedbackScreen
import verlintas.openvisum.ui.settings.LicensesScreen
import verlintas.openvisum.ui.settings.OnlineSubtitleSettingsScreen
import verlintas.openvisum.ui.settings.PlaybackSettingsScreen
import verlintas.openvisum.ui.settings.SettingsScreen
import verlintas.openvisum.ui.settings.SettingsViewModel
import verlintas.openvisum.ui.settings.SubtitleSettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    app: OpenVisumApp,
) {
    val context = LocalContext.current
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    enterTransition = {
                        slideInHorizontally(
                            animationSpec = tween(340, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> fullWidth },
                        )
                    },
                    exitTransition = { androidx.compose.animation.ExitTransition.None },
                    popEnterTransition = { androidx.compose.animation.EnterTransition.None },
                    popExitTransition = {
                        slideOutHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            targetOffsetX = { fullWidth -> fullWidth },
                        )
                    },
                ) {
                    composable(Routes.HOME) {
                        val homeViewModel: HomeViewModel = viewModel(
                            factory = HomeViewModel.factory(
                                mediaRepository = app.container.mediaRepository,
                                networkRepository = app.container.networkRepository,
                            ),
                        )
                        HomeScreen(
                            viewModel = homeViewModel,
                            onPlayUri = { uri, title, restart ->
                                navController.navigate(Routes.player(uri, title, restart))
                            },
                            onOpenLibrary = { tab ->
                                navController.navigate(Routes.library(tab = tab))
                            },
                            onOpenSearch = {
                                navController.navigate(Routes.library(search = true))
                            },
                            onOpenFolder = { folder ->
                                navController.navigate(
                                    Routes.browseFolder(folder.folderKey, folder.folderName),
                                )
                            },
                            onOpenSafFolder = { folder ->
                                navController.navigate(Routes.safBrowser(folder.treeUri, folder.name))
                            },
                            onOpenNetwork = { navController.navigate(Routes.NETWORK) },
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            onOpenWebsite = {
                                runCatching {
                                    context.startActivity(
                                        android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(
                                                "https://verlintas.github.io/OpenVisum/",
                                            ),
                                        ),
                                    )
                                }
                            },
                        )
                    }

                    composable(
                        route = Routes.LIBRARY_PATTERN,
                        arguments = listOf(
                            navArgument("tab") {
                                type = NavType.IntType
                                defaultValue = 0
                            },
                            navArgument("search") {
                                type = NavType.BoolType
                                defaultValue = false
                            },
                        ),
                    ) { entry ->
                        val initialTab = entry.arguments?.getInt("tab") ?: 0
                        val startSearch = entry.arguments?.getBoolean("search") ?: false
                        val libraryViewModel: LibraryViewModel = viewModel(
                            factory = LibraryViewModel.factory(
                                repository = app.container.mediaRepository,
                                preferences = app.container.preferencesRepository,
                            ),
                        )
                        LibraryScreen(
                            viewModel = libraryViewModel,
                            initialTab = initialTab,
                            startSearch = startSearch,
                            onPlayUri = { uri, title ->
                                navController.navigate(Routes.player(uri, title))
                            },
                            onOpenFolder = { folder ->
                                navController.navigate(
                                    Routes.browseFolder(folder.folderKey, folder.folderName),
                                )
                            },
                            onOpenSafFolder = { folder ->
                                navController.navigate(Routes.safBrowser(folder.treeUri, folder.name))
                            },
                            onOpenNetwork = { navController.navigate(Routes.NETWORK) },
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        )
                    }

                    composable(Routes.SETTINGS) {
                        val settingsViewModel: SettingsViewModel = viewModel(
                            factory = SettingsViewModel.factory(
                                context = context.applicationContext,
                                preferences = app.container.preferencesRepository,
                                versionName = BuildConfig.VERSION_NAME,
                            ),
                        )
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenPlayback = { navController.navigate(Routes.SETTINGS_PLAYBACK) },
                            onOpenSubtitles = { navController.navigate(Routes.SETTINGS_SUBTITLES) },
                            onOpenOnline = { navController.navigate(Routes.SETTINGS_ONLINE) },
                            onOpenAppearance = { navController.navigate(Routes.SETTINGS_APPEARANCE) },
                            onOpenAbout = { navController.navigate(Routes.SETTINGS_ABOUT) },
                        )
                    }

                    composable(Routes.SETTINGS_PLAYBACK) {
                        PlaybackSettingsScreen(
                            viewModel = settingsViewModel(app),
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.SETTINGS_SUBTITLES) {
                        SubtitleSettingsScreen(
                            viewModel = settingsViewModel(app),
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.SETTINGS_ONLINE) {
                        OnlineSubtitleSettingsScreen(
                            viewModel = settingsViewModel(app),
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.SETTINGS_APPEARANCE) {
                        AppearanceSettingsScreen(
                            viewModel = settingsViewModel(app),
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.SETTINGS_ABOUT) {
                        AboutSettingsScreen(
                            viewModel = settingsViewModel(app),
                            onBack = { navController.popBackStack() },
                            onOpenLicenses = { navController.navigate(Routes.SETTINGS_LICENSES) },
                            onOpenChangelog = { navController.navigate(Routes.SETTINGS_CHANGELOG) },
                            onOpenFeedback = { navController.navigate(Routes.SETTINGS_FEEDBACK) },
                        )
                    }

                    composable(Routes.SETTINGS_LICENSES) {
                        LicensesScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.SETTINGS_CHANGELOG) {
                        ChangelogScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.SETTINGS_FEEDBACK) {
                        FeedbackScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.NETWORK) {
                        val networkViewModel: NetworkViewModel = viewModel(
                            factory = NetworkViewModel.factory(app.container.networkRepository),
                        )
                        NetworkScreen(
                            viewModel = networkViewModel,
                            onPlayUri = { uri, title ->
                                navController.navigate(Routes.player(uri, title))
                            },
                            onBrowseSource = { source ->
                                navController.navigate(
                                    Routes.networkBrowser(source.id, source.name),
                                )
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(
                        route = Routes.NETWORK_BROWSER_PATTERN,
                        arguments = listOf(
                            navArgument("sourceId") { type = NavType.LongType },
                            navArgument("name") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                        ),
                    ) { entry ->
                        val sourceId = entry.arguments?.getLong("sourceId") ?: 0L
                        val name = entry.arguments?.getString("name").orEmpty()
                        val browserViewModel: NetworkBrowserViewModel = viewModel(
                            factory = NetworkBrowserViewModel.factory(
                                repository = app.container.networkRepository,
                                sourceId = sourceId,
                                sourceName = name,
                            ),
                        )
                        NetworkBrowserScreen(
                            viewModel = browserViewModel,
                            onPlayUri = { uri, title ->
                                navController.navigate(Routes.player(uri, title))
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(
                        route = Routes.BROWSE_FOLDER_PATTERN,
                        arguments = listOf(
                            navArgument("folderKey") { type = NavType.StringType },
                            navArgument("name") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                        ),
                    ) { entry ->
                        val folderKey = entry.arguments?.getString("folderKey").orEmpty()
                        val name = entry.arguments?.getString("name").orEmpty()
                        val browseViewModel: FolderBrowseViewModel = viewModel(
                            factory = FolderBrowseViewModel.factory(
                                repository = app.container.mediaRepository,
                                folderKey = folderKey,
                            ),
                        )
                        BrowseFolderScreen(
                            folderName = name,
                            viewModel = browseViewModel,
                            onPlayUri = { uri, title ->
                                navController.navigate(Routes.player(uri, title))
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(
                        route = Routes.SAF_BROWSER_PATTERN,
                        arguments = listOf(
                            navArgument("uri") { type = NavType.StringType },
                            navArgument("name") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                        ),
                    ) { entry ->
                        val treeUri = Uri.parse(entry.arguments?.getString("uri").orEmpty())
                        val name = entry.arguments?.getString("name").orEmpty()
                        val safViewModel: SafBrowserViewModel = viewModel(
                            factory = SafBrowserViewModel.factory(
                                repository = app.container.mediaRepository,
                                rootUri = treeUri,
                                rootName = name,
                            ),
                        )
                        SafBrowserScreen(
                            viewModel = safViewModel,
                            onPlayUri = { uri, title ->
                                navController.navigate(Routes.player(uri, title))
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(
                        route = Routes.PLAYER_PATTERN,
                        arguments = listOf(
                            navArgument("uri") { type = NavType.StringType },
                            navArgument("title") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                            navArgument("restart") {
                                type = NavType.BoolType
                                defaultValue = false
                            },
                        ),
                        enterTransition = { fadeIn(tween(250)) + scaleIn(tween(300), initialScale = 0.94f) },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition = { fadeOut(tween(200)) + scaleOut(tween(220), targetScale = 0.96f) },
                    ) { entry ->
                        val uri = entry.arguments?.getString("uri").orEmpty()
                        val title = entry.arguments?.getString("title").orEmpty()
                        val restart = entry.arguments?.getBoolean("restart") ?: false
                        PlayerScreen(
                            mediaUri = uri,
                            mediaTitle = title.ifBlank { null },
                            restart = restart,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }}

@Composable
private fun settingsViewModel(app: OpenVisumApp): SettingsViewModel = viewModel(
    factory = SettingsViewModel.factory(
        context = LocalContext.current.applicationContext,
        preferences = app.container.preferencesRepository,
        versionName = BuildConfig.VERSION_NAME,
    ),
)
