package ci.devsphere.civmarketplace.ui.screens.seller

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ci.devsphere.civmarketplace.data.model.ProductDto
import ci.devsphere.civmarketplace.data.model.SellerStatsDto
import ci.devsphere.civmarketplace.di.NetworkStatusBus
import ci.devsphere.civmarketplace.di.ProductRefreshBus
import ci.devsphere.civmarketplace.di.RealtimeStatusBus
import ci.devsphere.civmarketplace.di.SyncStatusBus
import ci.devsphere.civmarketplace.domain.repository.AuthRepository
import ci.devsphere.civmarketplace.domain.repository.OrderRepository
import ci.devsphere.civmarketplace.domain.repository.ProductRepository
import ci.devsphere.civmarketplace.util.NetworkMonitor
import ci.devsphere.civmarketplace.util.OfflineActionManager
import ci.devsphere.civmarketplace.util.PusherManager
import ci.devsphere.civmarketplace.util.RefreshCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SellerViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val pusherManager: PusherManager,
    private val networkMonitor: NetworkMonitor,
    private val offlineActionManager: OfflineActionManager,
    private val refreshCoordinator: RefreshCoordinator
) : ViewModel() {

    private val _state = mutableStateOf<SellerState>(SellerState.Loading)
    val state: State<SellerState> = _state

    private val _sellerId = mutableStateOf<Int?>(null)
    val sellerId: State<Int?> = _sellerId

    private val _sellerName = mutableStateOf("")
    val sellerName: State<String> = _sellerName

    private val _uploadState = mutableStateOf<UploadState>(UploadState.Idle)
    val uploadState: State<UploadState> = _uploadState
    val isRefreshing = refreshCoordinator.activeKeys
        .map { SELLER_DASHBOARD_CACHE_KEY in it }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var latestStats: SellerStatsDto? = null
    private var latestProducts: List<ProductDto> = emptyList()
    private var dashboardJob: Job? = null

    init {
        viewModelScope.launch {
            val sellerId = authRepository.getUserId()
            _sellerId.value = sellerId
            _sellerName.value = authRepository.getName().orEmpty()
            if (sellerId != null) {
                loadDashboard()
            }
        }
        setupPusher()
        observeReconnection()
        observeRealtimeReconnection()
        SyncStatusBus.synced
            .onEach { refreshDashboard(force = true) }
            .launchIn(viewModelScope)
    }

    private fun observeReconnection() {
        NetworkStatusBus.reconnected
            .onEach {
                refreshIfStale()
            }
            .launchIn(viewModelScope)
    }

    private fun observeRealtimeReconnection() {
        RealtimeStatusBus.reconnected
            .onEach { setupPusher() }
            .launchIn(viewModelScope)
    }

    private fun setupPusher() {
        pusherManager.init()
        pusherManager.subscribeToChannel("products-channel", "product-added") { refreshDashboard(force = true) }
        pusherManager.subscribeToChannel("products-channel", "product-updated") { refreshDashboard(force = true) }
    }

    /** Appelé une fois que le profil vendeur est chargé et que l'ID est connu */
    fun subscribeToPrivateOrdersChannel(sellerId: Int) {
        pusherManager.subscribeToPrivateChannel(sellerId, "notification-created") {
            refreshDashboard(force = true)
        }
    }

    /**
     * Charge stats + produits EN PARALLÈLE, puis met à jour l'état UNE SEULE FOIS
     * avec les deux résultats → plus de race condition / écran flou.
     */
    fun refreshIfStale() = refreshDashboard(force = false)

    fun refresh(force: Boolean = true) = refreshDashboard(force)

    fun loadDashboard() = refreshDashboard(force = true)

    private fun refreshDashboard(force: Boolean) {
        if (dashboardJob?.isActive == true) return
        dashboardJob = viewModelScope.launch {
            refreshCoordinator.refresh(SELLER_DASHBOARD_CACHE_KEY, SELLER_TTL_MS, force) {
                val wasSuccess = _state.value is SellerState.Success
                val wasEmpty = _state.value is SellerState.Empty
                if (!wasSuccess && !wasEmpty) _state.value = SellerState.Loading

                val statsResult = runCatching {
                    kotlinx.coroutines.withTimeout(45_000L) { orderRepository.getSellerStats().getOrThrow() }
                }
                val productsResult = runCatching {
                    kotlinx.coroutines.withTimeout(45_000L) { productRepository.getSellerProducts().getOrThrow() }
                }

                val stats = statsResult.getOrNull()
                val products = productsResult.getOrNull() ?: emptyList()

                when {
                    stats != null -> {
                        latestStats = stats
                        latestProducts = products
                        _state.value = if (products.isEmpty()) {
                            SellerState.Empty("Aucun produit pour le moment")
                        } else {
                            SellerState.Success(stats, products)
                        }
                    }
                    products.isNotEmpty() && stats == null -> {
                        _state.value = SellerState.Error("Réessayer")
                    }
                    else -> {
                        _state.value = SellerState.Error(
                            statsResult.exceptionOrNull()?.message
                                ?: productsResult.exceptionOrNull()?.message
                                ?: "Réessayer"
                        )
                    }
                }

                statsResult.isSuccess || productsResult.isSuccess
            }
        }
    }

    private companion object {
        const val SELLER_DASHBOARD_CACHE_KEY = "seller:dashboard"
        const val SELLER_TTL_MS = 60_000L
    }

    fun loadStats()    { loadDashboard() }
    fun loadMyProducts() { loadDashboard() }

    fun addProduct(
        name: String,
        description: String,
        price: String,
        stock: String,
        category: String,
        type: String = "product",
        imageUri: String
    ) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading

            val cleanName = name.trim()
            val cleanDescription = description.trim()
            val cleanCategory = category.trim()
            val cleanType = if (type.equals("service", true)) "service" else "product"
            val priceDouble = parseProductPrice(price)
            val stockInt = stock.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0

            when {
                cleanName.isBlank() -> {
                    _uploadState.value = UploadState.Error("Le nom du produit est obligatoire")
                    return@launch
                }
                cleanCategory.isBlank() -> {
                    _uploadState.value = UploadState.Error("La catégorie est obligatoire")
                    return@launch
                }
                cleanDescription.isBlank() -> {
                    _uploadState.value = UploadState.Error("La description est obligatoire")
                    return@launch
                }
                priceDouble == null || priceDouble <= 0.0 -> {
                    _uploadState.value = UploadState.Error("Le prix doit être supérieur à 0")
                    return@launch
                }
                imageUri.isBlank() -> {
                    _uploadState.value = UploadState.Error("Ajoutez une image du produit")
                    return@launch
                }
            }

            if (!networkMonitor.isOnline.value) {
                offlineActionManager.queueProduct(
                    name = cleanName,
                    description = cleanDescription,
                    price = priceDouble!!,
                    stock = stockInt,
                    category = cleanCategory,
                    type = cleanType,
                    imageUri = imageUri
                )
                _uploadState.value = UploadState.Pending
                return@launch
            }

            productRepository.createProduct(
                name = cleanName,
                description = cleanDescription,
                price = priceDouble!!,
                stock = stockInt,
                category = cleanCategory,
                type = cleanType,
                imageUri = imageUri
            ).onSuccess {
                _uploadState.value = UploadState.Success
                ProductRefreshBus.emit()
                loadStats()
                loadMyProducts()
            }.onFailure { e ->
                if (!networkMonitor.isOnline.value) {
                    offlineActionManager.queueProduct(
                        name = cleanName,
                        description = cleanDescription,
                        price = priceDouble!!,
                        stock = stockInt,
                        category = cleanCategory,
                        type = cleanType,
                        imageUri = imageUri
                    )
                    _uploadState.value = UploadState.Pending
                } else {
                    _uploadState.value = UploadState.Error(e.message ?: "Erreur d'envoi")
                }
            }
        }
    }

    fun updateOrderStatus(orderId: Int, status: String) {
        viewModelScope.launch {
            orderRepository.updateOrderStatus(orderId, status)
                .onSuccess { loadStats() }
                .onFailure { e ->
                    _state.value = SellerState.Error(e.message ?: "Statut de commande non mis à jour")
                }
        }
    }

    fun deleteProduct(productId: Int) {
        viewModelScope.launch {
            productRepository.deleteProduct(productId)
                .onSuccess { loadDashboard() }
                .onFailure { _state.value = SellerState.Error(it.message ?: "Suppression impossible") }
        }
    }

    fun updateProduct(product: ProductDto, name: String, description: String, price: String, stock: String, category: String) {
        viewModelScope.launch {
            val parsedPrice = parseProductPrice(price)
            val parsedStock = stock.toIntOrNull()
            if (name.isBlank() || description.isBlank() || category.isBlank() || parsedPrice == null || parsedPrice <= 0 || parsedStock == null || parsedStock < 0) {
                _state.value = SellerState.Error("Vérifiez le nom, la description, le prix, le stock et la catégorie")
                return@launch
            }
            productRepository.updateProduct(product.id, name.trim(), description.trim(), parsedPrice, parsedStock, category.trim())
                .onSuccess { loadDashboard() }
                .onFailure { _state.value = SellerState.Error(it.message ?: "Modification impossible") }
        }
    }

    private fun parseProductPrice(value: String): Double? {
        return value
            .replace("FCFA", "", ignoreCase = true)
            .replace("\u00A0", "")
            .replace(" ", "")
            .replace(",", ".")
            .toDoubleOrNull()
    }
}

sealed class SellerState {
    object Loading : SellerState()
    data class Empty(val message: String = "Aucun produit pour le moment") : SellerState()
    data class Success(
        val stats: SellerStatsDto,
        val myProducts: List<ProductDto> = emptyList()
    ) : SellerState()
    data class Error(val message: String) : SellerState()
}

sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState()
    object Pending : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}

