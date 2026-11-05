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
import androidx.lifecycle.lifecycleScope
import app.thumbstall.data.SettingsStore
import app.thumbstall.service.MonitorService
import app.thumbstall.ui.AppPickerScreen
import app.thumbstall.ui.PermissionScreen
import app.thumbstall.ui.PermissionState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var permissionState by mutableStateOf(PermissionState(false, false))
    private var showPicker by mutableStateOf(false)
    private var selectedPackages by mutableStateOf<Set<String>>(emptySet())
    private lateinit var settingsStore: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsStore = SettingsStore(this)

        lifecycleScope.launch {
            selectedPackages = settingsStore.targetPackages()
        }

        setContent {
            MaterialTheme {
                if (showPicker) {
                    AppPickerScreen(
                        selectedPackages = selectedPackages,
                        onSelectionChanged = { newSelection ->
                            selectedPackages = newSelection
                            lifecycleScope.launch {
                                settingsStore.setTargetPackages(newSelection)
                            }
                        },
                        onDone = { showPicker = false }
                    )
                } else {
                    PermissionScreen(
                        state = permissionState,
                        selectedAppCount = selectedPackages.size,
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
                        onChooseApps = { showPicker = true },
                        onStartProtection = { MonitorService.startMonitor(this) },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionState = PermissionState(
            usageAccessGranted = Permissions.hasUsageAccess(this),
            overlayGranted = Permissions.hasOverlay(this),
        )
        lifecycleScope.launch {
            selectedPackages = settingsStore.targetPackages()
        }
    }
}
