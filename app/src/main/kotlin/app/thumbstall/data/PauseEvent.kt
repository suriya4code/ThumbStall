package app.thumbstall.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records pause screen events for stats and counters.
 */
@Entity(tableName = "pause_events")
data class PauseEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val type: String,
    val wallTimeMs: Long
) {
    companion object {
        const val TYPE_PAUSED = "paused"
        const val TYPE_DISMISSED = "dismissed"
        const val TYPE_CONTINUED = "continued"
    }
}
