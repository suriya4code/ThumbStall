package app.thumbstall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.thumbstall.R

enum class PermissionChip(val label: String) {
    NEEDED("Needed"),
    RECOMMENDED("Recommended")
}

data class PermissionCardData(
    val title: String,
    val description: String,
    val chip: PermissionChip,
    val granted: Boolean,
    val onGrant: (() -> Unit)?
)

@Composable
fun OnboardingPerms(
    state: PermissionState,
    onRequestUsage: () -> Unit,
    onRequestOverlay: () -> Unit,
    onContinue: () -> Unit
) {
    val cards = listOf(
        PermissionCardData(
            title = "Usage Access",
            description = "Lets ThumbStall see which app is in front so it knows when to show the pause screen.",
            chip = PermissionChip.NEEDED,
            granted = state.usageAccessGranted,
            onGrant = onRequestUsage
        ),
        PermissionCardData(
            title = "Display over other apps",
            description = "Lets the pause screen appear on top of distracting apps.",
            chip = PermissionChip.NEEDED,
            granted = state.overlayGranted,
            onGrant = onRequestOverlay
        ),
        PermissionCardData(
            title = "Keep running in background",
            description = "Some phone makers aggressively kill background apps. If protection stops working, check your battery settings.",
            chip = PermissionChip.RECOMMENDED,
            granted = true, // Always show as info, not a grantable permission
            onGrant = null
        )
    )

    val canContinue = state.usageAccessGranted && state.overlayGranted

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "What ThumbStall needs, and why",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            cards.forEach { card ->
                PermissionCard(card)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Trust footer
            TrustFooter()

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onContinue,
                enabled = canContinue,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (canContinue) "Continue" else "Grant permissions to continue",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(data: PermissionCardData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                ChipBadge(data.chip)
            }

            Text(
                text = data.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (data.onGrant != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = data.onGrant,
                    enabled = !data.granted,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (data.granted) "✓ Granted" else "Grant")
                }
            }
        }
    }
}

@Composable
private fun ChipBadge(chip: PermissionChip) {
    val color = when (chip) {
        PermissionChip.NEEDED -> MaterialTheme.colorScheme.error
        PermissionChip.RECOMMENDED -> MaterialTheme.colorScheme.tertiary
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = chip.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TrustFooter() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "No internet access · No account · Open source",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
