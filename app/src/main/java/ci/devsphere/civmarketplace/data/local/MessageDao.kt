package ci.devsphere.civmarketplace.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationUserId = :conversationUserId ORDER BY timestamp ASC")
    fun observeConversation(conversationUserId: Int): Flow<List<MessageEntity>>

    @Query("SELECT COALESCE(MAX(id), 0) FROM messages WHERE conversationUserId = :conversationUserId AND id > 0")
    suspend fun latestServerMessageId(conversationUserId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE clientMessageId = :clientMessageId LIMIT 1")
    suspend fun findByClientMessageId(clientMessageId: String): MessageEntity?

    @Query("""
        UPDATE messages
        SET deliveredAt = COALESCE(:deliveredAt, deliveredAt),
            readAt = COALESCE(:readAt, readAt),
            isRead = CASE WHEN :readAt IS NOT NULL THEN 1 ELSE isRead END,
            status = COALESCE(:status, status)
        WHERE clientMessageId = :clientMessageId OR id = :messageId
    """)
    suspend fun updateDeliveryStatus(
        messageId: Int,
        clientMessageId: String?,
        deliveredAt: String?,
        readAt: String?,
        status: String?
    )
}

