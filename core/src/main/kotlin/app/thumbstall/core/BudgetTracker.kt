package app.thumbstall.core

/**
 * Tracks per-package session budgets using monotonic elapsed time.
 * Budget allows an app to open without pause for a duration.
 * In-memory only — budget is lost on process death.
 */
class BudgetTracker {
    // packageName -> expiry elapsed time in ms
    private val budgets = mutableMapOf<String, Long>()

    /**
     * Grants budget for [packageName] lasting [durationMs] from [nowElapsedMs].
     * Overwrites any existing budget for this package.
     */
    fun grant(packageName: String, durationMs: Long, nowElapsedMs: Long) {
        budgets[packageName] = nowElapsedMs + durationMs
    }

    /**
     * Returns true if [packageName] has an active (unexpired) budget.
     */
    fun hasActiveBudget(packageName: String, nowElapsedMs: Long): Boolean {
        val expiry = budgets[packageName] ?: return false
        return nowElapsedMs < expiry
    }

    /**
     * Returns remaining budget in ms, or 0 if expired or no budget.
     */
    fun remainingMs(packageName: String, nowElapsedMs: Long): Long {
        val expiry = budgets[packageName] ?: return 0L
        return (expiry - nowElapsedMs).coerceAtLeast(0L)
    }
}
