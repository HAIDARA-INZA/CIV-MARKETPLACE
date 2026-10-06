package ci.devsphere.civmarketplace.ui.screens

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ci.devsphere.civmarketplace.data.local.ConversationDao
import ci.devsphere.civmarketplace.data.local.ConversationEntity
import ci.devsphere.civmarketplace.data.local.MessageDao
import ci.devsphere.civmarketplace.data.local.MessageEntity
import ci.devsphere.civmarketplace.data.model.ChatMessageDto
import ci.devsphere.civmarketplace.data.model.ConversationDto
import ci.devsphere.civmarketplace.di.RealtimeStatusBus
import ci.devsphere.civmarketplace.di.SyncStatusBus
import ci.devsphere.civmarketplace.di.TypingLifecycleBus
import ci.devsphere.civmarketplace.di.PendingActionStatusBus
import ci.devsphere.civmarketplace.domain.repository.AuthRepository
import ci.devsphere.civmarketplace.domain.repository.ChatRepository
import ci.devsphere.civmarketplace.util.ApiErrorMapper
import ci.devsphere.civmarketplace.util.AppError
import ci.devsphere.civmarketplace.util.ActiveConversationTracker
import ci.devsphere.civmarketplace.util.NetworkMonitor
import ci.devsphere.civmarketplace.util.NotificationUtils
import ci.devsphere.civmarketplace.util.OfflineActionManager
import ci.devsphere.civmarketplace.util.PusherManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val pusherManager: PusherManager,
    private val networkMonitor: NetworkMonitor,
    private val offlineActionManager: OfflineActionManager,
    private val activeConversationTracker: ActiveConversationTracker,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _conversationsState = mutableStateOf<ChatState<List<ConversationDto>>>(ChatState.Loading)
    val conversationsState: State<ChatState<List<ConversationDto>>> = _conversationsState

    private val _messagesState = mutableStateOf<ChatState<List<ChatMessageDto>>>(ChatState.Idle)
    val messagesState: State<ChatState<List<ChatMessageDto>>> = _messagesState

    /** Bug messagerie #2 : true si l'interlocuteur de la conversation ouverte est en train d'écrire. */
    private val _typingState = mutableStateOf(false)
    val typingState: State<Boolean> = _typingState

    /**
     * Bug messagerie #1 : IDs en ligne en temps réel (mis à jour par le presence channel).
     * ⚠️ Précision : n'est fiable que pendant qu'un écran de messagerie est ouvert, car
     * c'est ce qui déclenche l'abonnement au canal — pas encore un vrai "présence app entière".
     */
    val onlineUserIds: StateFlow<Set<Int>> = pusherManager.onlineUserIds

    private val _hasMoreState = mutableStateOf(false)
    val hasMoreState: State<Boolean> = _hasMoreState

    // PresenceDto allway null — endpoint /presence/{userId} non encore disponible
    private val _presenceState = mutableStateOf<ci.devsphere.civmarketplace.data.model.PresenceDto?>(null)
    val presenceState: State<ci.devsphere.civmarketplace.data.model.PresenceDto?> = _presenceState

    private val _currentUserIdState = mutableStateOf<Int?>(null)
    val currentUserIdState: State<Int?> = _currentUserIdState

    private val _selectedProductId = mutableStateOf<Int?>(null)
    val selectedProductId: State<Int?> = _selectedProductId

    private var openConversationUserId: Int? = null
    private var loadedMessagesUserId: Int? = null
    private var messagesPage = 1
    private var conversationsJob: Job? = null
    private var messagesJob: Job? = null
    private var messagesRequestId = 0L
    private var typingClearJob: Job? = null
    private var conversationObserverJob: Job? = null
    private var conversationMessagesObserverJob: Job? = null
    private var catchUpJob: Job? = null
    private val typingStopJobs = mutableMapOf<Int, Job>()
    private val typingHeartbeatJobs = mutableMapOf<Int, Job>()
    private val subscribedConversationUserIds = mutableSetOf<Int>()
    private val _typingUsers = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val typingUsers: StateFlow<Map<Int, Boolean>> = _typingUsers.asStateFlow()

    init {
        viewModelScope.launch {
            conversationDao.observeAll().collectLatest { cached ->
                if (cached.isNotEmpty()) {
                    _conversationsState.value = ChatState.Success(cached.map(ConversationEntity::toDto))
                }
            }
        }
        viewModelScope.launch {
            _currentUserIdState.value = authRepository.getUserId()
            openConversationUserId?.let { setConversationVisible(it, true) }
            setupPusher()
            loadConversations()
        }
        observeRealtimeReconnection()
        SyncStatusBus.synced
            .onEach {
                loadConversations()
                openConversationUserId?.let { syncMissedMessages(it) }
            }
            .launchIn(viewModelScope)
        TypingLifecycleBus.stop
            .onEach { stopAllTyping() }
            .launchIn(viewModelScope)
        PendingActionStatusBus.events
            .onEach { status -> reconcilePendingMessage(status) }
            .launchIn(viewModelScope)
    }

    private fun observeRealtimeReconnection() {
        RealtimeStatusBus.reconnected
            .onEach {
                setupPusher()
                loadConversations()
                openConversationUserId?.let { syncMissedMessages(it) }
            }
            .launchIn(viewModelScope)
    }

    private fun syncMissedMessages(userId: Int) {
        if (catchUpJob?.isActive == true || openConversationUserId != userId) return

        catchUpJob = viewModelScope.launch {
            if (!networkMonitor.isOnline.value) return@launch

            var cursor = messageDao.latestServerMessageId(userId)
            if (cursor <= 0) {
                loadMessages(userId)
                return@launch
            }

            var hasMore = true
            while (hasMore && openConversationUserId == userId && networkMonitor.isOnline.value) {
                val page = chatRepository.getMessages(userId, afterId = cursor).getOrElse {
                    return@launch
                }
                if (page.messages.isEmpty()) break

                messageDao.upsertAll(page.messages.map { MessageEntity.fromDto(it, userId) })
                val currentMessages = (_messagesState.value as? ChatState.Success)?.data.orEmpty()
                val merged = (currentMessages + page.messages)
                    .distinctBy(::messageKey)
                    .sortedBy(ChatMessageDto::timestamp)
                _messagesState.value = ChatState.Success(merged)
                acknowledgeIncomingMessages(page.messages)
                cursor = page.messages.maxOfOrNull(ChatMessageDto::id) ?: cursor
                hasMore = page.hasMore
            }

            loadConversations()
        }
    }

    fun loadConversations() {
        if (conversationsJob?.isActive == true) return
        conversationsJob = viewModelScope.launch {
            chatRepository.getConversations()
                .onSuccess { conversations ->
                    conversationDao.replaceAll(conversations.map(ConversationEntity::fromDto))
                    _conversationsState.value = ChatState.Success(conversations)
                }
                .onFailure { _conversationsState.value = ChatState.Error(ApiErrorMapper.fromCaught(it, "Erreur")) }
        }
    }

    fun loadMessages(userId: Int) {
        if (messagesJob?.isActive == true && openConversationUserId == userId) return

        messagesJob?.cancel()
        openConversationUserId = userId
        _currentUserIdState.value?.let { currentUserId ->
            val conversationId = conversationIdFor(currentUserId, userId)
            activeConversationTracker.setActiveConversation(conversationId)
            NotificationUtils.clearChatNotifications(context, conversationId)
        }
        conversationMessagesObserverJob?.cancel()
        conversationMessagesObserverJob = viewModelScope.launch {
            messageDao.observeConversation(userId).collectLatest { cachedMessages ->
                if (openConversationUserId == userId && cachedMessages.isNotEmpty()) {
                    val currentMessages = (_messagesState.value as? ChatState.Success)?.data.orEmpty()
                    val merged = (currentMessages + cachedMessages.map(MessageEntity::toDto))
                        .distinctBy(::messageKey)
                        .sortedBy(ChatMessageDto::timestamp)
                    _messagesState.value = ChatState.Success(merged)
                }
            }
        }
        messagesPage = 1
        _typingState.value = false
        subscribeToConversation(userId)
        val requestId = ++messagesRequestId
        if (loadedMessagesUserId != userId || _messagesState.value !is ChatState.Success) {
            _messagesState.value = ChatState.Loading
        }
        messagesJob = viewModelScope.launch {
            chatRepository.getMessages(userId, 1)
                .onSuccess { page ->
                    if (requestId == messagesRequestId && openConversationUserId == userId) {
                        messagesPage = page.currentPage
                        _hasMoreState.value = page.hasMore
                        loadedMessagesUserId = userId
                        _messagesState.value = ChatState.Success(page.messages)
                        messageDao.upsertAll(page.messages.map { message ->
                            MessageEntity.fromDto(message, userId)
                        })
                        acknowledgeIncomingMessages(page.messages)
                    }
                }
                .onFailure {
                    if (
                        requestId == messagesRequestId &&
                        openConversationUserId == userId &&
                        (loadedMessagesUserId != userId || _messagesState.value !is ChatState.Success)
                    ) {
                        _messagesState.value = ChatState.Error(ApiErrorMapper.fromCaught(it, "Erreur"))
                    }
                }
        }
    }

    fun loadMoreMessages() {
        val userId = openConversationUserId ?: return
        if (!_hasMoreState.value) return

        val nextPage = messagesPage + 1
        val currentState = _messagesState.value
        if (currentState is ChatState.Loading) return

        val preservedMessages = if (currentState is ChatState.Success) currentState.data else emptyList()
        _messagesState.value = ChatState.Loading
        viewModelScope.launch {
            chatRepository.getMessages(userId, nextPage)
                .onSuccess { page ->
                    val merged = (preservedMessages + page.messages).distinctBy { messageKey(it) }
                    messagesPage = page.currentPage
                    _hasMoreState.value = page.hasMore
                    _messagesState.value = ChatState.Success(merged)
                    messageDao.upsertAll(page.messages.map { MessageEntity.fromDto(it, userId) })
                    acknowledgeIncomingMessages(page.messages)
                }
                .onFailure {
                    _messagesState.value = if (currentState is ChatState.Success) {
                        ChatState.Success(currentState.data)
                    } else {
                        ChatState.Error(ApiErrorMapper.fromCaught(it, "Erreur"))
                    }
                }
        }
    }

    fun onComposerChanged(receiverId: Int, text: String) {
        if (receiverId <= 0) return

        typingStopJobs[receiverId]?.cancel()
        if (text.isBlank()) {
            stopTyping(receiverId)
            return
        }

        if (typingHeartbeatJobs[receiverId] == null) {
            typingHeartbeatJobs[receiverId] = viewModelScope.launch {
                delay(1_000)
                if (!isActive) return@launch
                sendTypingStatus(receiverId, true)
                while (isActive) {
                    delay(3_000)
                    sendTypingStatus(receiverId, true)
                }
            }
        }

        typingStopJobs[receiverId] = viewModelScope.launch {
            delay(4_500)
            stopTyping(receiverId)
        }
    }

    fun sendMessage(receiverId: Int, message: String, existingClientMessageId: String? = null, productId: Int? = null) {
        val cleanMessage = message.trim()
        if (receiverId <= 0 || cleanMessage.isBlank()) return
        // On arrête d'afficher "en train d'écrire" chez soi dès l'envoi.
        stopTyping(receiverId)
        val tempId = -((System.currentTimeMillis() % 1_000_000_000L).toInt() + 1)
        val clientMessageId = existingClientMessageId ?: UUID.randomUUID().toString()
        val optimisticMessage = ChatMessageDto(
            id = tempId,
            senderId = _currentUserIdState.value ?: 0,
            receiverId = receiverId,
            clientMessageId = clientMessageId,
            message = cleanMessage,
            timestamp = currentIsoTimestamp(),
            status = "sending",
            productId = productId
        )
        appendOrReplaceMessage(optimisticMessage)
        updateConversationPreview(receiverId, cleanMessage, optimisticMessage.timestamp)

        viewModelScope.launch {
            chatRepository.sendMessage(receiverId, cleanMessage, clientMessageId, productId)
                .onSuccess { sentMessage ->
                    replaceMessage(tempId, sentMessage)
                    loadConversations()
                }
                .onFailure {
                    if (!networkMonitor.isOnline.value) {
                        offlineActionManager.queueMessage(receiverId, cleanMessage, clientMessageId)
                        replaceMessage(tempId, optimisticMessage.copy(status = "pending"))
                    } else {
                        replaceMessage(tempId, optimisticMessage.copy(status = "failed"))
                        loadConversations()
                    }
                }
        }
    }

    fun retryMessage(clientMessageId: String) {
        viewModelScope.launch {
            val local = messageDao.findByClientMessageId(clientMessageId) ?: return@launch
            if (local.status == "sending" || local.status == "pending") return@launch
            val retrying = local.toDto().copy(status = "sending")
            replaceMessage(local.id, retrying)
            sendMessage(local.receiverId, local.message.orEmpty(), clientMessageId)
        }
    }

    fun sendAttachment(receiverId: Int, message: String, attachmentUri: String, attachmentType: String) {
        if (receiverId <= 0 || attachmentUri.isBlank() || attachmentType.isBlank()) return
        viewModelScope.launch {
            chatRepository.sendAttachment(receiverId, message.trim(), attachmentUri, attachmentType)
                .onSuccess {
                    loadMessages(receiverId)
                    loadConversations()
                }
                .onFailure { _messagesState.value = ChatState.Error(ApiErrorMapper.fromCaught(it, "Envoi du fichier impossible")) }
        }
    }

    /**
     * Bug messagerie #2 : à appeler depuis le TextField de saisie (onValueChange).
     * On limite les appels réseau à un toutes les 3s max quand l'utilisateur tape en continu,
     * plutôt que d'en envoyer un par frappe.
     */
    fun sendTyping(receiverId: Int, isTyping: Boolean) {
        if (isTyping) {
            onComposerChanged(receiverId, " ")
        } else {
            stopTyping(receiverId)
        }
    }

    private fun sendTypingStatus(receiverId: Int, isTyping: Boolean) {
        if (receiverId <= 0) return
        viewModelScope.launch {
            chatRepository.sendTypingStatus(receiverId, isTyping)
        }
    }

    fun stopTyping(receiverId: Int) {
        if (receiverId <= 0) return
        typingStopJobs.remove(receiverId)?.cancel()
        typingHeartbeatJobs.remove(receiverId)?.cancel()
        viewModelScope.launch { chatRepository.sendTypingStatus(receiverId, false) }
    }

    private fun stopAllTyping() {
        typingHeartbeatJobs.keys.toList().forEach(::stopTyping)
    }

    fun updateMessage(messageId: Int, message: String) { /* TODO: ajouter la route backend */ }
    fun deleteMessage(messageId: Int) { /* TODO: ajouter la route backend */ }
    private var presenceJob: Job? = null
    private var presenceUserId: Int? = null

    fun loadPresence(userId: Int) {
        if (presenceUserId != userId) {
            presenceUserId?.let(pusherManager::unsubscribeFromPresenceUser)
            presenceJob?.cancel()
            presenceUserId = userId
        }

        pusherManager.bindPresenceUser(userId) { payload ->
            if (payload.optInt("user_id", userId) == userId) {
                _presenceState.value = ci.devsphere.civmarketplace.data.model.PresenceDto(
                    userId = userId,
                    isOnline = payload.optBoolean("is_online", false),
                    lastSeenAt = payload.optString("last_seen_at").takeIf(String::isNotBlank)
                )
            }
        }

        if (presenceJob?.isActive == true) return
        presenceJob = viewModelScope.launch {
            chatRepository.getPresence(userId)
                .onSuccess { _presenceState.value = it }
        }
    }

    private fun setupPusher() {
        subscribedConversationUserIds.clear()
        pusherManager.init()
        pusherManager.subscribeToPresenceChannel()

        val userId = _currentUserIdState.value ?: return

        pusherManager.subscribeToPrivateConversation(userId) {
            loadConversations()
        }

        conversationObserverJob?.cancel()
        conversationObserverJob = viewModelScope.launch {
            conversationDao.observeAll().collectLatest { conversations ->
                conversations.forEach { subscribeToConversation(it.otherUserId) }
            }
        }
        openConversationUserId?.let(::loadPresence)
    }

    private fun subscribeToConversation(otherUserId: Int) {
        if (!subscribedConversationUserIds.add(otherUserId)) return

        val currentUserId = _currentUserIdState.value ?: return
        val conversationId = conversationIdFor(currentUserId, otherUserId)

        pusherManager.subscribeToPrivateChat(
            conversationId = conversationId,
            onMessage = { data ->
                val senderId = data.optInt("sender_id", 0)
                val receiverId = data.optInt("receiver_id", 0)
                val messageJson = data.optJSONObject("message")
                val messageId = messageJson?.optInt("id", 0) ?: 0
                if (senderId == currentUserId || receiverId == currentUserId) {
                    val otherUserId = if (senderId == currentUserId) receiverId else senderId
                    if (receiverId == currentUserId && messageId > 0) {
                        markDelivered(messageId)
                        if (openConversationUserId == senderId) {
                            markRead(messageId)
                        }
                    }
                    _typingUsers.value = _typingUsers.value - otherUserId
                    loadConversations()
                    if (openConversationUserId == otherUserId) {
                        _typingState.value = false
                        syncMissedMessages(otherUserId)
                    }
                }
            },
            onTyping = { data ->
                val fromUser = data.optInt("user_id", 0)
                val isTyping = data.optBoolean("is_typing", false)
                val eventConversationId = data.optInt("conversation_id", conversationId)
                if (fromUser != currentUserId && eventConversationId == conversationId) {
                    _typingUsers.value = _typingUsers.value.toMutableMap().apply {
                        this[otherUserId] = isTyping
                    }
                    _typingState.value = isTyping
                    typingClearJob?.cancel()
                    typingClearJob = viewModelScope.launch {
                        delay(5_000)
                        _typingUsers.value = _typingUsers.value - otherUserId
                        if (openConversationUserId == otherUserId) _typingState.value = false
                    }
                }
            },
            onDelivered = { data ->
                applyDeliveryEvent(
                    messageId = data.optInt("message_id", 0),
                    clientMessageId = data.optString("client_message_id").takeIf { it.isNotBlank() },
                    deliveredAt = data.optString("delivered_at").takeIf { it.isNotBlank() },
                    readAt = null
                )
            },
            onRead = { data ->
                applyDeliveryEvent(
                    messageId = data.optInt("message_id", 0),
                    clientMessageId = data.optString("client_message_id").takeIf { it.isNotBlank() },
                    deliveredAt = null,
                    readAt = data.optString("read_at").takeIf { it.isNotBlank() }
                )
            }
        )
    }

    private fun acknowledgeIncomingMessages(messages: List<ChatMessageDto>) {
        val currentUserId = _currentUserIdState.value ?: return
        val openUserId = openConversationUserId

        messages
            .filter { it.receiverId == currentUserId && it.id > 0 }
            .forEach { message ->
                if (message.deliveredAt == null) {
                    markDelivered(message.id)
                }

                if (openUserId == message.senderId && message.readAt == null) {
                    markRead(message.id)
                }
            }
    }

    private fun markDelivered(messageId: Int) {
        val current = (_messagesState.value as? ChatState.Success)?.data.orEmpty()
        val existing = current.firstOrNull { it.id == messageId }
        if (existing?.deliveredAt != null) return

        viewModelScope.launch {
            chatRepository.markDelivered(messageId)
                .onSuccess { deliveredMessage ->
                    replaceMessage(messageId, deliveredMessage)
                }
        }
    }

    private fun markRead(messageId: Int) {
        val current = (_messagesState.value as? ChatState.Success)?.data.orEmpty()
        val existing = current.firstOrNull { it.id == messageId }
        if (existing?.readAt != null) return

        viewModelScope.launch {
            chatRepository.markRead(messageId)
                .onSuccess { readMessage ->
                    replaceMessage(messageId, readMessage)
                    loadConversations()
                }
        }
    }

    private fun applyDeliveryEvent(
        messageId: Int,
        clientMessageId: String?,
        deliveredAt: String?,
        readAt: String?
    ) {
        if (messageId <= 0 && clientMessageId.isNullOrBlank()) return
        if (readAt != null) {
            val currentUserId = _currentUserIdState.value
            val openUserId = openConversationUserId
            if (currentUserId != null && openUserId != null) {
                NotificationUtils.clearChatNotifications(
                    context,
                    conversationIdFor(currentUserId, openUserId)
                )
            }
        }

        val current = (_messagesState.value as? ChatState.Success)?.data.orEmpty()
        val updated = current.map { message ->
            val sameMessage = message.id == messageId ||
                (!clientMessageId.isNullOrBlank() && message.clientMessageId == clientMessageId)

            if (sameMessage) {
                message.copy(
                    deliveredAt = deliveredAt ?: message.deliveredAt,
                    readAt = readAt ?: message.readAt,
                    isRead = if (readAt != null) true else message.isRead
                )
            } else {
                message
            }
        }

        if (updated != current) {
            _messagesState.value = ChatState.Success(updated)
        }

        viewModelScope.launch {
            messageDao.updateDeliveryStatus(
                messageId = messageId,
                clientMessageId = clientMessageId,
                deliveredAt = deliveredAt,
                readAt = readAt,
                status = null
            )
        }
    }

    private fun conversationIdFor(firstUserId: Int, secondUserId: Int): Int {
        val minUserId = minOf(firstUserId, secondUserId)
        val maxUserId = maxOf(firstUserId, secondUserId)
        return (minUserId * 1_000_000) + maxUserId
    }

    private fun updateConversationPreview(receiverId: Int, message: String, timestamp: String) {
        viewModelScope.launch {
            val existing = conversationDao.findByOtherUserId(receiverId)

            conversationDao.upsert(
                (existing ?: ConversationEntity(
                    otherUserId = receiverId,
                    otherUserName = "Discussion",
                    lastMessage = message,
                    lastMessageTime = timestamp,
                    unreadCount = 0,
                    isOnline = false,
                    lastSeenAt = null
                )).copy(
                    lastMessage = message,
                    lastMessageTime = timestamp,
                    unreadCount = 0
                )
            )
        }
    }

    fun setSelectedProductId(productId: Int?) {
        _selectedProductId.value = productId
    }

    fun setConversationVisible(userId: Int, visible: Boolean) {
        val currentUserId = _currentUserIdState.value ?: return
        val conversationId = conversationIdFor(currentUserId, userId)
        if (visible) {
            activeConversationTracker.setActiveConversation(conversationId)
        } else {
            activeConversationTracker.clearActiveConversation(conversationId)
        }
    }

    private fun messageKey(message: ChatMessageDto): String {
        return message.clientMessageId?.takeIf { it.isNotBlank() }
            ?: "server-${message.id}"
    }

    private fun appendOrReplaceMessage(message: ChatMessageDto) {
        val current = (_messagesState.value as? ChatState.Success)?.data.orEmpty()
        val updated = if (current.any { it.id == message.id || messageKey(it) == messageKey(message) }) {
            current.map {
                if (it.id == message.id || messageKey(it) == messageKey(message)) message else it
            }
        } else {
            current + message
        }
        _messagesState.value = ChatState.Success(updated.distinctBy { messageKey(it) })
        val otherUserId = if (message.senderId == _currentUserIdState.value) message.receiverId else message.senderId
        viewModelScope.launch { messageDao.upsert(MessageEntity.fromDto(message, otherUserId)) }
    }

    private fun replaceMessage(oldId: Int, newMessage: ChatMessageDto) {
        val current = (_messagesState.value as? ChatState.Success)?.data.orEmpty()
        val updated = if (current.any { it.id == oldId || messageKey(it) == messageKey(newMessage) }) {
            current.map {
                if (it.id == oldId || messageKey(it) == messageKey(newMessage)) newMessage else it
            }
        } else {
            current + newMessage
        }
        _messagesState.value = ChatState.Success(updated.distinctBy { messageKey(it) })
        val otherUserId = if (newMessage.senderId == _currentUserIdState.value) newMessage.receiverId else newMessage.senderId
        viewModelScope.launch { messageDao.upsert(MessageEntity.fromDto(newMessage, otherUserId)) }
    }

    private fun reconcilePendingMessage(status: ci.devsphere.civmarketplace.di.PendingActionStatus) {
        if (status.type != OfflineActionManager.TYPE_SEND_MESSAGE) return
        val clientMessageId = status.clientMessageId ?: return
        viewModelScope.launch {
            val local = messageDao.findByClientMessageId(clientMessageId)
            when (status.status) {
                "sent" -> status.message?.let { replaceMessage(local?.id ?: 0, it) }
                "failed" -> local?.let {
                    val failed = it.toDto().copy(status = "failed")
                    replaceMessage(it.id, failed)
                }
            }
            loadConversations()
        }
    }

    private fun currentIsoTimestamp(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }

    override fun onCleared() {
        stopAllTyping()
        conversationObserverJob?.cancel()
        conversationMessagesObserverJob?.cancel()
        catchUpJob?.cancel()
        val currentUserId = _currentUserIdState.value
        if (currentUserId != null) {
            openConversationUserId?.let {
                activeConversationTracker.clearActiveConversation(conversationIdFor(currentUserId, it))
            }
            pusherManager.unsubscribeFromPrivateConversation(currentUserId)
            subscribedConversationUserIds.forEach { otherUserId ->
                pusherManager.unsubscribeFromPrivateChat(
                    conversationIdFor(currentUserId, otherUserId)
                )
            }
        }
        subscribedConversationUserIds.clear()
        presenceUserId?.let(pusherManager::unsubscribeFromPresenceUser)
        presenceUserId = null
        super.onCleared()
        typingClearJob?.cancel()
        presenceJob?.cancel()
        typingStopJobs.values.forEach { it.cancel() }
        typingHeartbeatJobs.values.forEach { it.cancel() }
    }
}

sealed class ChatState<out T> {
    object Idle    : ChatState<Nothing>()
    object Loading : ChatState<Nothing>()
    data class Success<T>(val data: T) : ChatState<T>()
    data class Error(val error: AppError) : ChatState<Nothing>() {
        val message: String get() = error.message
    }
}

