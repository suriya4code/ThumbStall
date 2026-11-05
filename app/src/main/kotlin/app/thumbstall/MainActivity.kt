package app.thumbstall

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thumbstall.ui.PermissionScreen
import app.thumbstall.ui.PermissionState

class MainActivity : ComponentActivity() {

    private var permissionState by mutableStateOf(PermissionState(false, false))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PermissionScreen(
                    state = permissionState,
                    onRequestUsageAccess = {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    onRequestOverlay = {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName"),
                            ),
                        )
                    },
                    onStartProtection = { /* wired to MonitorService in Task 4 */ },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionState = PermissionState(
            usageAccessGranted = Permissions.hasUsageAccess(this),
            overlayGranted = Permissions.hasOverlay(this),
        )
    }
}
