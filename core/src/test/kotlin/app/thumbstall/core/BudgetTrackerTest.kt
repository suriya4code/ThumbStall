package app.thumbstall.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BudgetTrackerTest {

    @Test
    fun `no budget means not active`() {
        val tracker = BudgetTracker()
        assertFalse(tracker.hasActiveBudget("com.example", nowElapsedMs = 1000L))
    }

    @Test
    fun `granted budget is active within duration`() {
        val tracker = BudgetTracker()
        tracker.grant("com.example", durationMs = 60_000L, nowElapsedMs = 1000L)
        assertTrue(tracker.hasActiveBudget("com.example", nowElapsedMs = 30_000L))
    }

    @Test
    fun `budget expires after duration`() {
        val tracker = BudgetTracker()
        tracker.grant("com.example", durationMs = 60_000L, nowElapsedMs = 1000L)
        // Expiry is at 61_000; check at 62_000
        assertFalse(tracker.hasActiveBudget("com.example", nowElapsedMs = 62_000L))
    }

    @Test
    fun `budgets are per package`() {
        val tracker = BudgetTracker()
        tracker.grant("com.example.a", durationMs = 60_000L, nowElapsedMs = 1000L)
        assertTrue(tracker.hasActiveBudget("com.example.a", nowElapsedMs = 30_000L))
        assertFalse(tracker.hasActiveBudget("com.example.b", nowElapsedMs = 30_000L))
    }

    @Test
    fun `granting new budget overwrites previous`() {
        val tracker = BudgetTracker()
        tracker.grant("com.example", durationMs = 10_000L, nowElapsedMs = 1000L)
        // Original expiry at 11_000
        tracker.grant("com.example", durationMs = 60_000L, nowElapsedMs = 5000L)
        // New expiry at 65_000; check at 50_000 (would be expired under old grant)
        assertTrue(tracker.hasActiveBudget("com.example", nowElapsedMs = 50_000L))
    }

    @Test
    fun `remainingMs returns correct value`() {
        val tracker = BudgetTracker()
        tracker.grant("com.example", durationMs = 60_000L, nowElapsedMs = 1000L)
        // Expiry at 61_000; at 31_000 remaining is 30_000
        assertEquals(30_000L, tracker.remainingMs("com.example", nowElapsedMs = 31_000L))
    }

    @Test
    fun `remainingMs returns zero when expired`() {
        val tracker = BudgetTracker()
        tracker.grant("com.example", durationMs = 60_000L, nowElapsedMs = 1000L)
        assertEquals(0L, tracker.remainingMs("com.example", nowElapsedMs = 100_000L))
    }

    @Test
    fun `remainingMs returns zero when no budget`() {
        val tracker = BudgetTracker()
        assertEquals(0L, tracker.remainingMs("com.example", nowElapsedMs = 1000L))
    }
}
