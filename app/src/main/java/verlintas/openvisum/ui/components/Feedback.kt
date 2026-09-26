package verlintas.openvisum.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf

val LocalAppSnackbar = compositionLocalOf<SnackbarHostState?> { null }
