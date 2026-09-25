package verlintas.openvisum

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import verlintas.openvisum.navigation.Routes
import verlintas.openvisum.ui.library.BrowseFolderScreen
import verlintas.openvisum.ui.library.FolderBrowseViewModel
import verlintas.openvisum.ui.library.LibraryScreen
import verlintas.openvisum.ui.library.LibraryViewModel
import verlintas.openvisum.ui.library.SafBrowserScreen
import verlintas.openvisum.ui.library.SafBrowserViewModel
import verlintas.openvisum.ui.player.PlayerScreen
import verlintas.openvisum.ui.theme.OpenVisumTheme

class MainActivity : ComponentActivity() {

    private val pendingMediaUri = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingMediaUri.value = intent?.data

        setContent {
            OpenVisumTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val app = context.applicationContext as OpenVisumApp
                val pendingUri by pendingMediaUri.collectAsStateWithLifecycle()

                LaunchedEffect(pendingUri) {
                    val uri = pendingUri ?: return@LaunchedEffect
                    navController.navigate(Routes.player(uri.toString()))
                    pendingMediaUri.update { null }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.LIBRARY,
                ) {
                    composable(Routes.LIBRARY) {
                        val libraryViewModel: LibraryViewModel = viewModel(
                            factory = LibraryViewModel.factory(
                                repository = app.container.mediaRepository,
                                preferences = app.container.preferencesRepository,
                            ),
                        )
                        LibraryScreen(
                            viewModel = libraryViewModel,
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
                        ),
                    ) { entry ->
                        val uri = entry.arguments?.getString("uri").orEmpty()
                        val title = entry.arguments?.getString("title").orEmpty()
                        PlayerScreen(
                            mediaUri = uri,
                            mediaTitle = title.ifBlank { null },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingMediaUri.value = intent.data
    }
}
