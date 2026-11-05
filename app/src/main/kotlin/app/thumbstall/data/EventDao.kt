package app.thumbstall.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

data class AppOpenCount(
    val packageName: String,
    val count: Int
)

@Dao
interface EventDao {

    @Insert
    suspend fun insert(event: PauseEvent)

    @Query("SELECT COUNT(*) FROM pause_events WHERE packageName = :packageName AND type = :type AND wallTimeMs >= :sinceWallMs")
    suspend fun countSince(packageName: String, type: String, sinceWallMs: Long): Int

    @Query("SELECT COALESCE(SUM(pauseSeconds), 0) FROM pause_events WHERE type = 'dismissed' AND wallTimeMs >= :sinceWallMs")
    suspend fun totalDismissedSeconds(sinceWallMs: Long): Int

    @Query("SELECT COUNT(*) FROM pause_events WHERE type = 'dismissed' AND wallTimeMs >= :sinceWallMs")
    suspend fun totalDismissedCount(sinceWallMs: Long): Int

    @Query("SELECT COUNT(*) FROM pause_events WHERE type = 'paused' AND wallTimeMs >= :sinceWallMs")
    suspend fun totalPausedCount(sinceWallMs: Long): Int

    @Query("SELECT packageName, COUNT(*) as count FROM pause_events WHERE type = 'paused' AND wallTimeMs >= :sinceWallMs GROUP BY packageName ORDER BY count DESC")
    suspend fun appOpenCounts(sinceWallMs: Long): List<AppOpenCount>
}
