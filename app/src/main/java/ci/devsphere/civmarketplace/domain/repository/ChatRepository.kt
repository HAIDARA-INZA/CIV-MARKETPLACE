package ci.devsphere.civmarketplace.domain.repository

import ci.devsphere.civmarketplace.data.model.ChatMessageDto
import ci.devsphere.civmarketplace.data.model.ConversationDto
import ci.devsphere.civmarketplace.data.model.PresenceDto
import ci.devsphere.civmarketplace.data.model.MessagePageDto

interface ChatRepository {
    suspend fun getConversations(): Result<List<ConversationDto>>
    suspend fun getMessages(userId: Int, page: Int = 1, afterId: Int? = null): Result<MessagePageDto>
    suspend fun sendMessage(receiverId: Int, message: String, clientMessageId: String, productId: Int? = null): Result<ChatMessageDto>
    suspend fun openConversation(sellerId: Int, productId: Int? = null): Result<Unit>
    suspend fun markDelivered(messageId: Int): Result<ChatMessageDto>
    suspend fun markRead(messageId: Int): Result<ChatMessageDto>
    suspend fun sendAttachment(receiverId: Int, message: String, attachmentUri: String, attachmentType: String): Result<ChatMessageDto>

    /** Bug messagerie #1 : "vu pour la dernière fois" */
    suspend fun sendHeartbeat(): Result<Unit>
    suspend fun sendOffline(): Result<Unit>
    suspend fun getOnlineUserIds(): Result<List<Int>>
    suspend fun getPresence(userId: Int): Result<PresenceDto>

    /** Bug messagerie #2 : indicateur "en train d'écrire" */
    suspend fun sendTypingStatus(receiverId: Int, isTyping: Boolean): Result<Unit>
}

