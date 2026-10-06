package ci.devsphere.civmarketplace.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingActionDao {
    @Insert
    suspend fun insert(action: PendingActionEntity): Long

    @Query("SELECT * FROM pending_actions WHERE status IN ('pending', 'failed') ORDER BY createdAt ASC")
    suspend fun pendingForRetry(): List<PendingActionEntity>

    @Query("SELECT * FROM pending_actions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PendingActionEntity>>

    @Query("UPDATE pending_actions SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE pending_actions SET payloadJson = :payloadJson, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePayload(id: Long, payloadJson: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun delete(id: Long)
}

