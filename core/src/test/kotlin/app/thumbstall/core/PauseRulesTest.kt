package app.thumbstall.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PauseRulesTest {

    private val chrome = ForegroundEvent("com.android.chrome", 1000L)
    private val instagram = ForegroundEvent("com.instagram.android", 1000L)
    private val dialer = ForegroundEvent("com.google.android.dialer", 1000L)

    private val targets = setOf("com.instagram.android")
    private val essentials = setOf("com.google.android.dialer")

    @Test
    fun `non-target gets no pause`() {
        val rules = PauseRules()
        val decision = rules.decide(chrome, targets, essentials, priorOpensInWindow = 0)
        assertEquals(PauseDecision.None, decision)
    }

    @Test
    fun `target gets base pause`() {
        val rules = PauseRules()
        val decision = rules.decide(instagram, targets, essentials, priorOpensInWindow = 0)
        assertEquals(PauseDecision.Pause(8), decision)
    }

    @Test
    fun `essential never paused even when targeted`() {
        val rules = PauseRules()
        val targetsWithEssential = setOf("com.instagram.android", "com.google.android.dialer")
        val decision = rules.decide(dialer, targetsWithEssential, essentials, priorOpensInWindow = 0)
        assertEquals(PauseDecision.None, decision)
    }

    @Test
    fun `each repeat open within window adds step`() {
        val rules = PauseRules()
        // base 8 + 2 * 5 = 18
        val decision = rules.decide(instagram, targets, essentials, priorOpensInWindow = 2)
        assertEquals(PauseDecision.Pause(18), decision)
    }

    @Test
    fun `escalation caps at max`() {
        val rules = PauseRules()
        // base 8 + 10 * 5 = 58, but max is 30
        val decision = rules.decide(instagram, targets, essentials, priorOpensInWindow = 10)
        assertEquals(PauseDecision.Pause(30), decision)
    }

    @Test
    fun `gentle preset uses lower base`() {
        val rules = PauseRules(PauseConfig(basePauseSeconds = 5))
        val decision = rules.decide(instagram, targets, essentials, priorOpensInWindow = 0)
        assertEquals(PauseDecision.Pause(5), decision)
    }

    @Test
    fun `focused preset uses higher base`() {
        val rules = PauseRules(PauseConfig(basePauseSeconds = 15))
        val decision = rules.decide(instagram, targets, essentials, priorOpensInWindow = 0)
        assertEquals(PauseDecision.Pause(15), decision)
    }
}
