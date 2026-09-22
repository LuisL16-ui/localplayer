package com.cvc953.localplayer.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.AlbumController
import com.cvc953.localplayer.model.Album
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.model.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AlbumViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val controller = AlbumController(getApplication())
    private val repository = SongRepository.getInstance(getApplication())

    private val _selectedAlbum = MutableStateFlow<Album?>(null)
    val selectedAlbum: StateFlow<Album?> = _selectedAlbum
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val isScanning: StateFlow<Boolean> = repository.isFirstScanning

    private val _requestedAlbum = MutableStateFlow<Pair<String, String>?>(null)

    val albums: StateFlow<List<Album>> =
        repository.songs
            .map { controller.getAllAlbums() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, controller.getAllAlbums())

    val songs: StateFlow<List<Song>> =
        combine(repository.songs, _requestedAlbum) { _, requested ->
            if (requested == null) {
                emptyList()
            } else {
                songsForAlbum(requested.first, requested.second)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Persistent grid/list view preference
    private val prefs = application.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)

    @Suppress("ktlint:standard:property-naming")
    private val PREF_VIEW_AS_GRID = "pref_view_as_grid"

    fun isGridViewPreferred(): Boolean = prefs.getBoolean(PREF_VIEW_AS_GRID, true)

    fun setGridViewPreferred(value: Boolean) {
        prefs.edit().putBoolean(PREF_VIEW_AS_GRID, value).apply()
    }

    fun selectAlbum(album: Album) {
        _selectedAlbum.value = album
        _requestedAlbum.value = album.name to album.artist
    }

    fun clearSelection() {
        _selectedAlbum.value = null
        _requestedAlbum.value = null
    }

    fun loadSongsForAlbumByName(
        albumName: String,
        artistName: String,
    ) {
        _requestedAlbum.value = albumName to artistName
    }

    private fun songsForAlbum(
        albumName: String,
        artistName: String,
    ): List<Song> {
        val normalizedRequestedArtists = normalizeArtistName(artistName).map { it.trim() }
        val matchingAlbum =
            controller.getAllAlbums().find { album ->
                val nameMatches = album.name.trim().equals(albumName.trim(), ignoreCase = true)
                val artistMatches =
                    normalizeArtistName(album.artist).any { artist ->
                        normalizedRequestedArtists.any { requestedArtist ->
                            artist.trim().equals(requestedArtist, ignoreCase = true)
                        }
                    }
                nameMatches && artistMatches
            }

        return if (matchingAlbum != null) controller.getSongsForAlbum(matchingAlbum) else emptyList()
    }

    private fun normalizeArtistName(artist: String): List<String> =
        if (artist.trim().equals("AC/DC", ignoreCase = true)) {
            listOf("AC/DC")
        } else {
            artist
                .trim()
                .split(',', '/')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
}
