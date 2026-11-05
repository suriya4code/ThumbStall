package app.thumbstall.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import app.thumbstall.core.StatsWindows
import app.thumbstall.data.AppDatabase
import app.thumbstall.data.AppOpenCount
import java.time.ZoneId

@Composable
fun TodayScreen(
    protectionRunning: Boolean,
    selectedAppCount: Int,
    onChooseApps: () -> Unit,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
    onStart: () -> Unit,
    onRestart: () -> Unit = onStart,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val isHealthStale = remember { HealthCheck.isStale(context) }

    var timeReclaimedSeconds by remember { mutableIntStateOf(0) }
    var dismissedCount by remember { mutableIntStateOf(0) }
    var pausedCount by remember { mutableIntStateOf(0) }
    var appCounts by remember { mutableStateOf<List<AppOpenCount>>(emptyList()) }

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val todayStart = StatsWindows.todayStartMillis(now, zone)

        timeReclaimedSeconds = db.eventDao().totalDismissedSeconds(todayStart)
        dismissedCount = db.eventDao().totalDismissedCount(todayStart)
        pausedCount = db.eventDao().totalPausedCount(todayStart)
        appCounts = db.eventDao().appOpenCounts(todayStart)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))

                // Status pill
                StatusPill(
                    protectionRunning = protectionRunning,
                    appCount = selectedAppCount
                )

                // Health banner if protection seems stopped
                if (protectionRunning && isHealthStale) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HealthBanner(onRestart = onRestart)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Time reclaimed card
                TimeReclaimedCard(seconds = timeReclaimedSeconds)

                Spacer(modifier = Modifier.height(16.dp))

                // Secondary stat
                if (pausedCount > 0) {
                    Text(
                        text = "You closed $dismissedCount of $pausedCount openings today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Per-app list
            if (appCounts.isNotEmpty()) {
                item {
                    Text(
                        text = "Today's activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(appCounts) { appCount ->
                    AppRow(
                        packageName = appCount.packageName,
                        count = appCount.count
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                item {
                    EmptyState()
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                if (protectionRunning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSnooze,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Snooze")
                        }
                        OutlinedButton(
                            onClick = onStop,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Stop")
                        }
                    }
                } else {
                    Button(
                        onClick = onStart,
                        enabled = selectedAppCount > 0,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Start protection")
                    }
                    if (selectedAppCount == 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onChooseApps,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Choose apps first")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatusPill(protectionRunning: Boolean, appCount: Int) {
    val text = if (protectionRunning) {
        "Protection is on · $appCount app${if (appCount == 1) "" else "s"} guarded"
    } else {
        "Protection is off"
    }
    val color = if (protectionRunning) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TimeReclaimedCard(seconds: Int) {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60

    val formatted = when {
        hours > 0 && minutes > 0 -> "$hours h $minutes min"
        hours > 0 -> "$hours h"
        minutes > 0 -> "$minutes min"
        seconds > 0 -> "$seconds sec"
        else -> "0 min"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Time reclaimed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatted,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AppRow(packageName: String, count: Int) {
    val context = LocalContext.current
    val pm = context.packageManager

    val appLabel = remember(packageName) {
        try {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast('.')
        }
    }

    val appIcon = remember(packageName) {
        try {
            pm.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            appIcon?.let { icon ->
                Image(
                    bitmap = icon.toBitmap(48, 48).asImageBitmap(),
                    contentDescription = "$appLabel icon",
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Text(
                text = appLabel,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "$count open${if (count == 1) "" else "s"} today",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🌱",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Start using ThumbStall to see your stats here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
