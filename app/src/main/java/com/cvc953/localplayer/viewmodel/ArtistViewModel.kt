package com.cvc953.localplayer.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.ArtistController
import com.cvc953.localplayer.model.Artist
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.model.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ArtistViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val controller = ArtistController(getApplication())
    private val repository = SongRepository.getInstance(getApplication())

    val artists: StateFlow<List<Artist>> =
        repository.songs
            .map { controller.getAllArtists() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, controller.getAllArtists())

    private val _selectedArtist = MutableStateFlow<Artist?>(null)
    val selectedArtist: StateFlow<Artist?> = _selectedArtist
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _requestedArtist = MutableStateFlow<String?>(null)

    private val _artistSongs =
        combine(repository.songs, _requestedArtist) { library, artist ->
            if (artist == null) {
                emptyList()
            } else {
                library.filter { it.artist.equals(artist, ignoreCase = true) }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        repository.ensureLoadedAsync()
    }

    fun getSongsForArtist(artistName: String): StateFlow<List<Song>> {
        _requestedArtist.value = artistName
        return _artistSongs
    }

    fun selectArtist(artist: Artist) {
        _selectedArtist.value = artist
    }

    fun clearSelection() {
        _selectedArtist.value = null
    }

    // Persistent grid/list view preference
    private val prefs = application.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)
    private val PREF_VIEW_AS_GRID = "pref_view_as_grid_artist"

    fun isGridViewPreferred(): Boolean = prefs.getBoolean(PREF_VIEW_AS_GRID, true)

    fun setGridViewPreferred(value: Boolean) {
        prefs.edit().putBoolean(PREF_VIEW_AS_GRID, value).apply()
    }
}
