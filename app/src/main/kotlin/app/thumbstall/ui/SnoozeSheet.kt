package app.thumbstall.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.thumbstall.core.SnoozeCalc
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale

enum class SnoozeOption(val label: String, val durationMs: Long?) {
    FIFTEEN_MINUTES("15 minutes", 15 * 60 * 1000L),
    ONE_HOUR("1 hour", 60 * 60 * 1000L),
    UNTIL_TONIGHT("Until tonight", null) // Calculated dynamically
}

@Composable
fun SnoozeSheet(
    onSnooze: (durationMs: Long) -> Unit,
    onUntilTonight: () -> Unit,
    onTurnOff: (reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf(SnoozeOption.FIFTEEN_MINUTES) }
    var turnOffReason by remember { mutableStateOf("") }
    var turnOffCountdown by remember { mutableIntStateOf(24) }

    // Countdown for turn-off button
    LaunchedEffect(turnOffReason) {
        if (turnOffReason.isNotBlank()) {
            turnOffCountdown = 24
            while (turnOffCountdown > 0) {
                delay(1000)
                turnOffCountdown--
            }
        }
    }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val nowWall = System.currentTimeMillis()
    val nowElapsed = SystemClock.elapsedRealtime()
    val zone = ZoneId.systemDefault()

    fun resumeTimeFor(option: SnoozeOption): String {
        val expiryWallMs = when (option) {
            SnoozeOption.UNTIL_TONIGHT -> {
                SnoozeCalc.untilTonight(nowElapsed, nowWall, zone).expiryWallMs
            }
            else -> nowWall + (option.durationMs ?: 0L)
        }
        return timeFormat.format(Date(expiryWallMs))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Need a break from ThumbStall?",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Snooze it. Protection turns back on by itself, so you don't have to remember.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Radio options
            Column(modifier = Modifier.selectableGroup()) {
                SnoozeOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedOption == option,
                                onClick = { selectedOption = option },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedOption == option,
                                onClick = null
                            )
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Text(
                            text = "Back on at ${resumeTimeFor(option)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Snooze button
            Button(
                onClick = {
                    when (selectedOption) {
                        SnoozeOption.UNTIL_TONIGHT -> onUntilTonight()
                        else -> selectedOption.durationMs?.let { onSnooze(it) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Snooze for ${selectedOption.label.lowercase()}")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // Turn protection off section
            Text(
                text = "Turn protection off",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "What do you need it off for?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            OutlinedTextField(
                value = turnOffReason,
                onValueChange = { turnOffReason = it },
                placeholder = { Text("e.g. posting for work") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { onTurnOff(turnOffReason) },
                enabled = turnOffReason.isNotBlank() && turnOffCountdown == 0,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    if (turnOffReason.isBlank()) "Turn off"
                    else if (turnOffCountdown > 0) "Turn off in 0:${turnOffCountdown.toString().padStart(2, '0')}"
                    else "Turn off"
                )
            }
        }
    }
}
