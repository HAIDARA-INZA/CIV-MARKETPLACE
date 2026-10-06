package ci.devsphere.civmarketplace.util

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import ci.devsphere.civmarketplace.BuildConfig
import ci.devsphere.civmarketplace.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {

    @Inject
    lateinit var activeConversationTracker: ActiveConversationTracker

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val displayedToken = if (BuildConfig.DEBUG) token else "${token.take(8)}…${token.takeLast(4)}"
        Log.i("FCM", "FCM token refreshed: $displayedToken")

        com.google.firebase.messaging.FirebaseMessaging.getInstance()
            .subscribeToTopic(AppUpdateManager.FCM_TOPIC)
            .addOnFailureListener { exception ->
                Log.w("FCM", "Unable to subscribe to app updates topic", exception)
            }

        FcmTokenSyncWorker.enqueue(this, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val type = data["type"] ?: "new_message"
        Log.d(
            "FCM",
            "Message received: type=$type conversation_id=${data["conversation_id"]} message_id=${data["message_id"]}"
        )
        if (type == "chat_message" || type == "new_message") {
            val conversationId = data["conversation_id"]?.toIntOrNull() ?: 0
            val messageId = data["message_id"].orEmpty()
            if (!NotificationUtils.markChatMessageSeen(this, messageId)) {
                Log.d("FCM", "Duplicate chat push ignored: message_id=$messageId")
                return
            }
            if (activeConversationTracker.isConversationVisible(conversationId)) {
                NotificationUtils.clearChatNotifications(this, conversationId)
                Log.d("FCM", "Chat notification suppressed for active conversation=$conversationId")
                return
            }
        } else if (isAppInForeground() && type != "app_update") {
            return
        }

        if (type == "app_update") {
            val updateTitle = data["title"] ?: "Mise à jour CIV Marketplace"
            val updateBody = data["body"] ?: "Une nouvelle version est disponible."
            NotificationUtils.showAppUpdateNotification(
                context = this,
                title = updateTitle,
                body = updateBody,
                url = data["url"] ?: AppUpdateManager.LANDING_URL,
                versionCode = data["version_code"]?.toIntOrNull() ?: 0
            )
            return
        }

        val title = remoteMessage.notification?.title
            ?: data["title"]
            ?: data["sender_name"]
            ?: defaultTitle(type)
        val body = data["body"]
            ?: "Vous avez une nouvelle activite"

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (type) {
            "chat_message", "new_message" -> showChatNotification(notificationManager, title, body, data)
            else -> showMarketplaceNotification(notificationManager, title, body, type, data)
        }
    }

    private fun showChatNotification(
        notificationManager: NotificationManager,
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        val senderId = data["sender_id"]?.toIntOrNull() ?: 0
        val senderName = data["sender_name"] ?: title
        val conversationId = data["conversation_id"]?.toIntOrNull() ?: 0
        if (conversationId <= 0) {
            Log.w("FCM", "Chat push missing conversation_id")
            return
        }
        val groupKey = NotificationUtils.CHAT_GROUP_PREFIX + conversationId
        val count = NotificationUtils.incrementGroupCount(this, groupKey)
        NotificationUtils.addGroupLine(this, groupKey, "$senderName : $body")
        val pendingIntent = NotificationUtils.chatPendingIntent(
            context = this,
            senderId = senderId,
            senderName = senderName,
            conversationId = conversationId,
            productId = data["product_id"]?.toIntOrNull()
        )
        val sender = Person.Builder().setName(senderName).build()
        val recipient = Person.Builder().setName(getString(R.string.app_name)).build()
        val messagingStyle = NotificationCompat.MessagingStyle(recipient)
            .setConversationTitle(senderName)
            .addMessage(body, System.currentTimeMillis(), sender)

        val notification = NotificationCompat.Builder(this, NotificationUtils.CHAT_CHANNEL_ID)
            .setSmallIcon(R.drawable.inza)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(messagingStyle)
            .setAutoCancel(true)
            .setGroup(groupKey)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val summary = NotificationUtils.summaryNotification(
            context = this,
            channelId = NotificationUtils.CHAT_CHANNEL_ID,
            groupKey = groupKey,
            title = "$count nouveau(x) message(s)",
            lines = NotificationUtils.groupLines(this, groupKey),
            pendingIntent = pendingIntent
        )

        val messageId = data["message_id"]?.toIntOrNull() ?: System.currentTimeMillis().toInt()
        notificationManager.notify("chat_$conversationId", messageId, notification)
        notificationManager.notify("chat_summary_$conversationId", conversationId, summary)
        NotificationUtils.updateBadge(this)
    }

    private fun showMarketplaceNotification(
        notificationManager: NotificationManager,
        title: String,
        body: String,
        type: String,
        data: Map<String, String>
    ) {
        val productId = data["product_id"]?.toIntOrNull() ?: 0
        val vendorId = data["vendor_id"]?.toIntOrNull() ?: 0
        val orderId = data["order_id"]?.toIntOrNull() ?: 0
        val groupKey = NotificationUtils.MARKETPLACE_GROUP_PREFIX + type
        val count = NotificationUtils.incrementGroupCount(this, groupKey)
        NotificationUtils.addGroupLine(this, groupKey, "$title : $body")
        val pendingIntent = NotificationUtils.marketplacePendingIntent(
            context = this,
            type = type,
            productId = productId,
            vendorId = vendorId,
            orderId = orderId,
            requestCode = (type + productId + vendorId + orderId).hashCode()
        )

        val notification = NotificationCompat.Builder(this, NotificationUtils.MARKETPLACE_CHANNEL_ID)
            .setSmallIcon(R.drawable.inza)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setGroup(groupKey)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val summary = NotificationUtils.summaryNotification(
            context = this,
            channelId = NotificationUtils.MARKETPLACE_CHANNEL_ID,
            groupKey = groupKey,
            title = "$count notification(s)",
            lines = NotificationUtils.groupLines(this, groupKey),
            pendingIntent = pendingIntent
        )

        notificationManager.notify("marketplace_$type", System.currentTimeMillis().toInt(), notification)
        notificationManager.notify("marketplace_summary_$type", type.hashCode(), summary)
        NotificationUtils.updateBadge(this)
    }

    private fun defaultTitle(type: String): String {
        return when (type) {
            "app_update" -> "Mise à jour disponible"
            "new_order" -> "Nouvelle commande"
            "payment_success" -> "Paiement confirme"
            "product_offer" -> "Nouvelle offre"
            "vendor_created" -> "Nouveau vendeur"
            else -> "Notification CIV Marketplace"
        }
    }

    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get()
            .lifecycle
            .currentState
            .isAtLeast(Lifecycle.State.STARTED)
    }
}

