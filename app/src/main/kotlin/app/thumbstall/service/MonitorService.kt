package app.thumbstall.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import app.thumbstall.core.SnoozeCalc
import app.thumbstall.core.SnoozeState
import app.thumbstall.EssentialApps
import app.thumbstall.ThumbStallApp
import app.thumbstall.R
import app.thumbstall.core.OpenTracker
import app.thumbstall.core.OverlayGate
import app.thumbstall.core.PauseConfig
import app.thumbstall.core.PauseDecision
import app.thumbstall.core.PauseRules
import app.thumbstall.data.AppDatabase
import app.thumbstall.data.PauseEvent
import app.thumbstall.data.SettingsStore
import app.thumbstall.overlay.PauseActivity
import app.thumbstall.usage.ForegroundPoller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null
    private lateinit var poller: ForegroundPoller
    private val overlayGate = OverlayGate()
    private val openTracker = OpenTracker()

    private var targetPackages: Set<String> = emptySet()
    private var essentialPackages: Set<String> = emptySet()
    private var pauseRules: PauseRules = PauseRules()
    private var pollCount = 0
    private var snoozeState: SnoozeState? = null

    private lateinit var settingsStore: SettingsStore
    private lateinit var db: AppDatabase

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> stopPolling()
                Intent.ACTION_SCREEN_ON -> startPolling()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        poller = ForegroundPoller(this)
        settingsStore = SettingsStore(this)
        db = AppDatabase.get(this)
        essentialPackages = EssentialApps.resolve(this)
        refreshSettings()

        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
        )
    }

    private fun refreshSettings() {
        runBlocking {
            targetPackages = settingsStore.targetPackages()
            val intensity = settingsStore.intensity()
            val baseSeconds = when (intensity) {
                "gentle" -> 5
                "focused" -> 15
                else -> 8 // balanced
            }
            pauseRules = PauseRules(PauseConfig(basePauseSeconds = baseSeconds))

            // Recalculate snooze state from persisted wall time
            val snoozeExpiryWall = settingsStore.snoozeExpiryWallMs()
            snoozeState = if (snoozeExpiryWall != null) {
                val nowElapsed = SystemClock.elapsedRealtime()
                val nowWall = System.currentTimeMillis()
                val remainingMs = snoozeExpiryWall - nowWall
                if (remainingMs > 0) {
                    SnoozeState(
                        expiryElapsedMs = nowElapsed + remainingMs,
                        expiryWallMs = snoozeExpiryWall
                    )
                } else {
                    settingsStore.clearSnooze()
                    null
                }
            } else null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        refreshSettings()
        startPolling()
        return START_STICKY
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                // Refresh settings periodically
                pollCount++
                if (pollCount % SETTINGS_REFRESH_INTERVAL == 0) {
                    refreshSettings()
                }

                poller.poll()?.let { event ->
                    Log.d(TAG, "foreground: ${event.packageName}")
                    try {
                        val nowElapsed = SystemClock.elapsedRealtime()

                        // Skip all pauses during snooze
                        if (SnoozeCalc.isActive(snoozeState, nowElapsed)) {
                            return@let
                        }

                        // Skip pause if this package has active budget
                        val budgetTracker = ThumbStallApp.get().budgetTracker
                        if (budgetTracker.hasActiveBudget(event.packageName, nowElapsed)) {
                            return@let
                        }

                        if (!overlayGate.shouldLaunch(event, targetPackages, PauseActivity.showing)) {
                            return@let
                        }

                        val priorOpens = openTracker.opensWithin(
                            event.packageName,
                            pauseRules.config.escalationWindowMs,
                            event.elapsedMs
                        )

                        when (val decision = pauseRules.decide(event, targetPackages, essentialPackages, priorOpens)) {
                            is PauseDecision.None -> { /* pass through */ }
                            is PauseDecision.Pause -> {
                                openTracker.recordOpen(event.packageName, event.elapsedMs)
                                logEvent(event.packageName, PauseEvent.TYPE_PAUSED, decision.seconds)
                                startActivity(
                                    Intent(this@MonitorService, PauseActivity::class.java)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        .putExtra(PauseActivity.EXTRA_TARGET_PACKAGE, event.packageName)
                                        .putExtra(PauseActivity.EXTRA_PAUSE_SECONDS, decision.seconds)
                                )
                            }
                        }
                    } catch (t: Throwable) {
                        Log.w(TAG, "overlay decision/launch failed (failing open)", t)
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun logEvent(packageName: String, type: String, pauseSeconds: Int = 0) {
        scope.launch {
            try {
                db.eventDao().insert(
                    PauseEvent(
                        packageName = packageName,
                        type = type,
                        wallTimeMs = System.currentTimeMillis(),
                        pauseSeconds = pauseSeconds
                    )
                )
            } catch (t: Throwable) {
                Log.w(TAG, "event logging failed (failing open)", t)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Protection",
                    NotificationManager.IMPORTANCE_MIN,
                ),
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("ThumbStall is protecting you")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "ThumbStall"
        private const val CHANNEL_ID = "monitor"
        private const val NOTIFICATION_ID = 1
        private const val POLL_INTERVAL_MS = 900L
        private const val SETTINGS_REFRESH_INTERVAL = 30

        private const val PREFS = "thumbstall"
        private const val KEY_ENABLED = "protection_enabled"

        fun startMonitor(context: Context) {
            setEnabled(context, true)
            context.startForegroundService(Intent(context, MonitorService::class.java))
        }

        fun stopMonitor(context: Context) {
            setEnabled(context, false)
            context.stopService(Intent(context, MonitorService::class.java))
        }

        fun isEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false)

        private fun setEnabled(context: Context, value: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, value).apply()
        }
    }
}
