package ci.devsphere.civmarketplace.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import ci.devsphere.civmarketplace.data.local.PendingActionDao
import ci.devsphere.civmarketplace.data.local.PendingActionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineActionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingActionDao: PendingActionDao
) {
    suspend fun queueMessage(receiverId: Int, message: String, clientMessageId: String): Long {
        val id = pendingActionDao.insert(
            PendingActionEntity(
                type = TYPE_SEND_MESSAGE,
                payloadJson = JSONObject()
                    .put("receiver_id", receiverId)
                    .put("message", message)
                    .put("client_message_id", clientMessageId)
                    .toString()
            )
        )
        scheduleRetry()
        return id
    }

    suspend fun queueProduct(
        name: String,
        description: String,
        price: Double,
        stock: Int,
        category: String,
        type: String,
        imageUri: String
    ): Long {
        val id = pendingActionDao.insert(
            PendingActionEntity(
                type = TYPE_CREATE_PRODUCT,
                payloadJson = JSONObject()
                    .put("name", name)
                    .put("description", description)
                    .put("price", price)
                    .put("stock", stock)
                    .put("category", category)
                    .put("type", if (type.equals("service", true)) "service" else "product")
                    .put("image_uri", imageUri)
                    .toString()
            )
        )
        scheduleRetry()
        return id
    }

    fun scheduleRetry() {
        val request = OneTimeWorkRequestBuilder<PendingActionWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val TYPE_SEND_MESSAGE = "send_message"
        const val TYPE_CREATE_PRODUCT = "create_product"
        private const val WORK_NAME = "pending_actions_retry"
    }
}

