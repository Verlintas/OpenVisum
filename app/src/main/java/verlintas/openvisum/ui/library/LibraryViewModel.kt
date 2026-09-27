/*
 * Copyright (C) 2026 Verlintas
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This file is part of OpenVisum.
 *
 * OpenVisum is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OpenVisum is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OpenVisum. If not, see <https://www.gnu.org/licenses/>.
 */

package verlintas.openvisum.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import verlintas.openvisum.core.data.FolderSummary
import verlintas.openvisum.core.data.MediaRepository
import verlintas.openvisum.core.data.db.SafFolderEntity
import verlintas.openvisum.core.data.model.MediaItem
import verlintas.openvisum.core.data.prefs.PreferencesRepository
import verlintas.openvisum.core.data.prefs.SortOrder

enum class LibraryFilter { ALL, FAVORITES }

data class LibraryUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val continueWatching: List<MediaItem> = emptyList(),
    val items: List<MediaItem> = emptyList(),
    val favorites: List<MediaItem> = emptyList(),
    val folders: List<FolderSummary> = emptyList(),
    val safFolders: List<SafFolderEntity> = emptyList(),
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val filter: LibraryFilter = LibraryFilter.ALL,
    val query: String = "",
)

class LibraryViewModel(
    private val repository: MediaRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(LibraryFilter.ALL)
    private val refreshing = MutableStateFlow(false)
    private val loaded = MutableStateFlow(false)

    private data class DataSet(
        val allItems: List<MediaItem>,
        val favorites: List<MediaItem>,
        val continueWatching: List<MediaItem>,
        val folders: List<FolderSummary>,
        val safFolders: List<SafFolderEntity>,
    )

    private data class UiControls(
        val query: String,
        val filter: LibraryFilter,
        val refreshing: Boolean,
        val loaded: Boolean,
    )

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.allItems,
        repository.favorites,
        repository.continueWatching,
        repository.folders,
        repository.safFolderList(),
    ) { allItems, favorites, continueWatching, folders, safFolders ->
        DataSet(allItems, favorites, continueWatching, folders, safFolders)
    }.combine(preferences.settings) { data, settings ->
        data to settings
    }.combine(combine(query, filter, refreshing, loaded) { query, filter, refreshing, loaded ->
        UiControls(query, filter, refreshing, loaded)
    }) { (data, settings), controls ->
        val source = if (controls.query.isBlank()) {
            data.allItems
        } else {
            data.allItems.filter {
                it.title.contains(controls.query, ignoreCase = true) ||
                    it.displayName.contains(controls.query, ignoreCase = true)
            }
        }
        val filtered = when (controls.filter) {
            LibraryFilter.ALL -> source
            LibraryFilter.FAVORITES -> source.filter { it.isFavorite }
        }
        LibraryUiState(
            isLoading = !controls.loaded,
            isRefreshing = controls.refreshing,
            continueWatching = data.continueWatching,
            items = filtered.sortedWith(settings.sortOrder.comparator()),
            favorites = data.favorites,
            folders = data.folders,
            safFolders = data.safFolders,
            sortOrder = settings.sortOrder,
            filter = controls.filter,
            query = controls.query,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(),
    )

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            runCatching { repository.refresh() }
            loaded.value = true
            refreshing.value = false
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: LibraryFilter) {
        filter.value = value
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { preferences.setSortOrder(order) }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch { repository.toggleFavorite(item.uri, !item.isFavorite) }
    }

    fun addSafFolder(uri: android.net.Uri) {
        viewModelScope.launch {
            repository.addSafFolder(uri)
        }
    }

    fun removeSafFolder(treeUri: String) {
        viewModelScope.launch { repository.removeSafFolder(treeUri) }
    }

    companion object {
        fun factory(
            repository: MediaRepository,
            preferences: PreferencesRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LibraryViewModel(repository, preferences) as T
            }
        }
    }
}

private fun SortOrder.comparator(): Comparator<MediaItem> = when (this) {
    SortOrder.DATE_DESC -> compareByDescending { it.dateAddedSeconds }
    SortOrder.NAME_ASC -> compareBy { it.title.lowercase() }
    SortOrder.SIZE_DESC -> compareByDescending { it.sizeBytes }
    SortOrder.DURATION_DESC -> compareByDescending { it.durationMs }
}
