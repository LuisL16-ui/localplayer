package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.cvc953.localplayer.model.SongRepository
import com.cvc953.localplayer.preferences.AppPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val appPrefs = AppPrefs(application)
    private val songRepository = SongRepository.getInstance(application)

    private val _isSettingsVisible = MutableStateFlow(false)
    val isSettingsVisible: StateFlow<Boolean> = _isSettingsVisible

    private val _isAboutVisible = MutableStateFlow(false)
    val isAboutVisible: StateFlow<Boolean> = _isAboutVisible

    private val _isPlayerScreenVisible = MutableStateFlow(false)
    val isPlayerScreenVisible: StateFlow<Boolean> = _isPlayerScreenVisible

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

    fun openPlayerScreen() {
        _isPlayerScreenVisible.value = true
    }

    fun closePlayerScreen() {
        _isPlayerScreenVisible.value = false
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
        _autoScanEnabled.value = appPrefs.isAutoScanEnabled()
        _themeMode.value = appPrefs.getThemeMode()
        val newLanguage = appPrefs.getLanguage()
        if (newLanguage != _language.value) {
            _language.value = newLanguage
            _languageChangeVersion.value += 1
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
                "es", "en", "it" -> java.util.Locale.forLanguageTag(languageCode)
                else -> java.util.Locale.getDefault()
            }
            val app = getApplication<Application>()
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
}
