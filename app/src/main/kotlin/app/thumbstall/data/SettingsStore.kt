package app.thumbstall.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val targetPackagesKey = stringSetPreferencesKey("target_packages")
    private val intensityKey = stringPreferencesKey("intensity")
    private val snoozeExpiryKey = longPreferencesKey("snooze_expiry_wall_ms")

    suspend fun targetPackages(): Set<String> =
        context.dataStore.data.map { it[targetPackagesKey] ?: emptySet() }.first()

    suspend fun setTargetPackages(pkgs: Set<String>) {
        context.dataStore.edit { it[targetPackagesKey] = pkgs }
    }

    suspend fun intensity(): String =
        context.dataStore.data.map { it[intensityKey] ?: "balanced" }.first()

    suspend fun setIntensity(intensity: String) {
        context.dataStore.edit { it[intensityKey] = intensity }
    }

    /**
     * Returns the snooze expiry wall time in ms, or null if not set/expired.
     */
    suspend fun snoozeExpiryWallMs(): Long? {
        val expiry = context.dataStore.data.map { it[snoozeExpiryKey] }.first() ?: return null
        return if (expiry > System.currentTimeMillis()) expiry else null
    }

    /**
     * Sets snooze to expire at [expiryWallMs].
     */
    suspend fun setSnooze(expiryWallMs: Long) {
        context.dataStore.edit { it[snoozeExpiryKey] = expiryWallMs }
    }

    /**
     * Clears the snooze.
     */
    suspend fun clearSnooze() {
        context.dataStore.edit { it.remove(snoozeExpiryKey) }
    }
}
