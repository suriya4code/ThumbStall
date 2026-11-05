package app.thumbstall.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.thumbstall.Permissions

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        try {
            if (MonitorService.isEnabled(context) &&
                Permissions.hasUsageAccess(context) &&
                Permissions.hasOverlay(context)
            ) {
                MonitorService.startMonitor(context)
            }
        } catch (t: Throwable) {
            // Fail open: a failed re-arm must never crash the boot broadcast.
            Log.w("ThumbStall", "boot re-arm failed", t)
        }
    }
}
