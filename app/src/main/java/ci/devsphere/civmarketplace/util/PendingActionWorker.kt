package ci.devsphere.civmarketplace.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ci.devsphere.civmarketplace.data.local.PendingActionDao
import ci.devsphere.civmarketplace.data.remote.ChatService
import ci.devsphere.civmarketplace.data.remote.SendMessageRequest
import ci.devsphere.civmarketplace.di.PendingActionStatusBus
import ci.devsphere.civmarketplace.domain.repository.ProductRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.UUID

class PendingActionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            PendingActionWorkerEntryPoint::class.java
        )

        val dao = entryPoint.pendingActionDao()
        val actions = dao.pendingForRetry()
        var retryRequested = false

        for (action in actions) {
            val actionJson = JSONObject(action.payloadJson)
            var clientMessageId = actionJson.optString("client_message_id").takeIf { it.isNotBlank() }
            if (action.type == OfflineActionManager.TYPE_SEND_MESSAGE && clientMessageId == null) {
                clientMessageId = UUID.randomUUID().toString()
                actionJson.put("client_message_id", clientMessageId)
                dao.updatePayload(action.id, actionJson.toString())
            }
            dao.updateStatus(action.id, "sending")
            PendingActionStatusBus.emit(action.id, action.type, "sending", clientMessageId = clientMessageId)

            var serverMessage: Any? = null
            var temporaryFailure = false
            try {
                serverMessage = when (action.type) {
                    OfflineActionManager.TYPE_SEND_MESSAGE -> {
                        val json = actionJson
                        entryPoint.chatService().sendMessage(
                            SendMessageRequest(
                                receiverId = json.getInt("receiver_id"),
                                message = json.getString("message"),
                                clientMessageId = json.getString("client_message_id")
                            )
                        )
                    }
                    OfflineActionManager.TYPE_CREATE_PRODUCT -> {
                        val json = JSONObject(action.payloadJson)
                        entryPoint.productRepository().createProduct(
                            name = json.getString("name"),
                            description = json.getString("description"),
                            price = json.getDouble("price"),
                            stock = json.getInt("stock"),
                            category = json.getString("category"),
                            type = json.optString("type", "product"),
                            imageUri = json.getString("image_uri")
                        ).getOrThrow()
                    }
                    else -> Unit
                }
            } catch (exception: Exception) {
                temporaryFailure = exception is IOException || exception is SocketTimeoutException
            }

            if (serverMessage != null) {
                dao.delete(action.id)
                PendingActionStatusBus.emit(
                    action.id,
                    action.type,
                    "sent",
                    serverMessage as? ci.devsphere.civmarketplace.data.model.ChatMessageDto,
                    clientMessageId
                )
            } else if (temporaryFailure) {
                dao.updateStatus(action.id, "pending")
                PendingActionStatusBus.emit(action.id, action.type, "pending", clientMessageId = clientMessageId)
                retryRequested = true
            } else {
                dao.updateStatus(action.id, "failed")
                PendingActionStatusBus.emit(action.id, action.type, "failed", clientMessageId = clientMessageId)
            }
        }

        return if (retryRequested) Result.retry() else Result.success()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PendingActionWorkerEntryPoint {
    fun pendingActionDao(): PendingActionDao
    fun chatService(): ChatService
    fun productRepository(): ProductRepository
}

