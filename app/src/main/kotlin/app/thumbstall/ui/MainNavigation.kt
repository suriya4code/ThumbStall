package app.thumbstall.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import app.thumbstall.R

enum class MainTab(val label: String) {
    TODAY("Today"),
    SETTINGS("Settings")
}

@Composable
fun MainNavigation(
    protectionRunning: Boolean,
    selectedAppCount: Int,
    permissionState: PermissionState,
    onChooseApps: () -> Unit,
    onSnooze: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRequestUsage: () -> Unit,
    onRequestOverlay: () -> Unit,
    onChangeIntensity: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            when (tab) {
                                MainTab.TODAY -> Icon(
                                    imageVector = if (selectedTab == index) {
                                        ImageVector.vectorResource(R.drawable.ic_today_filled)
                                    } else {
                                        ImageVector.vectorResource(R.drawable.ic_today_outline)
                                    },
                                    contentDescription = tab.label
                                )
                                MainTab.SETTINGS -> Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_settings_outline),
                                    contentDescription = tab.label
                                )
                            }
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> TodayScreen(
                protectionRunning = protectionRunning,
                selectedAppCount = selectedAppCount,
                onChooseApps = onChooseApps,
                onSnooze = onSnooze,
                onStop = onStop,
                onStart = onStart,
                modifier = Modifier.padding(innerPadding)
            )
            1 -> SettingsScreen(
                permissionState = permissionState,
                selectedAppCount = selectedAppCount,
                protectionRunning = protectionRunning,
                onChooseApps = onChooseApps,
                onRequestUsage = onRequestUsage,
                onRequestOverlay = onRequestOverlay,
                onChangeIntensity = onChangeIntensity,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
