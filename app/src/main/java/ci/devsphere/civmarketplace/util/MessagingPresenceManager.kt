package ci.devsphere.civmarketplace.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import ci.devsphere.civmarketplace.di.NetworkStatusBus
import ci.devsphere.civmarketplace.di.RealtimeStatusBus
import ci.devsphere.civmarketplace.di.TypingLifecycleBus
import ci.devsphere.civmarketplace.data.local.TokenManager
import ci.devsphere.civmarketplace.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingPresenceManager @Inject constructor(
    private val tokenManager: TokenManager,
    private val chatRepository: ChatRepository,
    private val pusherManager: PusherManager,
    private val activeConversationTracker: ActiveConversationTracker
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(Dispatchers.IO)

    private var heartbeatJob: Job? = null
    private var isForeground = false

    init {
        scope.launch {
            NetworkStatusBus.reconnected.collect {
                pusherManager.disconnect()
                connectIfAuthenticated()
                RealtimeStatusBus.emitReconnected()
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        isForeground = true
        activeConversationTracker.setAppVisible(true)

        scope.launch {
            connectIfAuthenticated()
            RealtimeStatusBus.emitReconnected()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        isForeground = false
        activeConversationTracker.setAppVisible(false)

        TypingLifecycleBus.stopAll()

        heartbeatJob?.cancel()
        heartbeatJob = null

        // Dernière activité enregistrée avant passage arrière-plan.
        scope.launch {
            chatRepository.sendOffline()
        }

        // L'utilisateur passe hors-ligne : Laravel peut envoyer le push FCM.
        pusherManager.disconnect()
    }

    fun onAuthenticated() {
        scope.launch {
            pusherManager.disconnect()
            connectIfAuthenticated()
        }
    }

    fun onLoggedOut() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        pusherManager.disconnect()
    }

    private suspend fun connectIfAuthenticated() {
        val token = tokenManager.getToken().first()

        if (!isForeground || token.isNullOrBlank()) {
            return
        }

        pusherManager.init()
        pusherManager.subscribeToPresenceChannel()

        chatRepository.sendHeartbeat()

        heartbeatJob?.cancel()

        heartbeatJob = scope.launch {
            while (isActive && isForeground) {
                delay(45_000)
                chatRepository.sendHeartbeat()
            }
        }
    }
}

