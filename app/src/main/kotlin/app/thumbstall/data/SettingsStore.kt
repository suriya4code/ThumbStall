package app.thumbstall.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val targetPackagesKey = stringSetPreferencesKey("target_packages")
    private val intensityKey = stringPreferencesKey("intensity")

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
}
