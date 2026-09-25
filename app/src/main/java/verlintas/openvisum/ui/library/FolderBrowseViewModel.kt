package verlintas.openvisum.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import verlintas.openvisum.core.data.MediaRepository
import verlintas.openvisum.core.data.model.MediaItem

class FolderBrowseViewModel(
    private val repository: MediaRepository,
    folderKey: String,
) : ViewModel() {

    val items: StateFlow<List<MediaItem>> = repository.itemsInFolder(folderKey)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch { repository.toggleFavorite(item.uri, !item.isFavorite) }
    }

    companion object {
        fun factory(repository: MediaRepository, folderKey: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FolderBrowseViewModel(repository, folderKey) as T
                }
            }
    }
}
