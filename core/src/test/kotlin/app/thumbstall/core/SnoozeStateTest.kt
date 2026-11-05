package app.thumbstall.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class SnoozeStateTest {

    private val kolkata = ZoneId.of("Asia/Kolkata")

    @Test
    fun `forDuration computes correct expiry`() {
        val state = SnoozeCalc.forDuration(
            durationMs = 900_000L, // 15 min
            nowElapsedMs = 10_000L,
            nowWallMs = 1_000_000L
        )
        assertEquals(910_000L, state.expiryElapsedMs)
        assertEquals(1_900_000L, state.expiryWallMs)
    }

    @Test
    fun `untilTonight returns 9 PM local`() {
        // 2026-11-05T14:00:00+05:30 in Kolkata (2 PM)
        val nowWall = ZonedDateTime.of(2026, 11, 5, 14, 0, 0, 0, kolkata).toInstant().toEpochMilli()
        val nowElapsed = 100_000L

        val state = SnoozeCalc.untilTonight(nowElapsed, nowWall, kolkata)

        // 9 PM same day = 2026-11-05T21:00:00+05:30
        val expected9pm = ZonedDateTime.of(2026, 11, 5, 21, 0, 0, 0, kolkata).toInstant().toEpochMilli()
        assertEquals(expected9pm, state.expiryWallMs)

        // Elapsed should be nowElapsed + (9pm - nowWall)
        val expectedElapsed = nowElapsed + (expected9pm - nowWall)
        assertEquals(expectedElapsed, state.expiryElapsedMs)
    }

    @Test
    fun `untilTonight after 9 PM returns now plus 1 minute`() {
        // 2026-11-05T22:00:00+05:30 in Kolkata (10 PM, past 9 PM)
        val nowWall = ZonedDateTime.of(2026, 11, 5, 22, 0, 0, 0, kolkata).toInstant().toEpochMilli()
        val nowElapsed = 100_000L

        val state = SnoozeCalc.untilTonight(nowElapsed, nowWall, kolkata)

        // Should be now + 1 minute
        assertEquals(nowWall + 60_000L, state.expiryWallMs)
        assertEquals(nowElapsed + 60_000L, state.expiryElapsedMs)
    }

    @Test
    fun `isActive true before expiry`() {
        val state = SnoozeState(expiryElapsedMs = 100_000L, expiryWallMs = 0L)
        assertTrue(SnoozeCalc.isActive(state, nowElapsedMs = 50_000L))
    }

    @Test
    fun `isActive false after expiry`() {
        val state = SnoozeState(expiryElapsedMs = 100_000L, expiryWallMs = 0L)
        assertFalse(SnoozeCalc.isActive(state, nowElapsedMs = 150_000L))
    }

    @Test
    fun `isActive false when null`() {
        assertFalse(SnoozeCalc.isActive(null, nowElapsedMs = 50_000L))
    }
}
