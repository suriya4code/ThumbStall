package app.thumbstall.overlay

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import app.thumbstall.ThumbStallApp
import app.thumbstall.core.StatsWindows
import app.thumbstall.data.AppDatabase
import app.thumbstall.data.PauseEvent
import app.thumbstall.ui.theme.Cream
import app.thumbstall.ui.theme.PauseBackground
import app.thumbstall.ui.theme.PauseTextPrimary
import app.thumbstall.ui.theme.PauseTextSecondary
import app.thumbstall.ui.theme.PauseTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId

class PauseActivity : ComponentActivity() {

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.get(this)

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
        val pauseSeconds = intent.getIntExtra(EXTRA_PAUSE_SECONDS, 8)
        val appLabel = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(targetPackage, 0),
            ).toString()
        } catch (e: Exception) {
            "app"
        }

        setContent {
            PauseTheme {
                var opensToday by remember { mutableIntStateOf(0) }
                var dismissedThisWeek by remember { mutableIntStateOf(0) }

                LaunchedEffect(Unit) {
                    val now = System.currentTimeMillis()
                    val zone = ZoneId.systemDefault()
                    val todayStart = StatsWindows.todayStartMillis(now, zone)
                    val weekStart = StatsWindows.weekStartMillis(now, zone)

                    opensToday = db.eventDao().countSince(targetPackage, PauseEvent.TYPE_PAUSED, todayStart)
                    dismissedThisWeek = db.eventDao().countSince(targetPackage, PauseEvent.TYPE_DISMISSED, weekStart)
                }

                PauseScreen(
                    appLabel = appLabel,
                    pauseSeconds = pauseSeconds,
                    opensToday = opensToday,
                    dismissedThisWeek = dismissedThisWeek,
                    onClose = {
                        logEvent(targetPackage, PauseEvent.TYPE_DISMISSED, pauseSeconds)
                        goHome()
                    },
                    onContinue = { minutes ->
                        logEvent(targetPackage, PauseEvent.TYPE_CONTINUED, pauseSeconds)
                        // Grant budget for this package
                        val budgetTracker = ThumbStallApp.get().budgetTracker
                        val durationMs = minutes * 60 * 1000L
                        budgetTracker.grant(targetPackage, durationMs, SystemClock.elapsedRealtime())
                        finish()
                    }
                )
            }
        }
    }

    private fun logEvent(packageName: String, type: String, pauseSeconds: Int) {
        lifecycleScope.launch {
            try {
                db.eventDao().insert(
                    PauseEvent(
                        packageName = packageName,
                        type = type,
                        wallTimeMs = System.currentTimeMillis(),
                        pauseSeconds = pauseSeconds
                    )
                )
            } catch (_: Throwable) {
                // fail open
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION", "MissingSuperCall")
    override fun onBackPressed() {
        // Back does nothing; escape hatches are the chips
    }

    override fun onStart() {
        super.onStart()
        showing = true
    }

    override fun onStop() {
        showing = false
        super.onStop()
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
        const val EXTRA_PAUSE_SECONDS = "pause_seconds"

        @Volatile
        var showing: Boolean = false
    }
}

@Composable
private fun PauseScreen(
    appLabel: String,
    pauseSeconds: Int,
    opensToday: Int,
    dismissedThisWeek: Int,
    onClose: () -> Unit,
    onContinue: (minutes: Int) -> Unit
) {
    var remainingSeconds by remember { mutableIntStateOf(pauseSeconds) }
    var startElapsed by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val countdownComplete = remainingSeconds <= 0

    // Countdown using elapsed time (monotonic)
    LaunchedEffect(Unit) {
        startElapsed = SystemClock.elapsedRealtime()
        while (remainingSeconds > 0) {
            delay(100)
            val elapsed = (SystemClock.elapsedRealtime() - startElapsed) / 1000
            remainingSeconds = (pauseSeconds - elapsed.toInt()).coerceAtLeast(0)
        }
    }

    // Breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Breathing phase (in/out)
    var breatheIn by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            breatheIn = !breatheIn
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PauseBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: app name + open count
            Text(
                text = appLabel,
                style = MaterialTheme.typography.titleMedium,
                color = PauseTextSecondary
            )
            if (opensToday > 0) {
                Text(
                    text = "${ordinal(opensToday)} open today",
                    style = MaterialTheme.typography.bodySmall,
                    color = PauseTextSecondary.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Breathing circle with countdown
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .scale(if (!countdownComplete) scale else 1f)
                    .clip(CircleShape)
                    .background(Color(0xFF252A1C))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (countdownComplete) "✓" else remainingSeconds.toString(),
                        fontSize = 64.sp,
                        color = PauseTextPrimary
                    )
                    if (!countdownComplete) {
                        Text(
                            text = if (breatheIn) "breathe in" else "breathe out",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PauseTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Headline
            Text(
                text = "Still want to scroll?",
                style = MaterialTheme.typography.headlineSmall,
                color = PauseTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // Continue chips
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!countdownComplete) {
                    Text(
                        text = "Open for · available in ${remainingSeconds}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = PauseTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(5, 10, 15).forEach { minutes ->
                        FilledTonalButton(
                            onClick = { onContinue(minutes) },
                            enabled = countdownComplete,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF252A1C),
                                contentColor = PauseTextPrimary,
                                disabledContainerColor = Color(0xFF1E211A),
                                disabledContentColor = PauseTextSecondary.copy(alpha = 0.5f)
                            )
                        ) {
                            Text("${minutes}m")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Close button (always enabled)
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Cream,
                    contentColor = PauseBackground
                )
            ) {
                Text(
                    text = "Close $appLabel",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom caption
            if (dismissedThisWeek > 0) {
                Text(
                    text = "You closed it instead $dismissedThisWeek time${if (dismissedThisWeek == 1) "" else "s"} this week",
                    style = MaterialTheme.typography.bodySmall,
                    color = PauseTextSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun ordinal(n: Int): String = when {
    n % 100 in 11..13 -> "${n}th"
    n % 10 == 1 -> "${n}st"
    n % 10 == 2 -> "${n}nd"
    n % 10 == 3 -> "${n}rd"
    else -> "${n}th"
}
