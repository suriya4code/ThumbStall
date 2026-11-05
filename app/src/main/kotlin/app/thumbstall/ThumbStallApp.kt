package app.thumbstall

import android.app.Application
import app.thumbstall.core.BudgetTracker

/**
 * Application subclass holding singletons that need to survive across activities.
 */
class ThumbStallApp : Application() {

    /**
     * In-memory budget tracker. Budget is lost on process death — acceptable
     * since budgets are short (5-15 min) and fail-open is safe.
     */
    val budgetTracker = BudgetTracker()

    companion object {
        private lateinit var instance: ThumbStallApp

        fun get(): ThumbStallApp = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
