package app.thumbstall.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Schedules an exact alarm to resume protection after snooze expires.
 */
object SnoozeScheduler {

    private const val TAG = "ThumbStall"
    private const val ACTION_SNOOZE_END = "app.thumbstall.SNOOZE_END"

    fun schedule(context: Context, expiryWallMs: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, SnoozeEndReceiver::class.java).apply {
            action = ACTION_SNOOZE_END
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        expiryWallMs,
                        pendingIntent
                    )
                } else {
                    // Fall back to inexact alarm
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        expiryWallMs,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    expiryWallMs,
                    pendingIntent
                )
            }
            Log.d(TAG, "Snooze alarm scheduled for $expiryWallMs")
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot schedule exact alarm, using inexact", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                expiryWallMs,
                pendingIntent
            )
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, SnoozeEndReceiver::class.java).apply {
            action = ACTION_SNOOZE_END
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Snooze alarm cancelled")
    }

    class SnoozeEndReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action == ACTION_SNOOZE_END) {
                Log.d(TAG, "Snooze ended, restarting monitor")
                // Clear snooze and restart monitoring
                MonitorService.startMonitor(context)
            }
        }
    }
}
