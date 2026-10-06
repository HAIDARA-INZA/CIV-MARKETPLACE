package ci.devsphere.civmarketplace.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastMessageTime DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(conversations: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations WHERE otherUserId = :otherUserId LIMIT 1")
    suspend fun findByOtherUserId(otherUserId: Int): ConversationEntity?

    @Query("DELETE FROM conversations")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(conversations: List<ConversationEntity>) {
        clear()
        if (conversations.isNotEmpty()) upsertAll(conversations)
    }
}

