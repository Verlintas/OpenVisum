package verlintas.openvisum.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.platform.LocalContext
import verlintas.openvisum.R
import verlintas.openvisum.ui.components.SettingsGroup
import verlintas.openvisum.ui.components.SettingsGroupDivider
import verlintas.openvisum.ui.components.SettingsGroupLabel
import verlintas.openvisum.ui.components.SettingsHintText
import verlintas.openvisum.ui.components.SettingsNavRow
import verlintas.openvisum.ui.components.ShimmerRowPlaceholder
import verlintas.openvisum.ui.components.staggeredEntrance

private const val REPO_URL = "https://github.com/Verlintas/OpenVisum"
private const val RELEASES_URL = "$REPO_URL/releases"
private const val ISSUES_URL = "$REPO_URL/issues"
private const val CHANGELOG_URL = "$REPO_URL/blob/main/CHANGELOG.md"
private const val LICENSE_URL = "$REPO_URL/blob/main/LICENSE"

@Composable
fun AboutSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    var remoteInfo by remember { mutableStateOf<AboutRemoteInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var retryTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryTick) {
        isLoading = true
        remoteInfo = GitHubAboutSource.fetch()
        isLoading = false
    }

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.settings_section_about),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            AppHeaderCard(
                versionName = state.versionName,
                modifier = Modifier.staggeredEntrance(index = 0),
            )

            SettingsGroupLabel(stringResource(R.string.about_section_links))
            SettingsGroup(modifier = Modifier.staggeredEntrance(index = 1)) {
                SettingsNavRow(
                    icon = Icons.Filled.Code,
                    title = stringResource(R.string.about_link_repo),
                    summary = "Verlintas/OpenVisum",
                    onClick = { uriHandler.openUri(REPO_URL) },
                )
                SettingsGroupDivider()
                SettingsNavRow(
                    icon = Icons.Filled.NewReleases,
                    title = stringResource(R.string.about_link_releases),
                    summary = remoteInfo?.release?.tagName
                        ?: stringResource(R.string.about_link_releases_summary),
                    onClick = { uriHandler.openUri(RELEASES_URL) },
                )
                SettingsGroupDivider()
                SettingsNavRow(
                    icon = Icons.Filled.BugReport,
                    title = stringResource(R.string.about_link_issues),
                    summary = stringResource(R.string.about_link_issues_summary),
                    onClick = { uriHandler.openUri(ISSUES_URL) },
                )
                SettingsGroupDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Description,
                    title = stringResource(R.string.about_link_changelog),
                    summary = stringResource(R.string.about_link_changelog_summary),
                    onClick = { uriHandler.openUri(CHANGELOG_URL) },
                )
                SettingsGroupDivider()
                SettingsNavRow(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    title = stringResource(R.string.about_link_license),
                    summary = "GPL-3.0",
                    onClick = { uriHandler.openUri(LICENSE_URL) },
                )
                SettingsGroupDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Code,
                    title = stringResource(R.string.about_open_source_licenses),
                    summary = stringResource(R.string.about_open_source_licenses_summary),
                    onClick = onOpenLicenses,
                )
            }

            SettingsGroupLabel(stringResource(R.string.about_section_developer))
            when {
                isLoading -> SettingsGroup(modifier = Modifier.staggeredEntrance(index = 2)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ShimmerRowPlaceholder()
                    }
                }

                remoteInfo?.user == null && remoteInfo?.repo == null -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.about_load_failed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { retryTick++ }) {
                                Text(stringResource(R.string.about_retry))
                            }
                        }
                    }
                }

                else -> {
                    DeveloperCard(
                        user = remoteInfo?.user,
                        modifier = Modifier.staggeredEntrance(index = 2),
                    )
                    Spacer(Modifier.height(12.dp))
                    RepositoryCard(
                        repo = remoteInfo?.repo,
                        release = remoteInfo?.release,
                        isLoading = false,
                        modifier = Modifier.staggeredEntrance(index = 3),
                    )
                }
            }

            SettingsHintText(
                text = stringResource(R.string.about_license_note),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AppHeaderCard(
    versionName: String,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "aboutHeader")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "headerShift",
    )
    val colors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.primaryContainer,
    )
    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(1200f * shift, 0f),
        end = Offset(0f, 600f + 400f * shift),
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(24.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.about_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_version, versionName),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun DeveloperCard(
    user: GitHubUser?,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "avatarBreath")
    val breath by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "avatarScale",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = breath
                        scaleY = breath
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                val avatarUrl = user?.avatarUrl
                if (avatarUrl != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.size(64.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.size(64.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = (user?.name ?: user?.login ?: "V")
                                        .firstOrNull()?.uppercase() ?: "V",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        },
                        modifier = Modifier.size(64.dp),
                    )
                } else {
                    Text(
                        text = (user?.name ?: user?.login ?: "V")
                            .firstOrNull()?.uppercase() ?: "V",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.name ?: user?.login ?: "Verlintas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                user?.bio?.let { bio ->
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AnimatedStat(
                        value = user?.publicRepos ?: 0,
                        label = stringResource(R.string.about_stats_repos),
                    )
                    AnimatedStat(
                        value = user?.followers ?: 0,
                        label = stringResource(R.string.about_stats_followers),
                    )
                }
            }
        }
    }
}

@Composable
private fun RepositoryCard(
    repo: GitHubRepo?,
    release: GitHubRelease?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                return@Column
            }
            Text(
                text = repo?.fullName ?: "Verlintas/OpenVisum",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            repo?.description?.let { description ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AnimatedStat(value = repo?.stars ?: 0, label = "Stars")
                AnimatedStat(value = repo?.forks ?: 0, label = "Forks")
                AnimatedStat(value = repo?.openIssues ?: 0, label = "Issues")
            }
            release?.let {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(
                        R.string.about_latest_release,
                        it.tagName ?: it.name.orEmpty(),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                it.publishedAt?.let { published ->
                    Text(
                        text = stringResource(R.string.about_release_published, published.take(10)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedStat(value: Int, label: String) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "stat$label",
    )
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = "$animated",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
