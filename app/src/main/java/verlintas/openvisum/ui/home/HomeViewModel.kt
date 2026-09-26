package verlintas.openvisum.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import verlintas.openvisum.core.data.FolderSummary
import verlintas.openvisum.core.data.MediaRepository
import verlintas.openvisum.core.data.NetworkRepository
import verlintas.openvisum.core.data.NetworkSource
import verlintas.openvisum.core.data.db.SafFolderEntity
import verlintas.openvisum.core.data.model.MediaItem

data class FolderSection(
    val folderKey: String,
    val folderName: String,
    val items: List<MediaItem>,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val continueWatching: List<MediaItem> = emptyList(),
    val recent: List<MediaItem> = emptyList(),
    val favorites: List<MediaItem> = emptyList(),
    val folderSections: List<FolderSection> = emptyList(),
    val folders: List<FolderSummary> = emptyList(),
    val safFolders: List<SafFolderEntity> = emptyList(),
    val networkSources: List<NetworkSource> = emptyList(),
    val heroItem: MediaItem? = null,
    val heroIsResume: Boolean = false,
    val totalCount: Int = 0,
    val totalSizeBytes: Long = 0,
) {
    val hasAnyContent: Boolean
        get() = continueWatching.isNotEmpty() || recent.isNotEmpty() || favorites.isNotEmpty()
}

class HomeViewModel(
    private val mediaRepository: MediaRepository,
    private val networkRepository: NetworkRepository,
) : ViewModel() {

    private data class MediaData(
        val continueWatching: List<MediaItem>,
        val recent: List<MediaItem>,
        val favorites: List<MediaItem>,
        val all: List<MediaItem>,
        val folders: List<FolderSummary>,
        val safFolders: List<SafFolderEntity>,
    )

    val uiState: StateFlow<HomeUiState> = combine(
        mediaRepository.continueWatching,
        mediaRepository.recent,
        mediaRepository.favorites,
        mediaRepository.allItems,
    ) { continueWatching, recent, favorites, all ->
        MediaData(
            continueWatching = continueWatching,
            recent = recent,
            favorites = favorites,
            all = all,
            folders = emptyList(),
            safFolders = emptyList(),
        )
    }.combine(
        combine(mediaRepository.folders, mediaRepository.safFolderList()) { folders, saf ->
            folders to saf
        },
    ) { data, folderData ->
        data.copy(folders = folderData.first, safFolders = folderData.second)
    }.combine(networkRepository.sources()) { data, sources ->
        val hero = data.continueWatching.firstOrNull()
            ?: data.recent.firstOrNull()
            ?: data.favorites.firstOrNull()
            ?: data.all.firstOrNull()
        val folderSections = data.all
            .filter { !it.folderKey.isNullOrBlank() }
            .groupBy { it.folderKey!! }
            .map { (key, items) ->
                FolderSection(
                    folderKey = key,
                    folderName = items.firstOrNull()?.folderName ?: key,
                    items = items.sortedByDescending { it.dateAddedSeconds }.take(20),
                )
            }
            .filter { it.items.size >= 2 }
            .sortedByDescending { it.items.size }
            .take(3)
        HomeUiState(
            isLoading = false,
            continueWatching = data.continueWatching,
            recent = data.recent,
            favorites = data.favorites,
            folderSections = folderSections,
            folders = data.folders,
            safFolders = data.safFolders,
            networkSources = sources,
            heroItem = hero,
            heroIsResume = data.continueWatching.isNotEmpty(),
            totalCount = data.all.size,
            totalSizeBytes = data.all.sumOf { it.sizeBytes },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun refresh() {
        viewModelScope.launch {
            runCatching { mediaRepository.refresh() }
        }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            mediaRepository.toggleFavorite(item.uri, !item.isFavorite)
        }
    }

    companion object {
        fun factory(
            mediaRepository: MediaRepository,
            networkRepository: NetworkRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(mediaRepository, networkRepository) as T
            }
        }
    }
}
