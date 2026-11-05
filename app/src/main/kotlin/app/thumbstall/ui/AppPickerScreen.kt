package app.thumbstall.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable
)

@Composable
fun AppPickerScreen(
    selectedPackages: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager

    val apps = remember {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val essentials = app.thumbstall.EssentialApps.resolve(context)

        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .asSequence()
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it !in essentials }
            .map { pkg ->
                val appInfo = pm.getApplicationInfo(pkg, 0)
                AppInfo(
                    packageName = pkg,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo)
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    val selection = remember { mutableStateOf(selectedPackages) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Choose apps to pause",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    val checked = app.packageName in selection.value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selection.value = if (checked) {
                                    selection.value - app.packageName
                                } else {
                                    selection.value + app.packageName
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = app.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                selection.value = if (it) {
                                    selection.value + app.packageName
                                } else {
                                    selection.value - app.packageName
                                }
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    onSelectionChanged(selection.value)
                    onDone()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Done (${selection.value.size} selected)")
            }
        }
    }
}
