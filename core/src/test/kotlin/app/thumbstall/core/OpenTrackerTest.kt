package app.thumbstall.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OpenTrackerTest {

    @Test
    fun `no opens recorded means zero in window`() {
        val tracker = OpenTracker()
        assertEquals(0, tracker.opensWithin("com.example", 60_000L, 0L))
    }

    @Test
    fun `counts opens inside window`() {
        val tracker = OpenTracker()
        tracker.recordOpen("com.example", 0L)
        tracker.recordOpen("com.example", 10_000L)
        assertEquals(2, tracker.opensWithin("com.example", 60_000L, 20_000L))
    }

    @Test
    fun `ignores opens outside window`() {
        val tracker = OpenTracker()
        tracker.recordOpen("com.example", 0L)
        assertEquals(0, tracker.opensWithin("com.example", 60_000L, 61_000L))
    }

    @Test
    fun `windows are per package`() {
        val tracker = OpenTracker()
        tracker.recordOpen("com.example.a", 0L)
        assertEquals(0, tracker.opensWithin("com.example.b", 60_000L, 1_000L))
    }

    @Test
    fun `monotonic timestamps immune to wall clock`() {
        val tracker = OpenTracker()
        tracker.recordOpen("com.example", 5L)
        // window of 6ms from now=10 means anything >= 4 is in window
        assertEquals(1, tracker.opensWithin("com.example", 6L, 10L))
        // window of 4ms from now=10 means anything >= 6 is in window, so 5 is out
        assertEquals(0, tracker.opensWithin("com.example", 4L, 10L))
    }
}
