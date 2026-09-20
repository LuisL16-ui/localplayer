package com.cvc953.localplayer.controller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SleepTimerTest {
    @Test
    fun `deadline is calculated from current time and duration`() {
        assertEquals(90_000L, SleepTimer.deadlineFrom(30_000L, 60_000L))
    }

    @Test
    fun `duration must be positive`() {
        assertThrows(IllegalArgumentException::class.java) {
            SleepTimer.deadlineFrom(30_000L, 0L)
        }
    }

    @Test
    fun `remaining time never becomes negative`() {
        assertEquals(0L, SleepTimer.remainingMs(30_000L, 45_000L))
    }

    @Test
    fun `remaining minutes round up partial minutes`() {
        assertEquals(2L, SleepTimer.remainingMinutesFromMs(60_001L))
        assertEquals(1L, SleepTimer.remainingMinutes(90_000L, 30_001L))
    }
}
