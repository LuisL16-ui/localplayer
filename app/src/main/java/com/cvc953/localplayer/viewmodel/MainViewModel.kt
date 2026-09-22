
package com.cvc953.localplayer.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.Services.MusicService
import com.cvc953.localplayer.controller.PlayerController
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.model.SongRepository
import com.cvc953.localplayer.model.TtmlLyrics
import com.cvc953.localplayer.preferences.AppPrefs
import com.cvc953.localplayer.ui.PlayerState
import com.cvc953.localplayer.util.LrcLine
import com.cvc953.localplayer.util.TtmlParser
import com.cvc953.localplayer.util.parseLrc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    companion object {
        var instance: MainViewModel? = null
        private const val PREFS_NAME = "music_prefs"
        private const val LAST_SONG_URI = "last_song_uri"
        private const val LAST_SONG_TITLE = "last_song_title"
        private const val LAST_SONG_ARTIST = "last_song_artist"
        private const val LAST_IS_PLAYING = "last_is_playing"
        private const val PLAYLISTS_JSON = "playlists_json"
        private const val PREF_VIEW_AS_GRID = "pref_view_as_grid"
    }

    init {
        instance = this
    }

    private val appPrefs = AppPrefs(application)

    private val _isSettingsVisible = MutableStateFlow(false)
    val isSettingsVisible: StateFlow<Boolean> = _isSettingsVisible
    private val _isAboutVisible = MutableStateFlow(false)
    val isAboutVisible: StateFlow<Boolean> = _isAboutVisible

    fun openSettingsScreen() {
        _isSettingsVisible.value = true
    }

    fun closeSettingsScreen() {
        _isSettingsVisible.value = false
    }

    fun openAboutScreen() {
        _isAboutVisible.value = true
    }

    fun closeAboutScreen() {
        _isAboutVisible.value = false
    }

    private val songRepository = SongRepository.getInstance(application)
    private val prefs: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ViewModels especializados
    val lyricsViewModel = LyricsViewModel(application)
    val playbackViewModel = PlaybackViewModel(application)

    // Aquí puedes exponer solo el estado global mínimo necesario y delegar toda la lógica a los ViewModels anteriores.

    // Orden de visualización actual (usado para next/previous)
    private val _displayOrder = MutableStateFlow<List<Song>>(emptyList())
    val displayOrder: StateFlow<List<Song>> = _displayOrder

    // Estado del reproductor
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState

    init {
        // Keep MainViewModel's player state in sync with the centralized PlaybackViewModel
        viewModelScope.launch {
            playbackViewModel.playerState.collect { state ->
                _playerState.value = state
            }
        }
        // Ensure when the PlayerController's internal queue is exhausted, delegate to PlaybackViewModel
        try {
            val pc = PlayerController.getInstance(getApplication(), viewModelScope)

            pc.setOnQueueEndedListener {
                viewModelScope.launch {
                    try {
                        playbackViewModel.playNextSong()
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    // SYNC WITH refreshAllFromPrefs() — when adding a new preference, update both places
    private val _autoScanEnabled = MutableStateFlow(appPrefs.isAutoScanEnabled())
    val autoScanEnabled: StateFlow<Boolean> = _autoScanEnabled

    private val _themeMode = MutableStateFlow(appPrefs.getThemeMode())
    val themeMode: StateFlow<String> = _themeMode

    private val _language = MutableStateFlow(appPrefs.getLanguage())
    val language: StateFlow<String> = _language

    private val _languageChangeVersion = MutableStateFlow(0)
    val languageChangeVersion: StateFlow<Int> = _languageChangeVersion

    private val _dynamicColorEnabled = MutableStateFlow(appPrefs.isDynamicColorEnabled())
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled

    private val _primaryColorHex = MutableStateFlow(appPrefs.getPrimaryColor())
    val primaryColorHex: StateFlow<String> = _primaryColorHex

    private val _albumArtShape = MutableStateFlow(appPrefs.getAlbumArtShape())
    val albumArtShape: StateFlow<String> = _albumArtShape
    private val _progressBarStyle = MutableStateFlow(appPrefs.getProgressBarStyle())
    val progressBarStyle: StateFlow<String> = _progressBarStyle
    private val _transportStyle = MutableStateFlow(appPrefs.getTransportStyle())
    val transportStyle: StateFlow<String> = _transportStyle
    private val _playPauseStyle = MutableStateFlow(appPrefs.getPlayPauseStyle())
    val playPauseStyle: StateFlow<String> = _playPauseStyle
    private val _showAudioInfo = MutableStateFlow(appPrefs.getShowAudioInfo())
    val showAudioInfo: StateFlow<Boolean> = _showAudioInfo
    private val _songsTabEnabled = MutableStateFlow(appPrefs.isSongsTabEnabled())
    val songsTabEnabled: StateFlow<Boolean> = _songsTabEnabled
    private val _albumsTabEnabled = MutableStateFlow(appPrefs.isAlbumsTabEnabled())
    val albumsTabEnabled: StateFlow<Boolean> = _albumsTabEnabled
    private val _artistsTabEnabled = MutableStateFlow(appPrefs.isArtistsTabEnabled())
    val artistsTabEnabled: StateFlow<Boolean> = _artistsTabEnabled
    private val _playlistsTabEnabled = MutableStateFlow(appPrefs.isPlaylistsTabEnabled())
    val playlistsTabEnabled: StateFlow<Boolean> = _playlistsTabEnabled
    private val _genresTabEnabled = MutableStateFlow(appPrefs.isGenresTabEnabled())
    val genresTabEnabled: StateFlow<Boolean> = _genresTabEnabled
    private val _defaultStartTab = MutableStateFlow(appPrefs.getDefaultStartTab())
    val defaultStartTab: StateFlow<String> = _defaultStartTab
    private val _tabOrder = MutableStateFlow(appPrefs.getTabOrder())
    val tabOrder: StateFlow<List<String>> = _tabOrder

    private val _isPlayerScreenVisible = MutableStateFlow(false)
    val isPlayerScreenVisible: StateFlow<Boolean> = _isPlayerScreenVisible

    // Letras
    private val _showLyrics = MutableStateFlow(false)
    val showLyrics = _showLyrics.asStateFlow()
    private val _lyrics = MutableStateFlow<List<LrcLine>>(emptyList())
    val lyrics: StateFlow<List<LrcLine>> = _lyrics
    private val _ttmlLyrics = MutableStateFlow<TtmlLyrics?>(null)
    val ttmlLyrics: StateFlow<TtmlLyrics?> = _ttmlLyrics

    // Cola de reproducción
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue

    // Historial de reproducción para soportar "Anterior" respetando el orden
    private val playHistory: MutableList<Song> = mutableListOf()

    fun addToQueueEnd(song: Song) {
        val list = _queue.value.toMutableList()
        list.add(song)
        _queue.value = list
    }

    fun moveQueueItem(
        fromIndex: Int,
        toIndex: Int,
    ) {
        val list = _queue.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _queue.value = list
    }

    fun setUpcomingOrder(newOrder: List<Song>) {
        // Guardamos todo el orden de próximas canciones para que el drag funcione
        // tanto con cola manual como con el resto de la biblioteca.
        _queue.value = newOrder
    }

    fun playSong(
        song: Song,
        autoPlay: Boolean = true,
    ) {
        // Delegate playback to PlaybackViewModel to avoid duplicate MediaPlayer instances
        try {
            playbackViewModel.play(song)
            // Prefetch lyrics using centralized LyricsViewModel which supports caching
            try {
                // Prefetch TTML parsing in background and cache it; UI will load from cache
                lyricsViewModel.prefetchTtml(song)
            } catch (_: Exception) {
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error delegating playSong", e)
            // Delegate to playbackViewModel to play next song
            playbackViewModel.playNextSong()
        }
    }

    fun togglePlayPause() {
        // Delegate to playbackViewModel
        try {
            playbackViewModel.togglePlayPause()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "togglePlayPause delegate failed", e)
        }
    }

    fun toggleAutoScan(enabled: Boolean) {
        appPrefs.setAutoScanEnabled(enabled)
        _autoScanEnabled.value = enabled

        if (enabled) {
            songRepository.ensureLoadedAsync()
            songRepository.onAppForegrounded()
        }
    }

    fun setThemeMode(mode: String) {
        appPrefs.setThemeMode(mode)
        _themeMode.value = mode
    }

    fun setLanguage(language: String) {
        appPrefs.setLanguage(language)
        _language.value = language
        _languageChangeVersion.value += 1
    }

    fun toggleDynamicColor(enabled: Boolean) {
        appPrefs.setDynamicColorEnabled(enabled)
        _dynamicColorEnabled.value = enabled
    }

    fun setPrimaryColor(hex: String) {
        appPrefs.setPrimaryColor(hex)
        _primaryColorHex.value = hex
    }

    fun setAlbumArtShape(shape: String) {
        appPrefs.setAlbumArtShape(shape)
        _albumArtShape.value = shape
    }

    fun setProgressBarStyle(style: String) {
        appPrefs.setProgressBarStyle(style)
        _progressBarStyle.value = style
    }

    fun setTransportStyle(style: String) {
        appPrefs.setTransportStyle(style)
        _transportStyle.value = style
    }

    fun setPlayPauseStyle(style: String) {
        appPrefs.setPlayPauseStyle(style)
        _playPauseStyle.value = style
    }

    fun setShowAudioInfo(show: Boolean) {
        appPrefs.setShowAudioInfo(show)
        _showAudioInfo.value = show
    }

    fun setDefaultStartTab(tab: String) {
        appPrefs.setDefaultStartTab(tab)
        _defaultStartTab.value = appPrefs.getDefaultStartTab()
    }

    fun setSongsTabEnabled(enabled: Boolean) {
        appPrefs.setSongsTabEnabled(enabled)
        _songsTabEnabled.value = enabled
        if (!enabled && _defaultStartTab.value == "songs") {
            setDefaultStartTab("albums")
        }
    }

    fun setAlbumsTabEnabled(enabled: Boolean) {
        appPrefs.setAlbumsTabEnabled(enabled)
        _albumsTabEnabled.value = enabled
        if (!enabled && _defaultStartTab.value == "albums") {
            setDefaultStartTab("songs")
        }
    }

    fun setArtistsTabEnabled(enabled: Boolean) {
        appPrefs.setArtistsTabEnabled(enabled)
        _artistsTabEnabled.value = enabled
        if (!enabled && _defaultStartTab.value == "artists") {
            setDefaultStartTab("songs")
        }
    }

    fun setPlaylistsTabEnabled(enabled: Boolean) {
        appPrefs.setPlaylistsTabEnabled(enabled)
        _playlistsTabEnabled.value = enabled
        if (!enabled && _defaultStartTab.value == "playlists") {
            setDefaultStartTab("songs")
        }
    }

    fun setGenresTabEnabled(enabled: Boolean) {
        appPrefs.setGenresTabEnabled(enabled)
        _genresTabEnabled.value = enabled
        if (!enabled && _defaultStartTab.value == "genres") {
            setDefaultStartTab("songs")
        }
    }

    fun setTabOrder(order: List<String>) {
        appPrefs.setTabOrder(order)
        _tabOrder.value = appPrefs.getTabOrder()
    }

    fun moveTabUp(tabId: String) {
        val current = _tabOrder.value.toMutableList()
        val index = current.indexOf(tabId)
        if (index > 0) {
            val item = current.removeAt(index)
            current.add(index - 1, item)
            setTabOrder(current)
        }
    }

    fun moveTabDown(tabId: String) {
        val current = _tabOrder.value.toMutableList()
        val index = current.indexOf(tabId)
        if (index >= 0 && index < current.size - 1) {
            val item = current.removeAt(index)
            current.add(index + 1, item)
            setTabOrder(current)
        }
    }

    fun refreshAllFromPrefs() {
        // SYNC WITH init — when adding a new preference, update both places
        _autoScanEnabled.value = appPrefs.isAutoScanEnabled()
        _themeMode.value = appPrefs.getThemeMode()
        val newLanguage = appPrefs.getLanguage()
        if (newLanguage != _language.value) {
            _language.value = newLanguage
            _languageChangeVersion.value += 1
            // Apply locale imperatively so language takes effect immediately
            applyImportedLanguage(newLanguage)
        }
        _dynamicColorEnabled.value = appPrefs.isDynamicColorEnabled()
        _primaryColorHex.value = appPrefs.getPrimaryColor()
        _albumArtShape.value = appPrefs.getAlbumArtShape()
        _progressBarStyle.value = appPrefs.getProgressBarStyle()
        _transportStyle.value = appPrefs.getTransportStyle()
        _playPauseStyle.value = appPrefs.getPlayPauseStyle()
        _showAudioInfo.value = appPrefs.getShowAudioInfo()
        _songsTabEnabled.value = appPrefs.isSongsTabEnabled()
        _albumsTabEnabled.value = appPrefs.isAlbumsTabEnabled()
        _artistsTabEnabled.value = appPrefs.isArtistsTabEnabled()
        _playlistsTabEnabled.value = appPrefs.isPlaylistsTabEnabled()
        _genresTabEnabled.value = appPrefs.isGenresTabEnabled()
        _defaultStartTab.value = appPrefs.getDefaultStartTab()
        _tabOrder.value = appPrefs.getTabOrder()
    }

    private fun applyImportedLanguage(languageCode: String) {
        if (languageCode != "sistema") {
            val locale = when (languageCode) {
                "es" -> java.util.Locale("es")
                "en" -> java.util.Locale("en")
                "it" -> java.util.Locale("it")
                else -> java.util.Locale.getDefault()
            }
            val app = getApplication<android.app.Application>()
            val config = android.content.res.Configuration(app.resources.configuration)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                config.setLocale(locale)
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
            }
            app.resources.updateConfiguration(config, app.resources.displayMetrics)
        }
    }

    fun openPlayerScreen() {
        _isPlayerScreenVisible.value = true
    }

    fun closePlayerScreen() {
        _isPlayerScreenVisible.value = false
    }

    fun seekTo(position: Long) {
        playbackViewModel.seekTo(position)
    }

    fun toggleLyrics() {
        _showLyrics.value = !_showLyrics.value
    }

    fun updateDisplayOrder(orderedSongs: List<Song>) {
        _displayOrder.value = orderedSongs
    }

    fun loadLyricsForSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                // Obtener la ruta del archivo (desde song.filePath o desde ContentResolver)
                var audioFilePath = song.filePath

                if (audioFilePath.isNullOrEmpty()) {
                    // Si no tenemos filePath, obtenerlo del ContentResolver
                    val resolver = getApplication<Application>().contentResolver
                    val projection = arrayOf(MediaStore.Audio.Media.DATA)
                    resolver.query(song.uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                            if (dataCol >= 0) {
                                audioFilePath = cursor.getString(dataCol)
                            }
                        }
                    }
                }


                // Si tenemos la ruta del archivo, buscar .lrc en el mismo directorio
                if (!audioFilePath.isNullOrEmpty()) {
                    val audioFile = File(audioFilePath)
                    val audioDir = audioFile.parentFile
                    val audioNameWithoutExt = audioFile.nameWithoutExtension


                    if (audioDir != null && audioDir.exists()) {

                        // Primero intentar con TTML para letras palabra por palabra
                        val ttmlFile = File(audioDir, "$audioNameWithoutExt.ttml")

                        var ttmlParsedSuccessfully = false
                        if (ttmlFile.exists()) {
                            try {
                                val text = ttmlFile.readText()
                                val parsed = TtmlParser.parseTtml(text)

                                // Solo usar TTML si tiene líneas
                                if (parsed.lines.isNotEmpty()) {
                                    parsed.lines.forEachIndexed { i, line ->
                                    }
                                    _ttmlLyrics.value = parsed
                                    _lyrics.value = emptyList() // Limpiar letras LRC
                                    ttmlParsedSuccessfully = true
                                    return@launch
                                } else {
                                }
                            } catch (e: Exception) {
                            }
                        }

                        // Si no hay TTML o falló el parsing, intentar con LRC
                        val lrcFile = File(audioDir, "$audioNameWithoutExt.lrc")

                        if (lrcFile.exists()) {
                            try {
                                val text = lrcFile.readText()
                                _ttmlLyrics.value = null // Limpiar TTML
                                _lyrics.value = parseLrc(text)
                                return@launch
                            } catch (e: Exception) {
                            }
                        }

                        // Buscar cualquier archivo TTML o LRC en el directorio
                        val ttmlFiles =
                            audioDir.listFiles { _, name ->
                                name.endsWith(".ttml", ignoreCase = true)
                            }

                        // Intentar con TTML primero
                        var ttmlFoundAndParsed = false
                        ttmlFiles?.forEach { file ->
                            val ttmlNameWithoutExt = file.nameWithoutExtension

                            val audioClean =
                                audioNameWithoutExt
                                    .replace(Regex("[:\\\\/*?\"<>|]"), "")
                                    .lowercase()
                                    .trim()
                            val ttmlClean =
                                ttmlNameWithoutExt
                                    .replace(Regex("[:\\\\/*?\"<>|]"), "")
                                    .lowercase()
                                    .trim()

                            if (audioClean == ttmlClean) {
                                try {
                                    val text = file.readText()
                                    val parsed = TtmlParser.parseTtml(text)

                                    // Solo usar TTML si tiene líneas
                                    if (parsed.lines.isNotEmpty()) {
                                        _ttmlLyrics.value = parsed
                                        _lyrics.value = emptyList()
                                        ttmlFoundAndParsed = true
                                        return@launch
                                    } else {
                                    }
                                } catch (e: Exception) {
                                }
                            }
                        }

                        // Si no hay TTML o falló el parsing, buscar LRC
                        val lrcFiles =
                            audioDir.listFiles { _, name ->
                                name.endsWith(".lrc", ignoreCase = true)
                            }

                        lrcFiles?.forEach { file ->
                            val lrcNameWithoutExt = file.nameWithoutExtension

                            // Comparar ignorando caracteres problemáticos
                            val audioClean =
                                audioNameWithoutExt
                                    .replace(Regex("[:\\\\/*?\"<>|]"), "")
                                    .lowercase()
                                    .trim()
                            val lrcClean =
                                lrcNameWithoutExt
                                    .replace(Regex("[:\\\\/*?\"<>|]"), "")
                                    .lowercase()
                                    .trim()


                            if (audioClean == lrcClean) {
                                try {
                                    val text = file.readText()
                                    _ttmlLyrics.value = null
                                    _lyrics.value = parseLrc(text)
                                    return@launch
                                } catch (e: Exception) {
                                }
                            }
                        }

                    } else {
                    }
                }

                _ttmlLyrics.value = null
                _lyrics.value = emptyList()
            } catch (e: Exception) {
                _ttmlLyrics.value = null
                _lyrics.value = emptyList()
            }
        }
    }

    fun startService(
        context: Context,
        song: Song,
        isPlaying: Boolean = true,
    ) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, MusicService::class.java).apply {
                putExtra("SONG_URI", song.uri.toString())
                putExtra("TITLE", song.title)
                putExtra("ARTIST", song.artist)
                putExtra("IS_PLAYING", isPlaying)
            },
        )
        saveLastSong(song, isPlaying)
    }

    private fun saveLastSong(
        song: Song,
        isPlaying: Boolean,
    ) {
        prefs.edit().apply {
            putString(LAST_SONG_URI, song.uri.toString())
            putString(LAST_SONG_TITLE, song.title)
            putString(LAST_SONG_ARTIST, song.artist)
            putBoolean(LAST_IS_PLAYING, isPlaying)
            apply()
        }
    }
}
