package com.cvc953.localplayer.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.cvc953.localplayer.Services.MusicService

abstract class MusicServiceAction(
    private val action: String,
) : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        context.startService(
            Intent(context, MusicService::class.java).apply { this.action = action }
        )
    }
}

class PlayPauseAction : MusicServiceAction(MusicService.ACTION_PLAY_PAUSE)
class NextAction : MusicServiceAction(MusicService.ACTION_NEXT)
class PrevAction : MusicServiceAction(MusicService.ACTION_PREV)
class SeekBackward10Action : MusicServiceAction(MusicService.ACTION_SEEK_BACKWARD)
class SeekForward10Action : MusicServiceAction(MusicService.ACTION_SEEK_FORWARD)
