package verlintas.openvisum.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import verlintas.openvisum.core.data.prefs.AppSettings

@Composable
fun OpenVisumTheme(
    themeMode: Int = AppSettings.THEME_SYSTEM,
    themeColorId: String = ThemeColor.BRAND.id,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        AppSettings.THEME_LIGHT -> false
        AppSettings.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }
    val themeColor = ThemeColor.fromId(themeColorId)
    val colorScheme = when {
        themeColor == ThemeColor.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> themeColor.darkScheme()
        else -> themeColor.lightScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OpenVisumTypography,
        shapes = OpenVisumShapes,
        content = content,
    )
}
