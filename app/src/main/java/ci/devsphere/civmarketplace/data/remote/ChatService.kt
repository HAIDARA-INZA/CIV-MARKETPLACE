package ci.devsphere.civmarketplace.data.remote

import ci.devsphere.civmarketplace.data.model.ChatMessageDto
import ci.devsphere.civmarketplace.data.model.ConversationDto
import ci.devsphere.civmarketplace.data.model.PresenceDto
import ci.devsphere.civmarketplace.data.model.MessagePageDto
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatService {

    /** Liste des conversations de l'utilisateur */
    @GET("conversations")
    suspend fun getConversations(): List<ConversationDto>

    /** Messages d'une conversation — route réelle du backend */
    @GET("conversations/{userId}")
    suspend fun getMessages(
        @Path("userId") userId: Int,
        @retrofit2.http.Query("page") page: Int = 1,
        @retrofit2.http.Query("after_id") afterId: Int? = null
    ): MessagePageDto

    /** Ouvre ou récupère une conversation avec un vendeur en gardant le contexte produit. */
    @POST("conversations/open")
    suspend fun openConversation(@Body request: OpenConversationRequest)

    /** Envoyer un message texte */
    @POST("messages/send")
    suspend fun sendMessage(@Body request: SendMessageRequest): ChatMessageDto

    @PATCH("messages/{id}/delivered")
    suspend fun markDelivered(@Path("id") messageId: Int): ChatMessageDto

    @PATCH("messages/{id}/read")
    suspend fun markRead(@Path("id") messageId: Int): ChatMessageDto

    /** Envoyer un message avec pièce jointe */
    @Multipart
    @POST("messages/send")
    suspend fun sendMessageWithAttachment(
        @Part("receiver_id") receiverId: RequestBody,
        @Part("message") message: RequestBody,
        @Part("attachment_type") attachmentType: RequestBody,
        @Part attachment: MultipartBody.Part
    ): ChatMessageDto

    /** Bug messagerie #1 : "vu pour la dernière fois" — appelé régulièrement tant que l'app est ouverte */
    @POST("presence/heartbeat")
    suspend fun sendHeartbeat()

    @POST("presence/offline")
    suspend fun sendOffline()

    /** Snapshot des utilisateurs en ligne au moment de l'appel (avant que le presence channel ne soit à jour) */
    @GET("presence/online")
    suspend fun getOnlineUserIds(): OnlineUsersResponse

    @GET("presence/{userId}")
    suspend fun getPresence(@Path("userId") userId: Int): PresenceDto

    /** Bug messagerie #2 : indicateur "en train d'écrire" */
    @POST("typing")
    suspend fun sendTypingStatus(@Body request: TypingRequest)
}

data class SendMessageRequest(
    @SerializedName("receiver_id")
    val receiverId: Int,
    val message: String,
    @SerializedName("client_message_id")
    val clientMessageId: String = java.util.UUID.randomUUID().toString(),
    @SerializedName("product_id")
    val productId: Int? = null
)

data class OpenConversationRequest(
    @SerializedName("seller_id")
    val sellerId: Int,
    @SerializedName("product_id")
    val productId: Int? = null
)

data class TypingRequest(
    @SerializedName("receiver_id")
    val receiverId: Int,
    @SerializedName("is_typing")
    val isTyping: Boolean
)

data class OnlineUsersResponse(
    @SerializedName("online_user_ids")
    val onlineUserIds: List<Int>
)

