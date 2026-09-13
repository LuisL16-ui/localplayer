package com.cvc953.localplayer.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.GlanceId

class PlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: android.content.Context,
        id: GlanceId,
    ) {
        // Glance composable functions (@Composable) cannot be called from a suspend function.
        // The UI is rendered via GlanceAppWidgetKt.provideContent() which is internal.
        // State updates are pushed from MusicService via GlanceAppWidgetManager.getGlanceIds
        // and PlayerWidget().update(context, id).
    }
}