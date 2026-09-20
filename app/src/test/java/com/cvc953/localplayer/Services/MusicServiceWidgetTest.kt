package com.cvc953.localplayer.Services

import org.junit.Test
import org.junit.Assert.assertTrue

class MusicServiceWidgetTest {

    @Test
    fun `MusicService has widget update constants`() {
        assertTrue(MusicService.ACTION_SEEK_BACKWARD.isNotEmpty())
        assertTrue(MusicService.ACTION_SEEK_FORWARD.isNotEmpty())
    }
}