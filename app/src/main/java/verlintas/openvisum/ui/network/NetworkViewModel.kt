package verlintas.openvisum.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import verlintas.openvisum.core.data.NetworkRepository
import verlintas.openvisum.core.data.NetworkSource
import verlintas.openvisum.core.data.NetworkSourceType
import verlintas.openvisum.core.data.StreamHistoryItem

data class NetworkUiState(
    val sources: List<NetworkSource> = emptyList(),
    val history: List<StreamHistoryItem> = emptyList(),
    val isBusy: Boolean = false,
    val error: String? = null,
)

class NetworkViewModel(
    private val repository: NetworkRepository,
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NetworkUiState> = combine(
        repository.sources(),
        repository.streamHistory(),
        busy,
        error,
    ) { sources, history, isBusy, errorMessage ->
        NetworkUiState(sources, history, isBusy, errorMessage)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NetworkUiState(),
    )

    fun addSource(
        type: NetworkSourceType,
        name: String,
        host: String,
        port: Int,
        username: String?,
        password: String?,
        domain: String?,
        basePath: String?,
        useHttps: Boolean,
    ) {
        viewModelScope.launch {
            busy.value = true
            runCatching {
                repository.addSource(
                    type = type,
                    name = name,
                    host = host,
                    port = port,
                    username = username,
                    password = password,
                    domain = domain,
                    basePath = basePath,
                    useHttps = useHttps,
                )
            }.onFailure { error.value = it.message }
            busy.value = false
        }
    }

    fun deleteSource(id: Long) {
        viewModelScope.launch { repository.deleteSource(id) }
    }

    fun forgetStream(url: String) {
        viewModelScope.launch { repository.forgetStream(url) }
    }

    fun rememberStream(url: String, title: String?) {
        viewModelScope.launch { repository.rememberStream(url, title) }
    }

    fun clearError() {
        error.value = null
    }

    companion object {
        fun factory(repository: NetworkRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NetworkViewModel(repository) as T
                }
            }
    }
}
