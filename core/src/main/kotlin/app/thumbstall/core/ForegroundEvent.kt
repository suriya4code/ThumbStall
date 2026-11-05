package app.thumbstall.core

/**
 * A foreground-app change observed by the poller.
 *
 * @param packageName the package now in the foreground
 * @param elapsedMs monotonic timestamp (SystemClock.elapsedRealtime) of the observation
 */
data class ForegroundEvent(
    val packageName: String,
    val elapsedMs: Long,
)
