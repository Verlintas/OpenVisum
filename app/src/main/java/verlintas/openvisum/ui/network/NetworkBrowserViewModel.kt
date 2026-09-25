package verlintas.openvisum.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import verlintas.openvisum.core.data.NetworkPlaybackTarget
import verlintas.openvisum.core.data.NetworkRepository
import verlintas.openvisum.core.data.source.NetworkEntry

data class NetworkBrowserState(
    val sourceName: String = "",
    val currentPath: String = "",
    val entries: List<NetworkEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val canNavigateUp: Boolean = false,
)

class NetworkBrowserViewModel(
    private val repository: NetworkRepository,
    private val sourceId: Long,
    sourceName: String,
) : ViewModel() {

    private val stack = MutableStateFlow(listOf(""))
    private val entries = MutableStateFlow<List<NetworkEntry>>(emptyList())
    private val loading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<NetworkBrowserState> = combine(
        stack,
        entries,
        loading,
        error,
    ) { paths, entryList, isLoading, errorMessage ->
        NetworkBrowserState(
            sourceName = sourceName,
            currentPath = paths.last(),
            entries = entryList,
            isLoading = isLoading,
            error = errorMessage,
            canNavigateUp = paths.size > 1,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NetworkBrowserState(sourceName = sourceName),
    )

    init {
        load("")
    }

    fun open(entry: NetworkEntry) {
        if (!entry.isDirectory) return
        stack.update { it + entry.path }
        load(entry.path)
    }

    fun navigateUp() {
        val paths = stack.value
        if (paths.size <= 1) return
        val newStack = paths.dropLast(1)
        stack.value = newStack
        load(newStack.last())
    }

    suspend fun playbackTarget(entry: NetworkEntry): Result<NetworkPlaybackTarget> {
        val result = repository.playbackTarget(sourceId, entry)
        result.onSuccess { target ->
            repository.rememberStream(target.uri.toString(), target.title)
        }
        return result
    }

    fun clearError() {
        error.value = null
    }

    private fun load(path: String) {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            repository.browse(sourceId, path)
                .onSuccess { list -> entries.value = list }
                .onFailure { throwable ->
                    entries.value = emptyList()
                    error.value = throwable.message ?: "Failed to browse"
                }
            loading.value = false
        }
    }

    companion object {
        fun factory(
            repository: NetworkRepository,
            sourceId: Long,
            sourceName: String,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NetworkBrowserViewModel(repository, sourceId, sourceName) as T
            }
        }
    }
}
