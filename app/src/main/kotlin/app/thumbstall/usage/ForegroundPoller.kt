package app.thumbstall.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log
import app.thumbstall.core.ForegroundEvent

/**
 * Detects the current foreground app by polling [UsageStatsManager.queryEvents].
 *
 * Fail-open: any exception is logged and swallowed; a `null` return means
 * "nothing new", never an error the caller must handle.
 */
class ForegroundPoller(context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val foregroundEventType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        UsageEvents.Event.ACTIVITY_RESUMED
    } else {
        @Suppress("DEPRECATION")
        UsageEvents.Event.MOVE_TO_FOREGROUND
    }

    /** Wall-clock start of the next query window. Backdate the first query by 2 s. */
    private var windowStart: Long = System.currentTimeMillis() - FIRST_WINDOW_BACKDATE_MS

    private var lastPackage: String? = null

    /** Returns a new foreground package change, or null when nothing changed. */
    fun poll(): ForegroundEvent? {
        return try {
            val now = System.currentTimeMillis()
            val events = usageStatsManager.queryEvents(windowStart, now) ?: return null
            var latest: String? = null
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == foregroundEventType) {
                    latest = event.packageName
                }
            }
            // Advance the window only after a successful read.
            windowStart = now
            if (latest != null && latest != lastPackage) {
                lastPackage = latest
                ForegroundEvent(packageName = latest, elapsedMs = SystemClock.elapsedRealtime())
            } else {
                null
            }
        } catch (t: Throwable) {
            // Fail open: never let detection errors block anything.
            Log.w(TAG, "poll failed", t)
            null
        }
    }

    private companion object {
        const val TAG = "ThumbStall"
        const val FIRST_WINDOW_BACKDATE_MS = 2_000L
    }
}
