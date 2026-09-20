package com.cvc953.localplayer.widget

import com.cvc953.localplayer.Services.MusicService
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class PlayerWidgetActionsTest {

    @Test
    fun `PlayPauseAction has correct action string`() {
        assertEquals(MusicService.ACTION_PLAY_PAUSE, PlayPauseAction().action)
    }

    @Test
    fun `NextAction has correct action string`() {
        assertEquals(MusicService.ACTION_NEXT, NextAction().action)
    }

    @Test
    fun `PrevAction has correct action string`() {
        assertEquals(MusicService.ACTION_PREV, PrevAction().action)
    }

    @Test
    fun `SeekBackward10Action has correct action string`() {
        assertEquals(MusicService.ACTION_SEEK_BACKWARD, SeekBackward10Action().action)
    }

    @Test
    fun `SeekForward10Action has correct action string`() {
        assertEquals(MusicService.ACTION_SEEK_FORWARD, SeekForward10Action().action)
    }

    @Test
    fun `All 5 action subclasses are created`() {
        val actions = listOf(PlayPauseAction(), NextAction(), PrevAction(), SeekBackward10Action(), SeekForward10Action())
        assertTrue(actions.size == 5)
    }
}