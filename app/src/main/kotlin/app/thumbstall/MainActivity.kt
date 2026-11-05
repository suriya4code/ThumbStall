package app.thumbstall

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import app.thumbstall.core.SnoozeCalc
import app.thumbstall.data.SettingsStore
import app.thumbstall.service.MonitorService
import app.thumbstall.service.SnoozeScheduler
import app.thumbstall.ui.AppPickerScreen
import app.thumbstall.ui.PermissionScreen
import app.thumbstall.ui.PermissionState
import app.thumbstall.ui.SnoozeSheet
import app.thumbstall.ui.theme.ThumbStallTheme
import kotlinx.coroutines.launch
import java.time.ZoneId

class MainActivity : ComponentActivity() {

    private var permissionState by mutableStateOf(PermissionState(false, false))
    private var showPicker by mutableStateOf(false)
    private var showSnooze by mutableStateOf(false)
    private var selectedPackages by mutableStateOf<Set<String>>(emptySet())
    private var protectionRunning by mutableStateOf(false)
    private lateinit var settingsStore: SettingsStore

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsStore = SettingsStore(this)

        lifecycleScope.launch {
            selectedPackages = settingsStore.targetPackages()
        }

        setContent {
            ThumbStallTheme {
                val sheetState = rememberModalBottomSheetState()
                val scope = rememberCoroutineScope()

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
                        protectionRunning = protectionRunning,
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
                        onStartProtection = {
                            MonitorService.startMonitor(this)
                            protectionRunning = true
                        },
                        onStopProtection = {
                            MonitorService.stopMonitor(this)
                            protectionRunning = false
                        },
                        onSnooze = { showSnooze = true },
                    )
                }

                if (showSnooze) {
                    ModalBottomSheet(
                        onDismissRequest = { showSnooze = false },
                        sheetState = sheetState
                    ) {
                        SnoozeSheet(
                            onSnooze = { durationMs ->
                                scope.launch {
                                    val nowElapsed = SystemClock.elapsedRealtime()
                                    val nowWall = System.currentTimeMillis()
                                    val state = SnoozeCalc.forDuration(durationMs, nowElapsed, nowWall)
                                    settingsStore.setSnooze(state.expiryWallMs)
                                    SnoozeScheduler.schedule(this@MainActivity, state.expiryWallMs)
                                    showSnooze = false
                                }
                            },
                            onUntilTonight = {
                                scope.launch {
                                    val nowElapsed = SystemClock.elapsedRealtime()
                                    val nowWall = System.currentTimeMillis()
                                    val state = SnoozeCalc.untilTonight(nowElapsed, nowWall, ZoneId.systemDefault())
                                    settingsStore.setSnooze(state.expiryWallMs)
                                    SnoozeScheduler.schedule(this@MainActivity, state.expiryWallMs)
                                    showSnooze = false
                                }
                            },
                            onTurnOff = { reason ->
                                // Log reason if needed, then stop
                                MonitorService.stopMonitor(this@MainActivity)
                                protectionRunning = false
                                showSnooze = false
                            },
                            onDismiss = { showSnooze = false }
                        )
                    }
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
        protectionRunning = MonitorService.isEnabled(this)
        lifecycleScope.launch {
            selectedPackages = settingsStore.targetPackages()
        }
    }
}
