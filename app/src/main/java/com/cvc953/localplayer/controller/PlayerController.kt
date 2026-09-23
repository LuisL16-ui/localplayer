package com.cvc953.localplayer.controller

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.preferences.AppPrefs
import com.cvc953.localplayer.ui.PlayerState
import com.cvc953.localplayer.ui.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerController(
    private val context: Context,
    private val scope: CoroutineScope? = null,
) : AudioManager.OnAudioFocusChangeListener {
    private var mediaPlayer: MediaPlayer? = null
    private val queue = mutableListOf<Song>()
    private var currentIndex = -1

    // Audio focus
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var pausedByAudioFocus: Boolean = false
    private var duckedByAudioFocus: Boolean = false

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var onQueueEnded: (() -> Unit)? = null
    private var onAudioSessionIdChanged: ((Int) -> Unit)? = null
    private var onNextAtEnd: (() -> Unit)? = null

    private var pendingSeek: Long? = null
    private var pendingResume: Boolean = false
    private var repeatMode: RepeatMode = RepeatMode.NONE
    private var playbackFadeJob: Job? = null
    private var fixedAudioSessionId: Int = 0
    private val appPrefs by lazy { AppPrefs(context.applicationContext) }
    private val internalScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val timerScope = internalScope
    private val fallbackEqualizerController by lazy {
        EqualizerController(context.applicationContext as Application)
    }

    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMs: StateFlow<Long?> = _sleepTimerRemainingMs

    private val fadeInDurationMs = 140L
    private val fadeInSteps = 7

    init {
        restoreSleepTimer()
    }

    fun playNow(
        songs: List<Song>,
        startIndex: Int = 0,
        startPaused: Boolean = false,
        seekToPosition: Long? = null,
        resumeAfterSeek: Boolean = false,
    ) {
        val isSameQueue = queue.size == songs.size && queue.zip(songs).all { it.first.id == it.second.id }
        val isSameIndex = currentIndex == startIndex
        if (isSameQueue && isSameIndex && currentIndex in queue.indices && isPlaying()) {
            // Ya está sonando la misma canción en la misma posición, no reiniciar
            return
        }
        queue.clear()
        queue.addAll(songs)
        currentIndex = startIndex
        pendingSeek = seekToPosition
        pendingResume = resumeAfterSeek
        playCurrent(startPaused)
    }

    fun playSong(song: Song) {
        playNow(listOf(song))
    }

    fun restoreQueueState(
        songs: List<Song>,
        startIndex: Int,
        position: Long = 0L,
    ) {
        if (songs.isEmpty() || startIndex !in songs.indices) return
        playbackFadeJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null

        queue.clear()
        queue.addAll(songs)
        currentIndex = startIndex
        pendingSeek = position.takeIf { it > 0L }
        pendingResume = false

        val song = queue[startIndex]
        _state.value =
            PlayerState(
                currentSong = song,
                isPlaying = false,
                position = position.coerceAtLeast(0L),
                duration = song.duration,
            )
    }

    fun queueNext(songs: List<Song>) {
        val insertIndex = (currentIndex + 1).coerceAtMost(queue.size)
        queue.addAll(insertIndex, songs)
    }

    fun queueEnd(songs: List<Song>) {
        queue.addAll(songs)
    }

    /**
     * Replace the internal queue without automatically starting playback.
     * If [keepCurrentSong] is true and the current song exists in [songs],
     * keep the currentIndex pointing to that song; otherwise set to 0.
     */
    fun replaceQueue(
        songs: List<Song>,
        keepCurrentSong: Boolean = true,
    ) {
        val currentSong = if (currentIndex in queue.indices) queue[currentIndex] else null
        queue.clear()
        queue.addAll(songs)
        currentIndex =
            if (keepCurrentSong && currentSong != null) {
                val idx = queue.indexOfFirst { it.id == currentSong.id }
                if (idx >= 0) idx else 0
            } else {
                0
            }
    }

    private fun playCurrent(startPaused: Boolean = false) {
        if (currentIndex !in queue.indices) {
            stop()
            return
        }
        val song = queue[currentIndex]
        play(song, startPaused)
    }

    private fun play(
        song: Song,
        startPaused: Boolean = false,
    ) {
        playbackFadeJob?.cancel()
        mediaPlayer?.release()

        // Request audio focus before starting playback
        requestAudioFocus()

        mediaPlayer =
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build(),
                )
                try {
                    if (context.checkSelfPermission(android.Manifest.permission.WAKE_LOCK) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                    }
                } catch (_: Exception) {
                }
                val preferredSessionId = getOrCreateAudioSessionId()
                if (preferredSessionId != 0) {
                    try {
                        setAudioSessionId(preferredSessionId)
                    } catch (e: Exception) {
                        Log.w("PlayerController", "Could not set fixed audio session id=$preferredSessionId", e)
                    }
                }
                try {
                    setVolume(1f, 1f)
                } catch (_: Exception) {
                }
                setDataSource(context, song.uri)
                setOnPreparedListener { mp ->
                    pendingSeek?.let { pos ->
                        try {
                            mp.seekTo(pos.toInt())
                        } catch (_: Exception) {
                        }
                        pendingSeek = null
                    }

                    val realDuration =
                        try {
                            mp.duration.toLong()
                        } catch (_: Exception) {
                            song.duration
                        }
                    _state.update { it.copy(duration = realDuration) }

                    val sid =
                        try {
                            audioSessionId
                        } catch (_: Exception) {
                            preferredSessionId
                        }
                    var hasStartedPlayback = false
                    val startPlayback = {
                        if (!hasStartedPlayback && (!startPaused || pendingResume)) {
                            hasStartedPlayback = true
                            try {
                                mp.setVolume(1f, 1f)
                                mp.start()
                            } catch (e: Exception) {
                                Log.e("PlayerController", "Error starting MediaPlayer", e)
                            }
                            pendingResume = false
                            _state.update { it.copy(isPlaying = true) }
                            startProgressUpdates()
                        }
                    }
                    if (sid != 0) {
                        ensureFallbackEffectsReady(sid)
                    }
                    onAudioSessionIdChanged?.invoke(sid)
                    if (onReadyToAttachEffects != null && sid != 0) {
                        onReadyToAttachEffects?.invoke(sid, startPlayback)
                        internalScope.launch {
                            delay(300L)
                            startPlayback()
                        }
                    } else {
                        startPlayback()
                    }
                }
                prepareAsync()
                setOnCompletionListener {
                    when (repeatMode) {
                        RepeatMode.ONE -> {
                            // Repeat the same song
                            playCurrent()
                        }

                        RepeatMode.ALL -> {
                            if (currentIndex + 1 < queue.size) {
                                currentIndex++
                                playCurrent()
                            } else {
                                // Loop back to the beginning
                                currentIndex = 0
                                playCurrent()
                            }
                        }

                        RepeatMode.NONE -> {
                            if (currentIndex + 1 < queue.size) {
                                currentIndex++
                                playCurrent()
                            } else {
                                // At the end with no repeat - just pause
                                _state.update { it.copy(isPlaying = false) }
                                releaseAudioFocus()
                                progressJob?.cancel()
                            }
                        }
                    }
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("PlayerController", "MediaPlayer error: what=$what extra=$extra")
                    if (mediaPlayer === mp) {
                        try {
                            mediaPlayer?.release()
                        } catch (_: Exception) {
                        }
                        mediaPlayer = null
                        _state.update { it.copy(isPlaying = false) }
                    }
                    releaseAudioFocus()
                    progressJob?.cancel()
                    true
                }
            }

        // Inicializar el estado con la duración del Song, se actualizará al preparar
        _state.value = PlayerState(currentSong = song, isPlaying = !startPaused || pendingResume, position = 0L, duration = song.duration)

        startProgressUpdates()
    }

    fun setOnAudioSessionIdChangedListener(listener: ((Int) -> Unit)?) {
        onAudioSessionIdChanged = listener
    }

    fun setRepeatMode(mode: RepeatMode) {
        repeatMode = mode
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob =
            internalScope.launch {
                while (true) {
                    try {
                        val mp = mediaPlayer
                        if (mp != null) {
                            val isPlaying =
                                try {
                                    mp.isPlaying
                                } catch (_: Exception) {
                                    false
                                }
                            if (isPlaying) {
                                val pos =
                                    try {
                                        mp.currentPosition.toLong()
                                    } catch (_: Exception) {
                                        null
                                    }
                                if (pos != null) {
                                    _state.update { it.copy(position = pos) }
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                    delay(500L)
                }
            }
    }

    fun togglePlayPause() {
        if (mediaPlayer == null) {
            if (currentIndex in queue.indices) {
                pendingResume = true
                playCurrent(startPaused = false)
            }
            return
        }
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.pause()
                    _state.update { s -> s.copy(isPlaying = false) }
                } else {
                    mp.setVolume(1f, 1f)
                    mp.start()
                    val pos =
                        try {
                            mp.currentPosition.toLong()
                        } catch (_: Exception) {
                            _state.value.position
                        }
                    _state.update { s -> s.copy(isPlaying = true, position = pos) }
                    startProgressUpdates()
                }
            } catch (_: IllegalStateException) {
                // ignore invalid state transitions
                _state.update { s -> s.copy(isPlaying = false) }
            }
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
        } catch (_: IllegalStateException) {
        }
        _state.update { it.copy(isPlaying = false) }
        progressJob?.cancel()
    }

    fun resume() {
        if (mediaPlayer == null) {
            if (currentIndex in queue.indices) {
                pendingResume = true
                playCurrent(startPaused = false)
            }
            return
        }
        mediaPlayer?.let { mp ->
            try {
                mp.setVolume(1f, 1f)
                mp.start()
            } catch (_: Exception) {
            }
        }
        _state.update { it.copy(isPlaying = true) }
        startProgressUpdates()
    }

    fun stop() {
        cancelSleepTimer()
        progressJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        releaseAudioFocus()
        _state.value = PlayerState()
    }

    fun startSleepTimer(durationMs: Long) {
        scheduleSleepTimer(SleepTimer.deadlineFrom(System.currentTimeMillis(), durationMs))
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMs.value = null
        appPrefs.saveSleepTimerDeadline(null)
    }

    private fun restoreSleepTimer() {
        val deadline = appPrefs.loadSleepTimerDeadline() ?: return
        if (deadline <= System.currentTimeMillis()) {
            appPrefs.saveSleepTimerDeadline(null)
            return
        }
        scheduleSleepTimer(deadline)
    }

    private fun scheduleSleepTimer(deadlineEpochMs: Long) {
        sleepTimerJob?.cancel()
        appPrefs.saveSleepTimerDeadline(deadlineEpochMs)
        sleepTimerJob =
            timerScope.launch {
                while (true) {
                    val remainingMs = SleepTimer.remainingMs(deadlineEpochMs, System.currentTimeMillis())
                    if (remainingMs <= 0L) {
                        pause()
                        appPrefs.saveSleepTimerDeadline(null)
                        _sleepTimerRemainingMs.value = null
                        break
                    }
                    _sleepTimerRemainingMs.value = remainingMs
                    delay(1000L)
                }
            }
    }

    fun seekTo(position: Long) {
        mediaPlayer?.let { mp ->
            try {
                mp.seekTo(position.toInt())
            } catch (_: Exception) {
            }
        }
        _state.update { s -> s.copy(position = position) }
    }

    fun next() {

        // RepeatMode.ONE means repeat the current song
        if (repeatMode == RepeatMode.ONE) {
            playCurrent()
            return
        }

        if (currentIndex + 1 < queue.size) {
            currentIndex++
            playCurrent()
        } else {
            // When reaching the end, handle based on repeat mode
            when (repeatMode) {
                RepeatMode.ALL -> {
                    // Loop back to the beginning
                    currentIndex = 0
                    playCurrent()
                }

                RepeatMode.NONE -> {
                    // Do nothing - stay at the last song
                }

                else -> {
                    // Shouldn't reach here
                }
            }
        }
    }

    fun previous() {

        // RepeatMode.ONE means repeat the current song
        if (repeatMode == RepeatMode.ONE) {
            playCurrent()
            return
        }

        if (currentIndex - 1 >= 0) {
            currentIndex--
            playCurrent()
        } else {
            seekTo(0L)
        }
    }

    fun release() {
        progressJob?.cancel()
        playbackFadeJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        releaseAudioFocus()
    }

    fun getCurrentPosition(): Long =
        try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (_: IllegalStateException) {
            0L
        }

    /** Return the underlying MediaPlayer audio session id, or 0 if not available. */
    fun getAudioSessionId(): Int =
        try {
            mediaPlayer?.audioSessionId ?: 0
        } catch (_: Exception) {
            0
        }

    fun getDuration(): Long =
        try {
            mediaPlayer?.duration?.toLong() ?: 0L
        } catch (_: IllegalStateException) {
            0L
        }

    fun isPlaying(): Boolean =
        try {
            mediaPlayer?.isPlaying == true
        } catch (_: IllegalStateException) {
            false
        }

    fun setOnQueueEndedListener(listener: (() -> Unit)?) {
        onQueueEnded = listener
    }

    fun setOnNextAtEndListener(listener: (() -> Unit)?) {
        onNextAtEnd = listener
    }

    /**
     * Request audio focus for music playback.
     * Called when playback starts to ensure this app has priority for audio output.
     */
    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes =
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()

                audioFocusRequest =
                    AudioFocusRequest
                        .Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(audioAttributes)
                        .setWillPauseWhenDucked(false)
                        .setOnAudioFocusChangeListener(this)
                        .build()

                audioManager.requestAudioFocus(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    this,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN,
                )
            }
            pausedByAudioFocus = false
            duckedByAudioFocus = false
        } catch (e: Exception) {
            Log.w("PlayerController", "Error requesting audio focus", e)
        }
    }

    private fun releaseAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    audioManager.abandonAudioFocusRequest(it)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(this)
            }
            pausedByAudioFocus = false
            duckedByAudioFocus = false
            playbackFadeJob?.cancel()
        } catch (e: Exception) {
            Log.w("PlayerController", "Error releasing audio focus", e)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (duckedByAudioFocus) {
                    duckedByAudioFocus = false
                    fadeVolume(from = 0.2f, to = 1f, durationMs = 350L)
                }
                if (pausedByAudioFocus && mediaPlayer != null) {
                    try {
                        mediaPlayer?.setVolume(1f, 1f)
                        mediaPlayer?.start()
                        _state.update { it.copy(isPlaying = true) }
                        startProgressUpdates()
                        pausedByAudioFocus = false
                    } catch (e: Exception) {
                        Log.w("PlayerController", "Error resuming after audio focus gain", e)
                    }
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                        _state.update { it.copy(isPlaying = false) }
                        pausedByAudioFocus = true
                        duckedByAudioFocus = false
                    }
                } catch (e: Exception) {
                    Log.w("PlayerController", "Error pausing after audio focus loss", e)
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                        _state.update { it.copy(isPlaying = false) }
                        pausedByAudioFocus = true
                        duckedByAudioFocus = false
                    }
                } catch (e: Exception) {
                    Log.w("PlayerController", "Error pausing due to transient audio focus loss", e)
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        duckedByAudioFocus = true
                        pausedByAudioFocus = false
                        fadeVolume(from = 1f, to = 0.2f, durationMs = 250L)
                    }
                } catch (e: Exception) {
                    Log.w("PlayerController", "Error applying audio duck", e)
                }
            }
        }
    }

    private fun fadeVolume(
        from: Float,
        to: Float,
        durationMs: Long,
        onComplete: (() -> Unit)? = null,
    ) {
        val mp = mediaPlayer ?: return
        playbackFadeJob?.cancel()
        playbackFadeJob =
            internalScope.launch {
                val steps = 8
                val stepDelay = (durationMs / steps).coerceAtLeast(1L)
                for (step in 1..steps) {
                    if (mediaPlayer !== mp) return@launch
                    val fraction = step.toFloat() / steps.toFloat()
                    val currentVolume = from + (to - from) * fraction
                    try {
                        mp.setVolume(currentVolume, currentVolume)
                    } catch (_: Exception) {
                        return@launch
                    }
                    delay(stepDelay)
                }
                try {
                    mp.setVolume(to, to)
                } catch (_: Exception) {
                }
                onComplete?.invoke()
            }
    }

    private fun fadeInFromSilence(mp: MediaPlayer) {
        try {
            mp.setVolume(1f, 1f)
        } catch (_: Exception) {
        }
    }

    private fun getOrCreateAudioSessionId(): Int {
        if (fixedAudioSessionId != 0) return fixedAudioSessionId
        fixedAudioSessionId =
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioManager.generateAudioSessionId()
                } else {
                    0
                }
            } catch (_: Exception) {
                0
            }
        return fixedAudioSessionId
    }

    private fun ensureFallbackEffectsReady(sessionId: Int) {
        try {
            val enabled = appPrefs.isEqualizerEnabled()
            val savedLevels = appPrefs.getCustomBandLevels()
            fallbackEqualizerController.initializeWithAudioSession(
                sessionId = sessionId,
                bandLevels = savedLevels.ifEmpty { null },
                enabled = enabled,
            )
        } catch (_: Exception) {
        }
    }

    companion object {
        @Suppress("ktlint:standard:property-naming")
        @Volatile
        private var INSTANCE: PlayerController? = null

        fun getInstance(
            context: Context,
            scope: CoroutineScope? = null,
        ): PlayerController =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlayerController(context.applicationContext, scope).also { INSTANCE = it }
            }
    }

    // Callback que se llama cuando el el Mediaplayer está listo pero AÚN no ha iniciado
    private var onReadyToAttachEffects: ((sessionId: Int, startPlayback: () -> Unit) -> Unit)? = null

    fun setOnReadyToAttachEffectsListener(listener: ((Int, () -> Unit) -> Unit)?) {
        onReadyToAttachEffects = listener
    }
}
