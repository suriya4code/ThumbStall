package app.thumbstall.core

import java.time.Instant
import java.time.ZoneId

/**
 * Helpers for computing stat window boundaries in local time.
 * Uses wall time (not monotonic) — appropriate for day/week boundaries only.
 */
object StatsWindows {

    /**
     * Returns epoch millis of local midnight today.
     */
    fun todayStartMillis(nowWallMs: Long, zone: ZoneId): Long {
        val now = Instant.ofEpochMilli(nowWallMs).atZone(zone)
        val midnight = now.toLocalDate().atStartOfDay(zone)
        return midnight.toInstant().toEpochMilli()
    }

    /**
     * Returns epoch millis of local midnight 6 days before today (rolling 7-day window).
     */
    fun weekStartMillis(nowWallMs: Long, zone: ZoneId): Long {
        val now = Instant.ofEpochMilli(nowWallMs).atZone(zone)
        val sixDaysAgo = now.toLocalDate().minusDays(6).atStartOfDay(zone)
        return sixDaysAgo.toInstant().toEpochMilli()
    }
}
