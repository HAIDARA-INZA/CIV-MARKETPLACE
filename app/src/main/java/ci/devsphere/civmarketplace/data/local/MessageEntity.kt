package ci.devsphere.civmarketplace.data.local

import androidx.room.Entity
import ci.devsphere.civmarketplace.data.model.ChatMessageDto

@Entity(tableName = "messages")
data class MessageEntity(
    @androidx.room.PrimaryKey val localKey: String,
    val id: Int,
    val conversationUserId: Int,
    val senderId: Int,
    val receiverId: Int,
    val clientMessageId: String,
    val message: String?,
    val timestamp: String,
    val isRead: Boolean,
    val status: String?,
    val deliveredAt: String?,
    val readAt: String?,
    val attachmentUrl: String?,
    val attachmentType: String?,
    val attachmentName: String?
) {
    fun toDto() = ChatMessageDto(
        id = id,
        senderId = senderId,
        receiverId = receiverId,
        clientMessageId = clientMessageId,
        message = message,
        timestamp = timestamp,
        isRead = isRead,
        status = status,
        deliveredAt = deliveredAt,
        readAt = readAt,
        attachmentUrl = attachmentUrl,
        attachmentType = attachmentType,
        attachmentName = attachmentName
    )

    companion object {
        fun fromDto(message: ChatMessageDto, conversationUserId: Int): MessageEntity {
            val clientId = message.clientMessageId ?: "server-${message.id}"
            return MessageEntity(
                localKey = clientId,
                id = message.id,
                conversationUserId = conversationUserId,
                senderId = message.senderId,
                receiverId = message.receiverId,
                clientMessageId = clientId,
                message = message.message,
                timestamp = message.timestamp,
                isRead = message.isRead,
                status = message.status,
                deliveredAt = message.deliveredAt,
                readAt = message.readAt,
                attachmentUrl = message.attachmentUrl,
                attachmentType = message.attachmentType,
                attachmentName = message.attachmentName
            )
        }
    }
}

