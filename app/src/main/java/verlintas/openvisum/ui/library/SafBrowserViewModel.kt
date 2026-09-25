package verlintas.openvisum.ui.library

import android.net.Uri
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
import verlintas.openvisum.core.data.MediaRepository
import verlintas.openvisum.core.data.model.SafEntry

data class SafBrowserState(
    val currentName: String = "",
    val entries: List<SafEntry> = emptyList(),
    val isLoading: Boolean = false,
    val canNavigateUp: Boolean = false,
)

private data class FolderLevel(val uri: Uri, val name: String)

class SafBrowserViewModel(
    private val repository: MediaRepository,
    rootUri: Uri,
    rootName: String,
) : ViewModel() {

    private val stack = MutableStateFlow(listOf(FolderLevel(rootUri, rootName)))
    private val entries = MutableStateFlow<List<SafEntry>>(emptyList())
    private val loading = MutableStateFlow(false)

    val state: StateFlow<SafBrowserState> = combine(stack, entries, loading) { levels, entryList, isLoading ->
        SafBrowserState(
            currentName = levels.last().name,
            entries = entryList,
            isLoading = isLoading,
            canNavigateUp = levels.size > 1,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SafBrowserState(currentName = rootName),
    )

    init {
        load(rootUri)
    }

    fun open(entry: SafEntry) {
        if (!entry.isDirectory) return
        val level = FolderLevel(Uri.parse(entry.uri), entry.name)
        stack.update { it + level }
        load(level.uri)
    }

    fun navigateUp() {
        val levels = stack.value
        if (levels.size <= 1) return
        val newStack = levels.dropLast(1)
        stack.value = newStack
        load(newStack.last().uri)
    }

    private fun load(uri: Uri) {
        viewModelScope.launch {
            loading.value = true
            entries.value = runCatching { repository.safFolders.listChildren(uri) }
                .getOrDefault(emptyList())
            loading.value = false
        }
    }

    companion object {
        fun factory(
            repository: MediaRepository,
            rootUri: Uri,
            rootName: String,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SafBrowserViewModel(repository, rootUri, rootName) as T
            }
        }
    }
}
