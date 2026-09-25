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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import verlintas.openvisum.navigation.Routes
import verlintas.openvisum.ui.home.HomeScreen
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
                val pendingUri by pendingMediaUri.collectAsStateWithLifecycle()

                LaunchedEffect(pendingUri) {
                    val uri = pendingUri ?: return@LaunchedEffect
                    navController.navigate(Routes.player(uri.toString()))
                    pendingMediaUri.update { null }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            onOpenMedia = { uri, title ->
                                navController.navigate(Routes.player(uri.toString(), title))
                            },
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
