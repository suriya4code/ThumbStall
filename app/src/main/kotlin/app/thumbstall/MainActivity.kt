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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import app.thumbstall.core.SnoozeCalc
import app.thumbstall.data.SettingsStore
import app.thumbstall.service.MonitorService
import app.thumbstall.service.SnoozeScheduler
import app.thumbstall.ui.AppPickerScreen
import app.thumbstall.ui.MainNavigation
import app.thumbstall.ui.OnboardingIntensity
import app.thumbstall.ui.OnboardingPerms
import app.thumbstall.ui.PermissionState
import app.thumbstall.ui.SnoozeSheet
import app.thumbstall.ui.theme.ThumbStallTheme
import kotlinx.coroutines.launch
import java.time.ZoneId

enum class AppScreen {
    LOADING,
    ONBOARDING_INTENSITY,
    ONBOARDING_PERMS,
    MAIN,
    APP_PICKER
}

class MainActivity : ComponentActivity() {

    private var currentScreen by mutableStateOf(AppScreen.LOADING)
    private var permissionState by mutableStateOf(PermissionState(false, false))
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
            currentScreen = if (settingsStore.isOnboardingComplete()) {
                AppScreen.MAIN
            } else {
                AppScreen.ONBOARDING_INTENSITY
            }
        }

        setContent {
            ThumbStallTheme {
                val sheetState = rememberModalBottomSheetState()
                val scope = rememberCoroutineScope()

                when (currentScreen) {
                    AppScreen.LOADING -> {
                        // Show nothing while loading
                    }

                    AppScreen.ONBOARDING_INTENSITY -> {
                        OnboardingIntensity(
                            onSelect = { intensity ->
                                lifecycleScope.launch {
                                    settingsStore.setIntensity(intensity)
                                }
                                currentScreen = AppScreen.ONBOARDING_PERMS
                            }
                        )
                    }

                    AppScreen.ONBOARDING_PERMS -> {
                        OnboardingPerms(
                            state = permissionState,
                            onRequestUsage = {
                                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            },
                            onRequestOverlay = {
                                startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:$packageName")
                                    )
                                )
                            },
                            onContinue = {
                                lifecycleScope.launch {
                                    settingsStore.setOnboardingComplete()
                                }
                                currentScreen = AppScreen.MAIN
                            }
                        )
                    }

                    AppScreen.MAIN -> {
                        MainNavigation(
                            protectionRunning = protectionRunning,
                            selectedAppCount = selectedPackages.size,
                            permissionState = permissionState,
                            onChooseApps = { currentScreen = AppScreen.APP_PICKER },
                            onSnooze = { showSnooze = true },
                            onStart = {
                                MonitorService.startMonitor(this@MainActivity)
                                protectionRunning = true
                            },
                            onStop = {
                                MonitorService.stopMonitor(this@MainActivity)
                                protectionRunning = false
                            },
                            onRequestUsage = {
                                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            },
                            onRequestOverlay = {
                                startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:$packageName")
                                    )
                                )
                            },
                            onChangeIntensity = {
                                // For now, just go back to intensity picker
                                // Could be a dialog in future
                                currentScreen = AppScreen.ONBOARDING_INTENSITY
                            }
                        )
                    }

                    AppScreen.APP_PICKER -> {
                        AppPickerScreen(
                            selectedPackages = selectedPackages,
                            onSelectionChanged = { newSelection ->
                                selectedPackages = newSelection
                                lifecycleScope.launch {
                                    settingsStore.setTargetPackages(newSelection)
                                }
                            },
                            onDone = { currentScreen = AppScreen.MAIN }
                        )
                    }
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
