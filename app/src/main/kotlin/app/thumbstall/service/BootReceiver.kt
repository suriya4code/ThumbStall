package app.thumbstall.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.thumbstall.Permissions
import app.thumbstall.data.SettingsStore
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        try {
            val settingsStore = SettingsStore(context)
            val snoozeExpiry = runBlocking { settingsStore.snoozeExpiryWallMs() }

            if (snoozeExpiry != null && snoozeExpiry > System.currentTimeMillis()) {
                // Snooze is still active — re-arm the alarm, don't start monitoring
                SnoozeScheduler.schedule(context, snoozeExpiry)
                Log.d("ThumbStall", "boot: snooze still active, re-armed alarm")
            } else if (MonitorService.isEnabled(context) &&
                Permissions.hasUsageAccess(context) &&
                Permissions.hasOverlay(context)
            ) {
                // Clear any stale snooze and start monitoring
                runBlocking { settingsStore.clearSnooze() }
                MonitorService.startMonitor(context)
            }
        } catch (t: Throwable) {
            // Fail open: a failed re-arm must never crash the boot broadcast.
            Log.w("ThumbStall", "boot re-arm failed", t)
        }
    }
}
