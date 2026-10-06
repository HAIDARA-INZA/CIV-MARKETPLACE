package ci.devsphere.civmarketplace.data.repository

import android.content.Context
import android.net.Uri
import ci.devsphere.civmarketplace.data.model.ChatMessageDto
import ci.devsphere.civmarketplace.data.model.ConversationDto
import ci.devsphere.civmarketplace.data.model.PresenceDto
import ci.devsphere.civmarketplace.data.model.MessagePageDto
import ci.devsphere.civmarketplace.data.remote.ChatService
import ci.devsphere.civmarketplace.data.remote.OpenConversationRequest
import ci.devsphere.civmarketplace.data.remote.SendMessageRequest
import ci.devsphere.civmarketplace.data.remote.TypingRequest
import ci.devsphere.civmarketplace.domain.repository.ChatRepository
import ci.devsphere.civmarketplace.util.ApiErrorMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatService: ChatService,
    @ApplicationContext private val context: Context
) : ChatRepository {

    override suspend fun getConversations(): Result<List<ConversationDto>> {
        return try {
            Result.success(chatService.getConversations())
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    override suspend fun getMessages(userId: Int, page: Int, afterId: Int?): Result<MessagePageDto> {
        return try {
            Result.success(chatService.getMessages(userId, page, afterId))
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    override suspend fun sendMessage(receiverId: Int, message: String, clientMessageId: String, productId: Int?): Result<ChatMessageDto> {
        return try {
            val response = chatService.sendMessage(SendMessageRequest(receiverId, message, clientMessageId, productId))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    override suspend fun openConversation(sellerId: Int, productId: Int?): Result<Unit> {
        return try {
            chatService.openConversation(OpenConversationRequest(sellerId, productId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    override suspend fun markDelivered(messageId: Int): Result<ChatMessageDto> {
        return try {
            Result.success(chatService.markDelivered(messageId))
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    override suspend fun markRead(messageId: Int): Result<ChatMessageDto> {
        return try {
            Result.success(chatService.markRead(messageId))
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    override suspend fun sendAttachment(
        receiverId: Int,
        message: String,
        attachmentUri: String,
        attachmentType: String
    ): Result<ChatMessageDto> {
        return try {
            val uri  = Uri.parse(attachmentUri)
            val file = getFileFromUri(uri) ?: return Result.failure(Exception("Impossible de lire le fichier"))

            val attachmentBody  = file.asRequestBody(attachmentType.toMediaTypeOrNull())
            val attachmentPart  = MultipartBody.Part.createFormData("attachment", file.name, attachmentBody)
            val receiverBody    = receiverId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val messageBody     = message.toRequestBody("text/plain".toMediaTypeOrNull())
            val attachmentTypeBody = attachmentType.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = chatService.sendMessageWithAttachment(
                receiverId     = receiverBody,
                message        = messageBody,
                attachmentType = attachmentTypeBody,
                attachment     = attachmentPart
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val file = File(context.cacheDir, "upload_file_${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return null
            file
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun sendHeartbeat(): Result<Unit> {
        return try {
            chatService.sendHeartbeat()
            Result.success(Unit)
        } catch (e: Exception) {
            // Le heartbeat échoue silencieusement (pas grave si un battement est raté,
            // pas besoin de remonter une erreur bloquante à l'UI).
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    override suspend fun sendOffline(): Result<Unit> {
        return try {
            chatService.sendOffline()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    override suspend fun getOnlineUserIds(): Result<List<Int>> {
        return try {
            Result.success(chatService.getOnlineUserIds().onlineUserIds)
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    override suspend fun getPresence(userId: Int): Result<PresenceDto> {
        return try {
            Result.success(chatService.getPresence(userId))
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    override suspend fun sendTypingStatus(receiverId: Int, isTyping: Boolean): Result<Unit> {
        return try {
            chatService.sendTypingStatus(TypingRequest(receiverId, isTyping))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    private fun handleError(e: Exception): Exception {
        if (e is HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            if (errorBody != null) {
                try {
                    val json = JSONObject(errorBody)
                    return Exception(json.getString("message"))
                } catch (_: Exception) {}
            }
        }
        return e
    }
}

