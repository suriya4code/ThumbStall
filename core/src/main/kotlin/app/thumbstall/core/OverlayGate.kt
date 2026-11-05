package app.thumbstall.core

/**
 * Decides whether the pause overlay should launch for a foreground event.
 *
 * Launches only when the package is a configured target, no overlay is already
 * showing, and the same package has not triggered a launch within [cooldownMs]
 * (prevents overlay launch loops from repeated foreground events).
 *
 * Timestamps are monotonic ([ForegroundEvent.elapsedMs]); wall-clock changes
 * cannot bypass the cooldown.
 */
class OverlayGate(private val cooldownMs: Long = 1000) {

    private val lastLaunch = mutableMapOf<String, Long>()

    fun shouldLaunch(
        event: ForegroundEvent,
        targetPackages: Set<String>,
        overlayShowing: Boolean,
    ): Boolean {
        if (event.packageName !in targetPackages) return false
        if (overlayShowing) return false
        val last = lastLaunch[event.packageName]
        if (last != null && event.elapsedMs - last < cooldownMs) return false
        lastLaunch[event.packageName] = event.elapsedMs
        return true
    }
}
