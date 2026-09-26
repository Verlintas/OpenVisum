package verlintas.openvisum

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.currentBackStackEntryAsState
import verlintas.openvisum.ui.navigation.AppNavigationRail
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import verlintas.openvisum.core.data.prefs.AppSettings
import verlintas.openvisum.navigation.Routes
import verlintas.openvisum.ui.library.BrowseFolderScreen
import verlintas.openvisum.ui.library.FolderBrowseViewModel
import verlintas.openvisum.ui.library.LibraryScreen
import verlintas.openvisum.ui.library.LibraryViewModel
import verlintas.openvisum.ui.library.SafBrowserScreen
import verlintas.openvisum.ui.library.SafBrowserViewModel
import verlintas.openvisum.ui.home.HomeScreen
import verlintas.openvisum.ui.home.HomeViewModel
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
import verlintas.openvisum.ui.theme.OpenVisumTheme

class MainActivity : ComponentActivity() {

    private val pendingMediaUri = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingMediaUri.value = intent?.data

        setContent {
            val app = LocalContext.current.applicationContext as OpenVisumApp
            val appSettings by app.container.preferencesRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            OpenVisumTheme(
                themeMode = appSettings.themeMode,
                themeColorId = appSettings.themeColor,
            ) {
                val navController = rememberNavController()
                val context = LocalContext.current
                val pendingUri by pendingMediaUri.collectAsStateWithLifecycle()

                LaunchedEffect(pendingUri) {
                    val uri = pendingUri ?: return@LaunchedEffect
                    navController.navigate(Routes.player(uri.toString()))
                    pendingMediaUri.update { null }
                }

                val appSnackbar = remember { androidx.compose.material3.SnackbarHostState() }
                androidx.compose.runtime.CompositionLocalProvider(
                    verlintas.openvisum.ui.components.LocalAppSnackbar provides appSnackbar,
                ) {
                Box(modifier = Modifier.fillMaxSize()) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val wideLayout = maxWidth >= 840.dp
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route
                    val selectedRailIndex = when (currentRoute) {
                        Routes.HOME -> 0
                        Routes.LIBRARY_PATTERN, Routes.LIBRARY -> 1
                        Routes.NETWORK -> 2
                        Routes.SETTINGS -> 3
                        else -> -1
                    }
                    if (wideLayout) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            AppNavigationRail(
                                selectedIndex = selectedRailIndex,
                                onNavigate = { _, route ->
                                    navController.navigate(route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onOpenWebsite = {
                                    runCatching {
                                        context.startActivity(
                                            android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse("https://verlintas.github.io/OpenVisum/"),
                                            ),
                                        )
                                    }
                                },
                                versionName = BuildConfig.VERSION_NAME,
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                AppNavGraph(navController = navController, app = app)
                            }
                        }
                    } else {
                        AppNavGraph(navController = navController, app = app)
                    }
                }
                androidx.compose.material3.SnackbarHost(
                    hostState = appSnackbar,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
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

@Composable
private fun settingsViewModel(app: OpenVisumApp): SettingsViewModel = viewModel(
    factory = SettingsViewModel.factory(
        context = LocalContext.current.applicationContext,
        preferences = app.container.preferencesRepository,
        versionName = BuildConfig.VERSION_NAME,
    ),
)
