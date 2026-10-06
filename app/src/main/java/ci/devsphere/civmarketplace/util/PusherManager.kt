package ci.devsphere.civmarketplace.util

import android.util.Log
import ci.devsphere.civmarketplace.data.local.TokenManager
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.Channel
import com.pusher.client.channel.PresenceChannel
import com.pusher.client.channel.PresenceChannelEventListener
import com.pusher.client.channel.PrivateChannel
import com.pusher.client.channel.PrivateChannelEventListener
import com.pusher.client.channel.PusherEvent
import com.pusher.client.channel.SubscriptionEventListener
import com.pusher.client.channel.User
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange
import com.pusher.client.util.HttpAuthorizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PusherManager @Inject constructor(
    private val tokenManager: TokenManager
) {
    companion object {
        internal fun isDuplicateBinding(
            channelName: String,
            eventName: String,
            registry: Map<String, Map<String, *>>
        ): Boolean {
            return registry[channelName]?.containsKey(eventName) == true
        }
    }

    private var pusher: Pusher? = null

    private val privateChatChannels = mutableMapOf<String, PrivateChannel>()
    private val privateConversationChannels = mutableMapOf<String, PrivateChannel>()
    private val privatePresenceChannels = mutableMapOf<String, PresenceChannel>()
    private val boundPrivateChatChannels = mutableSetOf<String>()
    private val boundEventsByChannel = mutableMapOf<String, MutableMap<String, Any>>()
    private var presenceChannel: PresenceChannel? = null

    private fun privateChannelListener(channelName: String): PrivateChannelEventListener {
        return object : PrivateChannelEventListener {
            override fun onEvent(event: PusherEvent?) {
                Log.d("PusherManager", "event received: $channelName / ${event?.eventName ?: "unknown"}")
            }

            override fun onAuthenticationFailure(message: String?, e: Exception?) {
                Log.e("PusherManager", "private channel auth failed for $channelName: $message", e)
            }

            override fun onSubscriptionSucceeded(channelName: String?) {
                Log.d("PusherManager", "subscribed to private channel: $channelName")
            }
        }
    }

    private fun privateEventListener(
        channelName: String,
        eventName: String,
        onPayload: (PusherEvent?) -> Unit
    ): PrivateChannelEventListener {
        return object : PrivateChannelEventListener {
            override fun onEvent(event: PusherEvent?) {
                Log.d("PusherManager", "event received: $channelName / ${event?.eventName ?: eventName}")
                onPayload(event)
            }

            override fun onAuthenticationFailure(message: String?, e: Exception?) {
                Log.e("PusherManager", "private channel auth failed for $channelName: $message", e)
            }

            override fun onSubscriptionSucceeded(channelName: String?) {
                Log.d("PusherManager", "subscribed to private channel: $channelName")
            }
        }
    }

    private val _onlineUserIds = MutableStateFlow<Set<Int>>(emptySet())
    val onlineUserIds: StateFlow<Set<Int>> = _onlineUserIds.asStateFlow()

    fun init() {
        if (pusher != null || Constants.PUSHER_KEY.isBlank()) return

        try {
            val token = runBlocking { tokenManager.getToken().first() }
            val options = PusherOptions().apply {
                setCluster(Constants.PUSHER_CLUSTER)
                if (!token.isNullOrBlank()) {
                    val authorizer = HttpAuthorizer(broadcastingAuthUrl())
                    authorizer.setHeaders(mapOf("Authorization" to "Bearer $token"))
                    setAuthorizer(authorizer)
                }
            }

            pusher = Pusher(Constants.PUSHER_KEY, options)
            pusher?.connect(object : ConnectionEventListener {
                override fun onConnectionStateChange(change: ConnectionStateChange?) {
                    Log.d("PusherManager", "state: ${change?.previousState} -> ${change?.currentState}")
                }

                override fun onError(msg: String?, code: String?, e: Exception?) {
                    Log.e("PusherManager", "Pusher connection error: $msg ($code)", e)
                }
            }, ConnectionState.ALL)
        } catch (e: Exception) {
            Log.e("PusherManager", "Unable to initialize Pusher", e)
            pusher = null
        }
    }

    private fun broadcastingAuthUrl(): String {
        val apiBase = Constants.BASE_API_URL.trimEnd('/')
        val webBase = apiBase.removeSuffix("/api")
        return "$webBase/broadcasting/auth"
    }

    private fun registerBinding(
        channelName: String,
        eventName: String,
        listener: Any
    ): Boolean {
        val snapshot = boundEventsByChannel
            .mapValues { (_, value) -> value.toMap() }
        if (isDuplicateBinding(channelName, eventName, snapshot)) return false

        val events = boundEventsByChannel.getOrPut(channelName) { mutableMapOf() }
        if (events.containsKey(eventName)) return false
        events[eventName] = listener
        return true
    }

    private fun unregisterBindings(channelName: String) {
        val events = boundEventsByChannel.remove(channelName) ?: return
        val channel = when {
            channelName.startsWith("private-chat.") -> privateChatChannels[channelName]
            channelName.startsWith("private-conversations.") -> privateConversationChannels[channelName]
            channelName.startsWith("presence-user.") -> privatePresenceChannels[channelName]
            else -> null
        }

        if (channel != null) {
            events.forEach { (eventName, listener) ->
                when (listener) {
                    is SubscriptionEventListener -> runCatching { channel.unbind(eventName, listener) }
                    is PrivateChannelEventListener -> runCatching { channel.unbind(eventName, listener) }
                }
            }
        }
    }

    private fun unsubscribeFromChannel(channelName: String) {
        unregisterBindings(channelName)
        pusher?.unsubscribe(channelName)
    }

    fun subscribeToChannel(channelName: String, eventName: String, onEvent: (String) -> Unit) {
        try {
            init()
            val instance = pusher ?: return
            val listener = object : SubscriptionEventListener {
                override fun onEvent(event: PusherEvent?) {
                    Log.d("PusherManager", "event received: $channelName / ${event?.eventName ?: eventName}")
                    event?.data?.let { onEvent(it) }
                }
            }
            if (!registerBinding(channelName, eventName, listener)) return

            val existing = instance.getChannel(channelName)
            val channel = existing ?: run {
                val subscribed = instance.subscribe(channelName)
                if (subscribed == null) {
                    unregisterBindings(channelName)
                    null
                } else {
                    subscribed
                }
            }

            if (channel == null) {
                Log.w("PusherManager", "Channel subscription failed for $channelName/$eventName")
                return
            }

            channel.bind(eventName, listener)
        } catch (e: Exception) {
            Log.w("PusherManager", "Unable to subscribe to $channelName/$eventName", e)
            unregisterBindings(channelName)
        }
    }

    fun subscribeToPrivateChannel(userId: Int, eventName: String, onEvent: (String) -> Unit) {
        try {
            init()
            val instance = pusher ?: return
            val channelName = "private-user-$userId"
            val listener = privateEventListener(channelName, eventName) { event ->
                event?.data?.let { onEvent(it) }
            }
            if (!registerBinding(channelName, eventName, listener)) return

            val existing = privateConversationChannels[channelName]
            val channel = (existing ?: instance.getPrivateChannel(channelName))
                ?: instance.subscribePrivate(channelName, privateChannelListener(channelName))
                ?: run {
                    unregisterBindings(channelName)
                    null
                }

            if (channel != null) {
                privateConversationChannels[channelName] = channel
                channel.bind(eventName, listener)
            }
        } catch (e: Exception) {
            Log.w("PusherManager", "Unable to subscribe to private-user-$userId", e)
            unregisterBindings("private-user-$userId")
        }
    }

    fun subscribeToUserChannel(userId: Int, eventName: String, onEvent: (String) -> Unit) {
        subscribeToPrivateChannel(userId, eventName, onEvent)
    }

    fun subscribeToPrivateConversation(userId: Int, onEvent: (JSONObject) -> Unit) {
        try {
            init()
            val instance = pusher ?: return
            val channelName = "private-conversations.$userId"
            val eventName = "conversation.updated"
            val listener = privateEventListener(channelName, eventName) { event ->
                event?.data?.let {
                    runCatching { JSONObject(it) }.onSuccess(onEvent)
                }
            }
            if (!registerBinding(channelName, eventName, listener)) return

            val channel = privateConversationChannels[channelName]
                ?: instance.subscribePrivate(channelName, privateChannelListener(channelName))
                    ?.also { privateConversationChannels[channelName] = it }

            channel?.bind(eventName, listener)
        } catch (e: Exception) {
            Log.w("PusherManager", "Unable to subscribe to private-conversations.$userId", e)
        }
    }

    fun subscribeToPrivateChat(
        conversationId: Int,
        onMessage: (JSONObject) -> Unit,
        onTyping: (JSONObject) -> Unit,
        onDelivered: (JSONObject) -> Unit = {},
        onRead: (JSONObject) -> Unit = {}
    ) {
        try {
            init()
            val instance = pusher ?: return
            val channelName = "private-chat.$conversationId"
            val desiredBindings = listOf(
                "message.sent" to onMessage,
                "typing.changed" to onTyping,
                "message.delivered" to onDelivered,
                "message.read" to onRead
            )

            val channel = privateChatChannels[channelName]
                ?: instance.subscribePrivate(channelName, privateChannelListener(channelName))
                    ?.also { privateChatChannels[channelName] = it }

            if (channel == null) return

            desiredBindings.forEach { (eventName, callback) ->
                val listener = privateEventListener(channelName, eventName) { event ->
                    event?.data?.let {
                        runCatching { JSONObject(it) }.onSuccess(callback)
                    }
                }
                if (!registerBinding(channelName, eventName, listener)) return@forEach
                channel.bind(eventName, listener)
            }

            boundPrivateChatChannels.add(channelName)
        } catch (e: Exception) {
            Log.w("PusherManager", "Unable to subscribe to private-chat.$conversationId", e)
        }
    }

    fun subscribeToPresenceChannel() {
        if (presenceChannel != null) return
        try {
            init()
            val instance = pusher ?: return

            presenceChannel = instance.subscribePresence("presence-online-users", object : PresenceChannelEventListener {
                override fun onEvent(event: PusherEvent?) {}
                override fun onAuthenticationFailure(message: String?, e: Exception?) {
                    Log.e("PusherManager", "presence-global auth failed: $message", e)
                }

                override fun onSubscriptionSucceeded(channelName: String?) {
                    Log.d("PusherManager", "presence-online-users subscribed")
                }

                override fun onUsersInformationReceived(channelName: String?, users: MutableSet<User>?) {
                    _onlineUserIds.value = users.orEmpty().mapNotNull { it.id.toIntOrNull() }.toSet()
                }

                override fun userSubscribed(channelName: String?, user: User?) {
                    val id = user?.id?.toIntOrNull() ?: return
                    _onlineUserIds.value = _onlineUserIds.value + id
                }

                override fun userUnsubscribed(channelName: String?, user: User?) {
                    val id = user?.id?.toIntOrNull() ?: return
                    _onlineUserIds.value = _onlineUserIds.value - id
                }
            })
        } catch (e: Exception) {
            Log.w("PusherManager", "Unable to subscribe to presence-online-users", e)
        }
    }

    fun bindPresenceUser(userId: Int, onStatus: (JSONObject) -> Unit) {
        try {
            init()
            val instance = pusher ?: return
            val channelName = "presence-user.$userId"
            val eventName = "user.presence.changed"
            val listener = object : PresenceChannelEventListener {
                override fun onEvent(event: PusherEvent?) {
                    Log.d("PusherManager", "event received: $channelName / ${event?.eventName ?: eventName}")
                    event?.data?.let {
                        runCatching { JSONObject(it) }.onSuccess(onStatus)
                    }
                }

                override fun onAuthenticationFailure(message: String?, e: Exception?) {
                    Log.e("PusherManager", "presence-user auth failed for $channelName: $message", e)
                }

                override fun onSubscriptionSucceeded(channelName: String?) {
                    Log.d("PusherManager", "subscribed to presence channel: $channelName")
                }

                override fun onUsersInformationReceived(channelName: String?, users: MutableSet<User>?) {
                    Log.d("PusherManager", "presence users received for $channelName: ${users?.size ?: 0}")
                }

                override fun userSubscribed(channelName: String?, user: User?) {
                    Log.d("PusherManager", "presence user subscribed: ${user?.id}")
                }

                override fun userUnsubscribed(channelName: String?, user: User?) {
                    Log.d("PusherManager", "presence user unsubscribed: ${user?.id}")
                }
            }
            if (!registerBinding(channelName, eventName, listener)) return
            val channel = privatePresenceChannels[channelName]
                ?: instance.subscribePresence(channelName, listener)
                    ?.also { privatePresenceChannels[channelName] = it }

            channel?.bind(eventName, listener)
        } catch (e: Exception) {
            Log.w("PusherManager", "Unable to bind presence-user.$userId", e)
        }
    }

    fun unsubscribeFromPresenceUser(userId: Int) {
        val channelName = "presence-user.$userId"
        unsubscribeFromChannel(channelName)
        privatePresenceChannels.remove(channelName)
    }

    fun triggerTyping(conversationId: Int, userId: Int, isTyping: Boolean) {
        val channelName = "private-chat.$conversationId"
        val channel = pusher?.getPrivateChannel(channelName) ?: return
        val payload = JSONObject().apply {
            put("user_id", userId)
            put("is_typing", isTyping)
        }
        channel.trigger("client-typing", payload.toString())
    }

    fun unsubscribeFromPrivateChat(conversationId: Int) {
        val channelName = "private-chat.$conversationId"
        unsubscribeFromChannel(channelName)
        privateChatChannels.remove(channelName)
        boundPrivateChatChannels.remove(channelName)
    }

    fun unsubscribeFromPrivateConversation(userId: Int) {
        val channelName = "private-conversations.$userId"
        unsubscribeFromChannel(channelName)
        privateConversationChannels.remove(channelName)
    }

    fun disconnect() {
        boundEventsByChannel.keys.toList().forEach { channelName -> unregisterBindings(channelName) }
        privateChatChannels.clear()
        privateConversationChannels.clear()
        privatePresenceChannels.clear()
        boundPrivateChatChannels.clear()
        boundEventsByChannel.clear()
        pusher?.disconnect()
        pusher = null
        presenceChannel = null
        _onlineUserIds.value = emptySet()
    }
}

