package com.cvc953.localplayer.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.text.Text
import com.cvc953.localplayer.R
import com.cvc953.localplayer.preferences.AppPrefs
import com.cvc953.localplayer.util.ArtworkLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val prefs = AppPrefs(context)
        val lastSongUri = prefs.loadLastSongUri()
        val position = prefs.loadPlaybackPosition()
        val isPlaying = prefs.loadIsPlaying()

        val bitmap: android.graphics.Bitmap? = null

        MyContent(
            isPlaying = isPlaying,
            position = position,
            duration = 0L,
            artwork = bitmap,
        )
    }
}

@Composable
fun MyContent(
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    artwork: android.graphics.Bitmap?,
) {
    Column(modifier = GlanceModifier) {
        Image(
            provider = if (artwork != null) ImageProvider(artwork)
                else ImageProvider(R.drawable.ic_launcher_foreground),
            contentDescription = "Album Art",
            modifier = GlanceModifier,
        )
        Text("Reproduciendo", modifier = GlanceModifier)
        Text("", modifier = GlanceModifier)
        LinearProgressIndicator(
            progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
            modifier = GlanceModifier,
        )
        Row(modifier = GlanceModifier) {
            Button(text = "\u2039", onClick = actionRunCallback<PrevAction>())
            Button(text = if (isPlaying) "Pause" else "Play", onClick = actionRunCallback<PlayPauseAction>())
            Button(text = "\u203A", onClick = actionRunCallback<NextAction>())
        }
        Row(modifier = GlanceModifier) {
            Button(text = "-10", onClick = actionRunCallback<SeekBackward10Action>())
            Button(text = "+10", onClick = actionRunCallback<SeekForward10Action>())
        }
    }
}