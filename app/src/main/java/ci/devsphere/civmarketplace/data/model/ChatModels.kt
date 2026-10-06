package ci.devsphere.civmarketplace.data.model

import com.google.gson.annotations.SerializedName

data class ChatMessageDto(
    val id: Int,
    @SerializedName(value = "sender_id", alternate = ["senderId"])
    val senderId: Int,
    @SerializedName(value = "receiver_id", alternate = ["receiverId"])
    val receiverId: Int,
    @SerializedName(value = "client_message_id", alternate = ["clientMessageId"])
    val clientMessageId: String? = null,
    val message: String? = null,
    @SerializedName(value = "created_at", alternate = ["timestamp"])
    val timestamp: String,
    @SerializedName(value = "is_read", alternate = ["isRead"])
    val isRead: Boolean = false,
    val status: String? = null,
    @SerializedName(value = "delivered_at", alternate = ["deliveredAt"])
    val deliveredAt: String? = null,
    @SerializedName(value = "read_at", alternate = ["readAt"])
    val readAt: String? = null,
    @SerializedName(value = "edited_at", alternate = ["editedAt"])
    val editedAt: String? = null,
    @SerializedName(value = "deleted_at", alternate = ["deletedAt"])
    val deletedAt: String? = null,
    @SerializedName(value = "attachment_url", alternate = ["attachmentUrl"])
    val attachmentUrl: String? = null,
    @SerializedName(value = "attachment_type", alternate = ["attachmentType"])
    val attachmentType: String? = null,
    @SerializedName(value = "attachment_name", alternate = ["attachmentName"])
    val attachmentName: String? = null,
    @SerializedName(value = "product_id", alternate = ["productId"])
    val productId: Int? = null,
    @SerializedName(value = "product_name", alternate = ["productName"])
    val productName: String? = null,
    @SerializedName(value = "product_image_url", alternate = ["productImageUrl"])
    val productImageUrl: String? = null
) {
    fun matchesProductFilter(productIdToMatch: Int?): Boolean =
        productIdToMatch == null || productId == null || productId == productIdToMatch

    fun hasProductContext(): Boolean = productId != null
}

data class ConversationDto(
    @SerializedName(value = "other_user_id", alternate = ["otherUserId", "user_id"])
    val otherUserId: Int,
    @SerializedName(value = "other_user_name", alternate = ["otherUserName", "name"])
    val otherUserName: String,
    @SerializedName(value = "last_message", alternate = ["lastMessage"])
    val lastMessage: String,
    @SerializedName(value = "last_message_time", alternate = ["lastMessageTime", "created_at"])
    val lastMessageTime: String,
    @SerializedName(value = "unread_count", alternate = ["unreadCount"])
    val unreadCount: Int,
    @SerializedName(value = "is_online", alternate = ["isOnline"])
    val isOnline: Boolean = false,
    @SerializedName(value = "last_seen_at", alternate = ["lastSeenAt"])
    val lastSeenAt: String? = null
)

data class MessagePageDto(
    val messages: List<ChatMessageDto>,
    @SerializedName(value = "current_page", alternate = ["currentPage"])
    val currentPage: Int = 1,
    @SerializedName(value = "last_page", alternate = ["lastPage"])
    val lastPage: Int = 1,
    @SerializedName(value = "has_more", alternate = ["hasMore"])
    val hasMore: Boolean = false
)

data class PresenceDto(
    @SerializedName(value = "user_id", alternate = ["userId"])
    val userId: Int,
    @SerializedName(value = "is_online", alternate = ["isOnline"])
    val isOnline: Boolean = false,
    @SerializedName(value = "last_seen_at", alternate = ["lastSeenAt"])
    val lastSeenAt: String? = null
)

