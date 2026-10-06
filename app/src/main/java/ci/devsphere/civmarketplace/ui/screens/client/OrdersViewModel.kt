package ci.devsphere.civmarketplace.ui.screens.client

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ci.devsphere.civmarketplace.data.model.OrderDto
import ci.devsphere.civmarketplace.di.NetworkStatusBus
import ci.devsphere.civmarketplace.di.RealtimeStatusBus
import ci.devsphere.civmarketplace.di.SyncStatusBus
import ci.devsphere.civmarketplace.domain.repository.AuthRepository
import ci.devsphere.civmarketplace.domain.repository.OrderRepository
import ci.devsphere.civmarketplace.domain.repository.ReviewRepository
import ci.devsphere.civmarketplace.util.PusherManager
import ci.devsphere.civmarketplace.util.RefreshCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
    private val pusherManager: PusherManager,
    private val reviewRepository: ReviewRepository,
    private val refreshCoordinator: RefreshCoordinator
) : ViewModel() {

    private val _state = mutableStateOf<OrdersState>(OrdersState.Loading)
    val state: State<OrdersState> = _state

    private val _reviewableOrderIds = mutableStateOf<Set<Int>>(emptySet())
    val reviewableOrderIds: State<Set<Int>> = _reviewableOrderIds

    private val _reviewSubmitState = mutableStateOf<ReviewSubmitState>(ReviewSubmitState.Idle)
    val reviewSubmitState: State<ReviewSubmitState> = _reviewSubmitState

    val isRefreshing: StateFlow<Boolean> = refreshCoordinator.activeKeys
        .map { keys -> ORDERS_CACHE_KEY in keys || REVIEWABLE_CACHE_KEY in keys }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var currentUserId: Int? = null
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            currentUserId = authRepository.getUserId()
            setupRealtime()
            refreshIfStale()
        }
        observeReconnection()
        observeRealtimeReconnection()
        SyncStatusBus.synced
            .onEach { refresh(force = true) }
            .launchIn(viewModelScope)
    }

    private fun observeReconnection() {
        NetworkStatusBus.reconnected
            .onEach { refresh(force = true) }
            .launchIn(viewModelScope)
    }

    private fun observeRealtimeReconnection() {
        RealtimeStatusBus.reconnected
            .onEach { setupRealtime() }
            .launchIn(viewModelScope)
    }

    fun refreshIfStale() = refresh(force = false)

    fun refresh(force: Boolean = true) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            launch { refreshOrders(force) }
            launch { refreshReviewableOrders(force) }
        }
    }

    fun loadOrders() = refresh(force = true)

    private suspend fun refreshOrders(force: Boolean) {
        refreshCoordinator.refresh(ORDERS_CACHE_KEY, DATA_TTL_MS, force) {
            val previous = _state.value
            if (previous !is OrdersState.Success && previous !is OrdersState.Empty) _state.value = OrdersState.Loading

            val result = runCatching {
                kotlinx.coroutines.withTimeout(45_000L) { orderRepository.getOrders().getOrThrow() }
            }
            result.onSuccess { orders ->
                _state.value = if (orders.isEmpty()) {
                    OrdersState.Empty("Aucune commande")
                } else {
                    OrdersState.Success(orders)
                }
            }.onFailure { error ->
                val errorMessage = if (
                    error.message?.contains("401") == true ||
                    error.message?.contains("Unauthenticated") == true
                ) {
                    "Votre session a expiré. Veuillez vous reconnecter."
                } else {
                    error.message ?: "Erreur de chargement des commandes"
                }
                _state.value = OrdersState.Error(errorMessage)
            }
            result.isSuccess
        }
    }

    private suspend fun refreshReviewableOrders(force: Boolean) {
        refreshCoordinator.refresh(REVIEWABLE_CACHE_KEY, DATA_TTL_MS, force) {
            val result = runCatching {
                kotlinx.coroutines.withTimeout(45_000L) { reviewRepository.getReviewableOrders().getOrThrow() }
            }
            result.onSuccess { list -> _reviewableOrderIds.value = list.map { it.orderId }.toSet() }
            result.isSuccess
        }
    }

    fun submitReview(orderId: Int, rating: Int, comment: String?) {
        viewModelScope.launch {
            _reviewSubmitState.value = ReviewSubmitState.Submitting
            reviewRepository.submitReview(orderId, rating, comment)
                .onSuccess {
                    _reviewSubmitState.value = ReviewSubmitState.Success
                    refreshReviewableOrders(force = true)
                }
                .onFailure {
                    _reviewSubmitState.value = ReviewSubmitState.Error(it.message ?: "Envoi de l'avis impossible")
                }
        }
    }

    fun resetReviewSubmitState() {
        _reviewSubmitState.value = ReviewSubmitState.Idle
    }

    private fun setupRealtime() {
        pusherManager.init()
        val userId = currentUserId ?: return
        pusherManager.subscribeToUserChannel(userId, "new-order") {
            refresh(force = true)
        }
        pusherManager.subscribeToUserChannel(userId, "order-updated") {
            refresh(force = true)
        }
    }

    private fun refreshIfOrderBelongsToUser(data: String) {
        val userId = currentUserId ?: return
        val json = runCatching { JSONObject(data) }.getOrNull() ?: return
        val clientId = json.optInt("client_id", 0)
        if (clientId == 0 || clientId == userId) refresh(force = true)
    }

    private companion object {
        const val ORDERS_CACHE_KEY = "orders:client"
        const val REVIEWABLE_CACHE_KEY = "orders:reviewable"
        const val DATA_TTL_MS = 45_000L
    }
}

sealed class OrdersState {
    object Loading : OrdersState()
    data class Empty(val message: String = "Aucune commande") : OrdersState()
    data class Success(val orders: List<OrderDto>) : OrdersState()
    data class Error(val message: String) : OrdersState()
}

sealed class ReviewSubmitState {
    object Idle : ReviewSubmitState()
    object Submitting : ReviewSubmitState()
    object Success : ReviewSubmitState()
    data class Error(val message: String) : ReviewSubmitState()
}