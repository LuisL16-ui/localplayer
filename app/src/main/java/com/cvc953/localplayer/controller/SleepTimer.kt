package com.cvc953.localplayer.controller

internal object SleepTimer {
    fun deadlineFrom(
        nowEpochMs: Long,
        durationMs: Long,
    ): Long {
        require(durationMs > 0L) { "Sleep timer duration must be positive" }
        return nowEpochMs + durationMs
    }

    fun remainingMs(
        deadlineEpochMs: Long,
        nowEpochMs: Long,
    ): Long = (deadlineEpochMs - nowEpochMs).coerceAtLeast(0L)

    fun remainingMinutes(
        deadlineEpochMs: Long,
        nowEpochMs: Long,
    ): Long = remainingMinutesFromMs(remainingMs(deadlineEpochMs, nowEpochMs))

    fun remainingMinutesFromMs(remainingMs: Long): Long {
        val normalizedRemainingMs = remainingMs.coerceAtLeast(0L)
        return if (normalizedRemainingMs == 0L) 0L else (normalizedRemainingMs + 59_999L) / 60_000L
    }
}
