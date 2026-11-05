package app.thumbstall.core

/**
 * Tracks app opens by package using monotonic elapsed time.
 * Used to count repeat opens within a rolling window for escalation.
 */
class OpenTracker {
    private val opens = mutableMapOf<String, MutableList<Long>>()

    fun recordOpen(packageName: String, elapsedMs: Long) {
        opens.getOrPut(packageName) { mutableListOf() }.add(elapsedMs)
    }

    fun opensWithin(packageName: String, windowMs: Long, nowElapsedMs: Long): Int {
        val list = opens[packageName] ?: return 0
        val cutoff = nowElapsedMs - windowMs
        // Prune stale entries while counting
        val fresh = list.filter { it > cutoff }
        opens[packageName] = fresh.toMutableList()
        return fresh.size
    }
}
