package app.thumbstall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class PermissionState(
    val usageAccessGranted: Boolean,
    val overlayGranted: Boolean,
)

@Composable
fun PermissionScreen(
    state: PermissionState,
    selectedAppCount: Int = 0,
    onRequestUsageAccess: () -> Unit,
    onRequestOverlay: () -> Unit,
    onChooseApps: () -> Unit = {},
    onStartProtection: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("ThumbStall", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Grant these two permissions so ThumbStall can show a pause " +
                "screen when a distracting app opens. Nothing leaves your phone.",
            style = MaterialTheme.typography.bodyMedium,
        )

        PermissionRow(
            label = "Usage access",
            granted = state.usageAccessGranted,
            onRequest = onRequestUsageAccess,
        )
        PermissionRow(
            label = "Display over other apps",
            granted = state.overlayGranted,
            onRequest = onRequestOverlay,
        )

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onChooseApps,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (selectedAppCount == 0) "Choose apps to pause"
                else "$selectedAppCount app${if (selectedAppCount == 1) "" else "s"} selected"
            )
        }

        Button(
            onClick = onStartProtection,
            enabled = state.usageAccessGranted && state.overlayGranted && selectedAppCount > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start protection")
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onRequest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (granted) "✓ $label" else "✗ $label")
        OutlinedButton(onClick = onRequest, enabled = !granted) {
            Text(if (granted) "Granted" else "Grant")
        }
    }
}
