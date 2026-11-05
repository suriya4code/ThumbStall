package app.thumbstall.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thumbstall.R

data class OemInstructions(
    val manufacturer: String,
    val steps: List<String>,
    val note: String? = null
)

private val oemInstructions = mapOf(
    "samsung" to OemInstructions(
        manufacturer = "Samsung",
        steps = listOf(
            "Open Settings → Apps → ThumbStall",
            "Tap Battery → Allow background activity",
            "Also check Settings → Battery → Background usage limits"
        ),
        note = "Samsung's battery optimization is aggressive. If protection keeps stopping, check both settings."
    ),
    "xiaomi" to OemInstructions(
        manufacturer = "Xiaomi / Redmi / POCO",
        steps = listOf(
            "Open Settings → Apps → Manage apps → ThumbStall",
            "Tap Autostart → Enable",
            "Tap Battery saver → No restrictions",
            "Also: Security app → Permissions → Autostart → Enable ThumbStall"
        ),
        note = "Xiaomi requires both autostart AND battery saver settings."
    ),
    "oneplus" to OemInstructions(
        manufacturer = "OnePlus",
        steps = listOf(
            "Open Settings → Battery → Battery optimization",
            "Find ThumbStall → Don't optimize",
            "Also check Settings → Apps → ThumbStall → Battery → Allow background activity"
        )
    ),
    "huawei" to OemInstructions(
        manufacturer = "Huawei / Honor",
        steps = listOf(
            "Open Settings → Apps → Apps",
            "Find ThumbStall → Battery → App launch → Manage manually",
            "Enable: Auto-launch, Secondary launch, Run in background"
        ),
        note = "Huawei's app launch management is very strict."
    ),
    "oppo" to OemInstructions(
        manufacturer = "OPPO / Realme",
        steps = listOf(
            "Open Settings → Battery → More settings",
            "Disable: Optimize battery use for ThumbStall",
            "Also check App management → App auto-start"
        )
    ),
    "vivo" to OemInstructions(
        manufacturer = "Vivo",
        steps = listOf(
            "Open Settings → Battery → High background power consumption",
            "Enable ThumbStall",
            "Also: Settings → Apps → Autostart → Enable ThumbStall"
        )
    )
)

private val fallbackInstructions = OemInstructions(
    manufacturer = "Other devices",
    steps = listOf(
        "Open Settings → Apps → ThumbStall → Battery",
        "Select 'Unrestricted' or 'Don't optimize'",
        "Check for any 'Battery saver' or 'Power saving' app list"
    ),
    note = "Settings vary by manufacturer. Search for 'dontkillmyapp.com' for device-specific guides."
)

@Composable
fun OemGuide(
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val manufacturer = Build.MANUFACTURER.lowercase()
    val instructions = oemInstructions.entries
        .firstOrNull { manufacturer.contains(it.key) }
        ?.value
        ?: fallbackInstructions

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Keep protection running",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (expanded) "Tap to collapse" else "Tap for ${instructions.manufacturer} guide",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = instructions.manufacturer,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    instructions.steps.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. $step",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                        )
                    }

                    instructions.note?.let { note ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 $note",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
