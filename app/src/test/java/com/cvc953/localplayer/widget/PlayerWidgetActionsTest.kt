package com.cvc953.localplayer.widget

import com.cvc953.localplayer.Services.MusicService
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

class PlayerWidgetActionsTest {

    @RelaxedMockK
    lateinit var context: android.content.Context

    @RelaxedMockK
    lateinit var glanceId: androidx.glance.GlanceId

    @RelaxedMockK
    lateinit var parameters: androidx.glance.action.ActionParameters

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `PlayPauseAction starts service with correct action string`() = runBlocking {
        val action = PlayPauseAction()
        action.onAction(context, glanceId, parameters)
        val intentCaptor = slot<android.content.Intent>()
        coVerify { context.startService(capture(intentCaptor)) }
        assertEquals(MusicService.ACTION_PLAY_PAUSE, intentCaptor.captured.action)
    }

    @Test
    fun `NextAction starts service with correct action string`() = runBlocking {
        val action = NextAction()
        action.onAction(context, glanceId, parameters)
        val intentCaptor = slot<android.content.Intent>()
        coVerify { context.startService(capture(intentCaptor)) }
        assertEquals(MusicService.ACTION_NEXT, intentCaptor.captured.action)
    }

    @Test
    fun `PrevAction starts service with correct action string`() = runBlocking {
        val action = PrevAction()
        action.onAction(context, glanceId, parameters)
        val intentCaptor = slot<android.content.Intent>()
        coVerify { context.startService(capture(intentCaptor)) }
        assertEquals(MusicService.ACTION_PREV, intentCaptor.captured.action)
    }

    @Test
    fun `SeekBackward10Action starts service with correct action string`() = runBlocking {
        val action = SeekBackward10Action()
        action.onAction(context, glanceId, parameters)
        val intentCaptor = slot<android.content.Intent>()
        coVerify { context.startService(capture(intentCaptor)) }
        assertEquals(MusicService.ACTION_SEEK_BACKWARD, intentCaptor.captured.action)
    }

    @Test
    fun `SeekForward10Action starts service with correct action string`() = runBlocking {
        val action = SeekForward10Action()
        action.onAction(context, glanceId, parameters)
        val intentCaptor = slot<android.content.Intent>()
        coVerify { context.startService(capture(intentCaptor)) }
        assertEquals(MusicService.ACTION_SEEK_FORWARD, intentCaptor.captured.action)
    }
}