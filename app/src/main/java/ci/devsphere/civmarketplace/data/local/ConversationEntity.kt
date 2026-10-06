package ci.devsphere.civmarketplace.data.local

import androidx.room.Entity
import ci.devsphere.civmarketplace.data.model.ConversationDto

@Entity(tableName = "conversations", primaryKeys = ["otherUserId"])
data class ConversationEntity(
    val otherUserId: Int,
    val otherUserName: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int,
    val isOnline: Boolean,
    val lastSeenAt: String?
) {
    fun toDto(): ConversationDto = ConversationDto(
        otherUserId = otherUserId,
        otherUserName = otherUserName,
        lastMessage = lastMessage,
        lastMessageTime = lastMessageTime,
        unreadCount = unreadCount,
        isOnline = isOnline,
        lastSeenAt = lastSeenAt
    )

    companion object {
        fun fromDto(conversation: ConversationDto): ConversationEntity = ConversationEntity(
            otherUserId = conversation.otherUserId,
            otherUserName = conversation.otherUserName,
            lastMessage = conversation.lastMessage,
            lastMessageTime = conversation.lastMessageTime,
            unreadCount = conversation.unreadCount,
            isOnline = conversation.isOnline,
            lastSeenAt = conversation.lastSeenAt
        )
    }
}

