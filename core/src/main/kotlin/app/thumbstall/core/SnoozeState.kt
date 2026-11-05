package app.thumbstall.core

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Represents a snooze with both monotonic and wall-clock expiry.
 * Monotonic (elapsedMs) used for correctness; wall (wallMs) for display and persistence.
 */
data class SnoozeState(
    val expiryElapsedMs: Long,
    val expiryWallMs: Long
)

/**
 * Calculations for snooze state using monotonic elapsed time.
 */
object SnoozeCalc {

    /**
     * Creates snooze state for a fixed duration from now.
     */
    fun forDuration(durationMs: Long, nowElapsedMs: Long, nowWallMs: Long): SnoozeState =
        SnoozeState(
            expiryElapsedMs = nowElapsedMs + durationMs,
            expiryWallMs = nowWallMs + durationMs
        )

    /**
     * Creates snooze state until 9 PM local time today.
     * If already past 9 PM, returns now + 1 minute as a minimal snooze.
     */
    fun untilTonight(nowElapsedMs: Long, nowWallMs: Long, zone: ZoneId): SnoozeState {
        val now = Instant.ofEpochMilli(nowWallMs).atZone(zone)
        val ninePm = now.toLocalDate().atTime(LocalTime.of(21, 0)).atZone(zone)

        val targetWallMs = if (now.isBefore(ninePm)) {
            ninePm.toInstant().toEpochMilli()
        } else {
            // Past 9 PM — snooze for 1 minute
            nowWallMs + 60_000L
        }

        val durationMs = targetWallMs - nowWallMs
        return SnoozeState(
            expiryElapsedMs = nowElapsedMs + durationMs,
            expiryWallMs = targetWallMs
        )
    }

    /**
     * Returns true if snooze is active (not expired).
     */
    fun isActive(state: SnoozeState?, nowElapsedMs: Long): Boolean {
        if (state == null) return false
        return nowElapsedMs < state.expiryElapsedMs
    }
}
