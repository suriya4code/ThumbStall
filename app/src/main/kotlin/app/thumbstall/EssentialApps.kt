package app.thumbstall

import android.content.Context
import android.provider.Telephony
import android.telecom.TelecomManager

/**
 * Resolves essential apps that should never be paused.
 * Combines system defaults with a static list of common utilities.
 */
object EssentialApps {

    private val staticEssentials = setOf(
        "com.google.android.dialer",
        "com.android.dialer",
        "com.android.phone",
        "com.google.android.apps.maps",
        "com.google.android.GoogleCamera",
        "com.google.android.apps.authenticator2",
        "com.android.settings"
    )

    fun resolve(context: Context): Set<String> = buildSet {
        // Own package
        add(context.packageName)

        // Default dialer
        val telecom = context.getSystemService(TelecomManager::class.java)
        telecom?.defaultDialerPackage?.let { add(it) }

        // Default SMS app
        Telephony.Sms.getDefaultSmsPackage(context)?.let { add(it) }

        // Static list
        addAll(staticEssentials)
    }
}
