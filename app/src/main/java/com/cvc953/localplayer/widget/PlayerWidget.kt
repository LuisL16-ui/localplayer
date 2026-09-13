package com.cvc953.localplayer.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.ImageProvider
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.Button
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.background
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
            WidgetContent(songTitle, artist, position, isPlaying, duration, albumUri)
        }
    }

    @Composable
    private fun WidgetContent(
        songTitle: String,
        artist: String,
        position: Long,
        isPlaying: Boolean,
        duration: Long,
        albumUri: Uri?,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFF1A1A1A), Color(0xFF1A1A1A))).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album art
            albumUri?.let { uri ->
                Image(
                    provider = ImageProvider(uri),
                    contentDescription = "Album art",
                    modifier = GlanceModifier.size(80.dp),
                )
            }

            // Song info + controls
            Column(
                modifier = GlanceModifier.defaultWeight().padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = songTitle,
                    style = TextStyle(
                        color = ColorProvider(Color.White, Color.White),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = artist,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFAAAAAA), Color(0xFFAAAAAA)),
                        fontSize = 12.sp,
                    ),
                    maxLines = 1,
                )

                LinearProgressIndicator(
                    progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
                    modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                )

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(
                        text = "⏮",
                        onClick = actionRunCallback<PrevAction>(),
                    )
                    Button(
                        text = if (isPlaying) "⏸" else "▶",
                        onClick = actionRunCallback<PlayPauseAction>(),
                    )
                    Button(
                        text = "⏭",
                        onClick = actionRunCallback<NextAction>(),
                    )
                }
            }
        }
    }
}
