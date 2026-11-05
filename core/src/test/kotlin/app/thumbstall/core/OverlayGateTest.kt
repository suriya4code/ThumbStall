package app.thumbstall.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OverlayGateTest {

    private val targets = setOf("com.instagram.android", "com.zhiliaoapp.musically")

    private fun ev(pkg: String, at: Long) = ForegroundEvent(packageName = pkg, elapsedMs = at)

    @Test
    fun `launches for target package`() {
        val gate = OverlayGate()
        assertTrue(gate.shouldLaunch(ev("com.instagram.android", 0), targets, overlayShowing = false))
    }

    @Test
    fun `ignores non-target package`() {
        val gate = OverlayGate()
        assertFalse(gate.shouldLaunch(ev("com.android.chrome", 0), targets, overlayShowing = false))
    }

    @Test
    fun `skips when overlay already showing`() {
        val gate = OverlayGate()
        assertFalse(gate.shouldLaunch(ev("com.instagram.android", 0), targets, overlayShowing = true))
    }

    @Test
    fun `skips same package within cooldown`() {
        val gate = OverlayGate(cooldownMs = 1000)
        assertTrue(gate.shouldLaunch(ev("com.instagram.android", 0), targets, overlayShowing = false))
        assertFalse(gate.shouldLaunch(ev("com.instagram.android", 500), targets, overlayShowing = false))
        assertTrue(gate.shouldLaunch(ev("com.instagram.android", 1500), targets, overlayShowing = false))
    }

    @Test
    fun `different target package not blocked by cooldown`() {
        val gate = OverlayGate(cooldownMs = 1000)
        assertTrue(gate.shouldLaunch(ev("com.instagram.android", 0), targets, overlayShowing = false))
        assertTrue(gate.shouldLaunch(ev("com.zhiliaoapp.musically", 500), targets, overlayShowing = false))
    }
}
