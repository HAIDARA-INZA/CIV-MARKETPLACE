package ci.devsphere.civmarketplace.data.model

import com.google.gson.annotations.SerializedName

data class SyncResponseDto(
    @SerializedName(value = "server_time", alternate = ["serverTime"])
    val serverTime: String,
    val messages: List<ChatMessageDto> = emptyList(),
    val notifications: List<NotificationDto> = emptyList(),
    @SerializedName(value = "unread_counts", alternate = ["unreadCounts"])
    val unreadCounts: UnreadCountsDto = UnreadCountsDto(),
    val conversations: List<ConversationDto> = emptyList(),
    val orders: List<OrderDto> = emptyList()
)

data class UnreadCountsDto(
    val messages: Int = 0,
    val notifications: Int = 0,
    val total: Int = 0
)

