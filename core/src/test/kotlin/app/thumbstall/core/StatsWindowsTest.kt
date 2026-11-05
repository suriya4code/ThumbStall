package app.thumbstall.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class StatsWindowsTest {

    private val kolkata = ZoneId.of("Asia/Kolkata")

    @Test
    fun `todayStart returns local midnight`() {
        // 2026-11-05T13:00:00+05:30 in Kolkata
        val now = ZonedDateTime.of(2026, 11, 5, 13, 0, 0, 0, kolkata).toInstant().toEpochMilli()
        val todayStart = StatsWindows.todayStartMillis(now, kolkata)

        // Should be 2026-11-05T00:00:00+05:30
        val expected = ZonedDateTime.of(2026, 11, 5, 0, 0, 0, 0, kolkata).toInstant().toEpochMilli()
        assertEquals(expected, todayStart)
    }

    @Test
    fun `todayStart rolls over after midnight`() {
        // 2026-11-06T00:05:00+05:30 — 5 minutes past midnight
        val now = ZonedDateTime.of(2026, 11, 6, 0, 5, 0, 0, kolkata).toInstant().toEpochMilli()
        val todayStart = StatsWindows.todayStartMillis(now, kolkata)

        // Should be 2026-11-06T00:00:00+05:30 (new day)
        val expected = ZonedDateTime.of(2026, 11, 6, 0, 0, 0, 0, kolkata).toInstant().toEpochMilli()
        assertEquals(expected, todayStart)
    }

    @Test
    fun `weekStart returns midnight 6 days ago`() {
        // 2026-11-05T13:00:00+05:30 (Thursday)
        val now = ZonedDateTime.of(2026, 11, 5, 13, 0, 0, 0, kolkata).toInstant().toEpochMilli()
        val weekStart = StatsWindows.weekStartMillis(now, kolkata)

        // Rolling 7-day window: 6 days before today = 2026-10-30T00:00:00+05:30
        val expected = ZonedDateTime.of(2026, 10, 30, 0, 0, 0, 0, kolkata).toInstant().toEpochMilli()
        assertEquals(expected, weekStart)
    }

    @Test
    fun `weekStart handles month boundary`() {
        // 2026-11-02T10:00:00+05:30 (Monday)
        val now = ZonedDateTime.of(2026, 11, 2, 10, 0, 0, 0, kolkata).toInstant().toEpochMilli()
        val weekStart = StatsWindows.weekStartMillis(now, kolkata)

        // 6 days before = 2026-10-27T00:00:00+05:30
        val expected = ZonedDateTime.of(2026, 10, 27, 0, 0, 0, 0, kolkata).toInstant().toEpochMilli()
        assertEquals(expected, weekStart)
    }
}
