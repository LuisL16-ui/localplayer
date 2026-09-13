package com.cvc953.localplayer.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.ImageProvider
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.glance.Button
import androidx.glance.appwidget.LinearProgressIndicator
import com.cvc953.localplayer.preferences.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PlayerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val prefs = AppPrefs(context)
        val lastUri = prefs.loadLastSongUri()
        val songTitle = prefs.loadTitle()
        val artist = prefs.loadArtist()
        val position = prefs.loadPlaybackPosition()
        val isPlaying = prefs.loadIsPlaying()
        val duration = prefs.loadDuration()
        val albumUri = lastUri?.let { uri ->
            runCatching {
                withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(Uri.parse(uri))
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        val cacheFile = File(context.cacheDir, "widget_album.png")
                        FileOutputStream(cacheFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        Uri.fromFile(cacheFile)
                    } else null
                }
            }.getOrNull()
        }
        provideContent {
            Content(songTitle, artist, position, isPlaying, duration, albumUri)
        }
    }

    @Composable
    private fun Content(
        songTitle: String,
        artist: String,
        position: Long,
        isPlaying: Boolean,
        duration: Long,
        albumUri: Uri?,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            albumUri?.let { uri ->
                Image(
                    provider = ImageProvider(uri),
                    contentDescription = "Album art",
                    modifier = GlanceModifier.fillMaxSize(),
                )
            }
            Text(text = songTitle)
            Text(text = artist)
            LinearProgressIndicator(
                progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
            )
            Row {
                Button(text = "-10", onClick = actionRunCallback<SeekBackward10Action>())
                Button(text = if (isPlaying) "❚❚" else "▶", onClick = actionRunCallback<PlayPauseAction>())
                Button(text = "+10", onClick = actionRunCallback<SeekForward10Action>())
            }
        }
    }
}
