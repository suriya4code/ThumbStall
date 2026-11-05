package app.thumbstall.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EventDao {

    @Insert
    suspend fun insert(event: PauseEvent)

    @Query("SELECT COUNT(*) FROM pause_events WHERE packageName = :packageName AND type = :type AND wallTimeMs >= :sinceWallMs")
    suspend fun countSince(packageName: String, type: String, sinceWallMs: Long): Int
}
