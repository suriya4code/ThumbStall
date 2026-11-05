package app.thumbstall.core

import kotlin.math.min

/**
 * Configuration for pause behavior.
 * @param basePauseSeconds Initial pause duration (Gentle=5, Balanced=8, Focused=15)
 * @param escalationStepSeconds Added per repeat open within the window
 * @param escalationWindowMs Rolling window for counting repeat opens (default 1 hour)
 * @param maxPauseSeconds Cap on total pause duration
 */
data class PauseConfig(
    val basePauseSeconds: Int = 8,
    val escalationStepSeconds: Int = 5,
    val escalationWindowMs: Long = 3_600_000L,
    val maxPauseSeconds: Int = 30
)

/**
 * Decision returned by the rules engine.
 */
sealed interface PauseDecision {
    data object None : PauseDecision
    data class Pause(val seconds: Int) : PauseDecision
}

/**
 * Pure rules engine deciding whether to pause an app open.
 * No side effects, no Android deps — fully JVM testable.
 */
class PauseRules(private val config: PauseConfig = PauseConfig()) {

    fun decide(
        event: ForegroundEvent,
        targetPackages: Set<String>,
        essentialPackages: Set<String>,
        priorOpensInWindow: Int
    ): PauseDecision {
        val pkg = event.packageName

        // Essentials always pass through, even if targeted
        if (pkg in essentialPackages) return PauseDecision.None

        // Non-targets pass through
        if (pkg !in targetPackages) return PauseDecision.None

        // Calculate escalating pause
        val raw = config.basePauseSeconds + (priorOpensInWindow * config.escalationStepSeconds)
        val capped = min(raw, config.maxPauseSeconds)
        return PauseDecision.Pause(capped)
    }
}
