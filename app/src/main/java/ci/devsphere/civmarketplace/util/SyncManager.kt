package ci.devsphere.civmarketplace.util

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import ci.devsphere.civmarketplace.data.local.TokenManager
import ci.devsphere.civmarketplace.data.remote.SyncService
import ci.devsphere.civmarketplace.di.NetworkStatusBus
import ci.devsphere.civmarketplace.di.SyncStatusBus
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncService: SyncService,
    private val tokenManager: TokenManager,
    private val userRepository: ci.devsphere.civmarketplace.domain.repository.UserRepository
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    init {
        scope.launch {
            NetworkStatusBus.reconnected.collect {
                syncNow()
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        syncNow()
        refreshFcmToken()
    }

    fun syncNow() {
        scope.launch {
            if (!syncMutex.tryLock()) return@launch

            try {
                val token = tokenManager.getToken().first()
                if (token.isNullOrBlank()) return@launch

                runCatching {
                    val response = syncService.sync(tokenManager.getLastSyncAt().first())
                    tokenManager.saveLastSyncAt(response.serverTime)
                    NotificationUtils.setBadgeCount(context, response.unreadCounts.total)
                    SyncStatusBus.emit(response)
                }
            } finally {
                syncMutex.unlock()
            }
        }
    }

    private fun refreshFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNotBlank()) {
                    scope.launch {
                        userRepository.updateFcmToken(token)
                    }
                }
            }
    }
}

