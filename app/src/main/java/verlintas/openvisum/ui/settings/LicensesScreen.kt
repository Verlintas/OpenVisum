package verlintas.openvisum.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import verlintas.openvisum.R
import verlintas.openvisum.ui.components.SettingsGroup
import verlintas.openvisum.ui.components.SettingsGroupDivider
import verlintas.openvisum.ui.components.SettingsHintText
import verlintas.openvisum.ui.components.SettingsNavRow
import verlintas.openvisum.ui.components.staggeredEntrance

private data class LicenseEntry(
    val name: String,
    val license: String,
    val url: String,
)

private val LICENSES = listOf(
    LicenseEntry("libVLC / VLC", "LGPL-2.1-or-later", "https://www.videolan.org/vlc/libvlc.html"),
    LicenseEntry("FFmpeg (via libVLC)", "LGPL-2.1-or-later", "https://ffmpeg.org/"),
    LicenseEntry("jcifs-ng", "LGPL-2.1", "https://github.com/AgNO3/jcifs-ng"),
    LicenseEntry("jUPnP", "Apache-2.0 / CPL-1.0", "https://github.com/jupnp/jupnp"),
    LicenseEntry("AndroidX / Jetpack Compose", "Apache-2.0", "https://developer.android.com/jetpack"),
    LicenseEntry("Kotlin & kotlinx.coroutines", "Apache-2.0", "https://kotlinlang.org/"),
    LicenseEntry("kotlinx.serialization", "Apache-2.0", "https://github.com/Kotlin/kotlinx.serialization"),
    LicenseEntry("OkHttp", "Apache-2.0", "https://square.github.io/okhttp/"),
    LicenseEntry("Coil", "Apache-2.0", "https://coil-kt.github.io/coil/"),
    LicenseEntry("Room", "Apache-2.0", "https://developer.android.com/jetpack/androidx/releases/room"),
    LicenseEntry("DataStore", "Apache-2.0", "https://developer.android.com/topic/libraries/architecture/datastore"),
    LicenseEntry("Material Icons Extended", "Apache-2.0", "https://fonts.google.com/icons"),
    LicenseEntry("material-color-utilities", "Apache-2.0", "https://github.com/material-foundation/material-color-utilities"),
    LicenseEntry("OpenVisum", "GPL-3.0", "https://github.com/Verlintas/OpenVisum/blob/main/LICENSE"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.licenses_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            SettingsHintText(stringResource(R.string.licenses_intro))
            Spacer(Modifier.height(8.dp))
            LICENSES.forEachIndexed { index, entry ->
                if (index == 0) {
                    SettingsGroup(modifier = Modifier.staggeredEntrance(index = 0)) {
                        LicenseRow(entry, uriHandler::openUri)
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    SettingsGroup(modifier = Modifier.staggeredEntrance(index = index.coerceAtMost(8))) {
                        LicenseRow(entry, uriHandler::openUri)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            SettingsHintText(stringResource(R.string.licenses_footer))
            Spacer(Modifier.height(24.dp))
        }
        }
    }
}

@Composable
private fun LicenseRow(entry: LicenseEntry, onOpen: (String) -> Unit) {
    SettingsNavRow(
        icon = Icons.Filled.Description,
        title = entry.name,
        summary = entry.license,
        onClick = { onOpen(entry.url) },
    )
}
