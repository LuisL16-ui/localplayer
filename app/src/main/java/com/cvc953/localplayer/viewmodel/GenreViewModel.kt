package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.GenreController
import com.cvc953.localplayer.model.Genre
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.model.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class GenreViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val controller = GenreController(getApplication())
    private val repository = SongRepository.getInstance(getApplication())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _searchQuery = MutableStateFlow("")
    private val _requestedGenre = MutableStateFlow<Genre?>(null)

    val genres: StateFlow<List<Genre>> =
        repository.songs
            .map { controller.getAllGenres() }
            .combine(_searchQuery) { genres, query ->
                if (query.isBlank()) genres else genres.filter { it.name.contains(query, ignoreCase = true) }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, controller.getAllGenres())

    val songs: StateFlow<List<Song>> =
        combine(repository.songs, _requestedGenre) { _, genre ->
            if (genre == null) emptyList() else controller.getSongsForGenre(genre)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        repository.ensureLoadedAsync()
    }

    fun searchGenres(query: String) {
        _searchQuery.value = query
    }

    fun loadSongsForGenre(genre: Genre) {
        _requestedGenre.value = genre
    }
}
