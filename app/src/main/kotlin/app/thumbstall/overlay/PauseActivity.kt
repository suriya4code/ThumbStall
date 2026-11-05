package app.thumbstall.overlay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Crude Phase 0 pause overlay. Covers the target app with a scrim and a
 * single "Close <app>" button that sends the user home.
 */
class PauseActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
        val appLabel = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(targetPackage, 0),
            ).toString()
        } catch (e: Exception) {
            "app"
        }
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xE6102010))
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "ThumbStall",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                    )
                    Button(
                        onClick = ::goHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                    ) {
                        Text("Close $appLabel")
                    }
                }
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION", "MissingSuperCall")
    override fun onBackPressed() {
        // Phase 0: back does nothing; escape hatches arrive in Phase 2.
    }

    override fun onStart() {
        super.onStart()
        showing = true
    }

    override fun onStop() {
        showing = false
        super.onStop()
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"

        @Volatile
        var showing: Boolean = false
    }
}
