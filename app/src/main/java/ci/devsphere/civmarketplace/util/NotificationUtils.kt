package ci.devsphere.civmarketplace.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import ci.devsphere.civmarketplace.MainActivity
import org.json.JSONArray

object NotificationUtils {
    const val CHAT_CHANNEL_ID = "chat_notifications"
    const val CHAT_CHANNEL_NAME = "Messages"
    const val MARKETPLACE_CHANNEL_ID = "marketplace_notifications"
    const val MARKETPLACE_CHANNEL_NAME = "Commandes et offres"
    const val APP_UPDATE_CHANNEL_ID = "app_update_notifications"
    const val APP_UPDATE_CHANNEL_NAME = "Mises à jour"

    const val CHAT_GROUP_PREFIX = "group_chat_"
    const val MARKETPLACE_GROUP_PREFIX = "group_marketplace_"

    private const val ACTION_OPEN_CHAT = "ci.devsphere.civmarketplace.OPEN_CHAT"
    private const val ACTION_OPEN_NOTIFICATION = "ci.devsphere.civmarketplace.OPEN_NOTIFICATION"
    private const val PREFS_NAME = "notification_counters"
    private const val CHAT_SEEN_IDS_KEY = "chat_seen_message_ids"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chatChannel = NotificationChannel(
                CHAT_CHANNEL_ID,
                CHAT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour les nouveaux messages"
                enableLights(true)
                enableVibration(true)
            }

            val marketplaceChannel = NotificationChannel(
                MARKETPLACE_CHANNEL_ID,
                MARKETPLACE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour les commandes, offres et activités marketplace"
                enableLights(true)
                enableVibration(true)
            }

            val appUpdateChannel = NotificationChannel(
                APP_UPDATE_CHANNEL_ID,
                APP_UPDATE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nouvelles versions de CIV Marketplace"
                enableLights(true)
                enableVibration(true)
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            manager.createNotificationChannel(chatChannel)
            manager.createNotificationChannel(marketplaceChannel)
            manager.createNotificationChannel(appUpdateChannel)
        }
    }

    fun showAppUpdateNotification(
        context: Context,
        title: String,
        body: String,
        url: String,
        versionCode: Int
    ) {
        val safeUrl = url.takeIf { it.startsWith("https://") }
            ?: AppUpdateManager.LANDING_URL
        val pendingIntent = PendingIntent.getActivity(
            context,
            "app_update_$versionCode".hashCode(),
            Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, APP_UPDATE_CHANNEL_ID)
            .setSmallIcon(ci.devsphere.civmarketplace.R.drawable.inza)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify("app_update", versionCode, notification)
    }

    fun chatPendingIntent(
        context: Context,
        senderId: Int,
        senderName: String,
        conversationId: Int,
        productId: Int? = null
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_CHAT

            putExtra("chat_user_id", senderId)
            putExtra("chat_user_name", senderName)
            putExtra("conversation_id", conversationId)
            putExtra("product_id", productId ?: 0)

            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        return PendingIntent.getActivity(
            context,
            conversationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun clearChatNotifications(context: Context, conversationId: Int) {
        if (conversationId <= 0) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val tags = setOf("chat_$conversationId", "chat_summary_$conversationId")
        manager.activeNotifications
            .filter { it.tag in tags }
            .forEach { manager.cancel(it.tag, it.id) }
        clearGroup(context, CHAT_GROUP_PREFIX + conversationId)
    }

    fun markChatMessageSeen(context: Context, messageId: String): Boolean {
        if (messageId.isBlank()) return true

        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val seenIds = LinkedHashSet(preferences.getStringSet(CHAT_SEEN_IDS_KEY, emptySet()).orEmpty())
        if (!seenIds.add(messageId)) return false
        while (seenIds.size > 200) {
            seenIds.remove(seenIds.first())
        }
        preferences.edit().putStringSet(CHAT_SEEN_IDS_KEY, seenIds).apply()
        return true
    }

    fun marketplacePendingIntent(
        context: Context,
        type: String,
        productId: Int = 0,
        vendorId: Int = 0,
        orderId: Int = 0,
        requestCode: Int = type.hashCode()
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_NOTIFICATION
            putExtra("notification_type", type)
            putExtra("product_id", productId)
            putExtra("vendor_id", vendorId)
            putExtra("order_id", orderId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun incrementGroupCount(context: Context, groupKey: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val next = prefs.getInt(groupKey, 0) + 1
        prefs.edit().putInt(groupKey, next).apply()
        return next
    }

    fun addGroupLine(context: Context, groupKey: String, line: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lines = runCatching { JSONArray(prefs.getString("${groupKey}_lines", "[]")) }
            .getOrDefault(JSONArray())
        lines.put(line)
        while (lines.length() > 20) lines.remove(0)
        prefs.edit().putString("${groupKey}_lines", lines.toString()).apply()
    }

    fun groupLines(context: Context, groupKey: String): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lines = runCatching { JSONArray(prefs.getString("${groupKey}_lines", "[]")) }
            .getOrDefault(JSONArray())
        return (0 until lines.length()).mapNotNull { lines.optString(it).takeIf(String::isNotBlank) }
    }

    fun clearGroup(context: Context, groupKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(groupKey)
            .remove("${groupKey}_lines")
            .apply()
        updateBadge(context)
    }

    fun setBadgeCount(context: Context, count: Int) {
        runCatching {
            me.leolin.shortcutbadger.ShortcutBadger.applyCount(context, count.coerceAtLeast(0))
        }
    }

    fun updateBadge(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val total = prefs.all
            .filterKeys { it.startsWith(CHAT_GROUP_PREFIX) || it.startsWith(MARKETPLACE_GROUP_PREFIX) }
            .filterKeys { !it.endsWith("_lines") }
            .values
            .sumOf { (it as? Int) ?: 0 }
        setBadgeCount(context, total)
    }

    fun clearBadge(context: Context) {
        runCatching {
            me.leolin.shortcutbadger.ShortcutBadger.removeCount(context)
        }
    }

    fun summaryNotification(
        context: Context,
        channelId: String,
        groupKey: String,
        title: String,
        lines: List<String>,
        pendingIntent: PendingIntent
    ) = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(ci.devsphere.civmarketplace.R.drawable.inza)
        .setContentTitle(title)
        .setContentText(lines.firstOrNull().orEmpty())
        .setStyle(
            NotificationCompat.InboxStyle().also { style ->
                lines.takeLast(5).forEach(style::addLine)
                style.setSummaryText("${lines.size} nouvelle(s)")
            }
        )
        .setGroup(groupKey)
        .setGroupSummary(true)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()
}

