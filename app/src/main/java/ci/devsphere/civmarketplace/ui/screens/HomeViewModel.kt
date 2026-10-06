package ci.devsphere.civmarketplace.ui.screens

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ci.devsphere.civmarketplace.data.model.CategoryDto
import ci.devsphere.civmarketplace.data.model.ProductDto
import ci.devsphere.civmarketplace.data.model.PromotionDto
import ci.devsphere.civmarketplace.di.NetworkStatusBus
import ci.devsphere.civmarketplace.di.ProductRefreshBus
import ci.devsphere.civmarketplace.di.RealtimeStatusBus
import ci.devsphere.civmarketplace.di.SyncStatusBus
import ci.devsphere.civmarketplace.domain.repository.ProductRepository
import ci.devsphere.civmarketplace.util.ApiErrorMapper
import ci.devsphere.civmarketplace.util.AppError
import ci.devsphere.civmarketplace.util.PusherManager
import ci.devsphere.civmarketplace.util.RefreshCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val pusherManager: PusherManager,
    private val refreshCoordinator: RefreshCoordinator
) : ViewModel() {

    private val _homeState = mutableStateOf<HomeState>(HomeState.Loading)
    val homeState: State<HomeState> = _homeState

    private val _productSearchState = mutableStateOf<ProductSearchState>(ProductSearchState.Loading)
    val productSearchState: State<ProductSearchState> = _productSearchState

    private val _vendorStoreState = mutableStateOf<VendorStoreState>(VendorStoreState.Loading)
    val vendorStoreState: State<VendorStoreState> = _vendorStoreState

    private val _categoriesState = mutableStateOf<HomeCategoriesState>(HomeCategoriesState.Loading)
    val categoriesState: State<HomeCategoriesState> = _categoriesState

    private val _promotionsState = mutableStateOf<PromotionsState>(PromotionsState.Loading)
    val promotionsState: State<PromotionsState> = _promotionsState

    private var productsJob: Job? = null
    private var categoriesJob: Job? = null
    private var promotionsJob: Job? = null
    private var productRequestId = 0L
    private var searchRequestId = 0L
    private var searchJob: Job? = null
    private var vendorStoreJob: Job? = null
    private var activeVendorStoreId: Int? = null
    private val loadedVendorStoreIds = mutableSetOf<Int>()
    private var currentProductQuery: String? = null
    val isRefreshing = refreshCoordinator.activeKeys
        .map { keys -> keys.any { it.startsWith("home:") } }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val cartCount: StateFlow<Int> = productRepository.getCartItems()
        .map { items -> items.sumOf { it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        setupPusher()
        observeReconnection()
        observeProductRefresh()
        observeRealtimeReconnection()
        SyncStatusBus.synced
            .onEach { refreshCatalog(force = true) }
            .launchIn(viewModelScope)
    }

    private fun observeProductRefresh() {
        ProductRefreshBus.events
            .onEach { loadProducts() }
            .launchIn(viewModelScope)
    }

    /**
     * Bug 4 : quand la connexion revient après une coupure, on relance
     * automatiquement les données visibles sur cet écran — pas besoin
     * de fermer/rouvrir l'app.
     */
    private fun observeReconnection() {
        NetworkStatusBus.reconnected
            .onEach {
                refreshCatalog(force = true)
            }
            .launchIn(viewModelScope)
    }

    fun refreshIfStale() {
        refreshCatalog(force = false)
    }

    fun refresh(force: Boolean) {
        refreshCatalog(force)
    }

    private fun refreshCatalog(force: Boolean) {
        loadProducts(query = null, force = force)
        loadCategories(force)
        loadPromotions(force)
    }

    private fun observeRealtimeReconnection() {
        RealtimeStatusBus.reconnected
            .onEach { setupPusher() }
            .launchIn(viewModelScope)
    }

    fun loadProducts(query: String? = null, force: Boolean = false) {
        val queryChanged = currentProductQuery != query
        val refreshKey = "home:products:${query.orEmpty().trim().lowercase()}"
        if (productsJob?.isActive == true && !queryChanged) return

        currentProductQuery = query
        val requestId = ++productRequestId
        productsJob?.cancel()
        productsJob = viewModelScope.launch {
            val shouldForce = force || queryChanged
            refreshCoordinator.refresh(refreshKey, CATALOG_TTL_MS, shouldForce) {
                if (_homeState.value !is HomeState.Success) _homeState.value = HomeState.Loading
                val result = productRepository.getProducts(query)
                result.onSuccess {
                    if (requestId == productRequestId) _homeState.value = HomeState.Success(it)
                }.onFailure {
                    if (requestId == productRequestId && _homeState.value !is HomeState.Success) {
                        _homeState.value = HomeState.Error(ApiErrorMapper.fromCaught(it, "Impossible de charger les produits"))
                    }
                }
                result.isSuccess
            }
        }
    }

    fun searchProducts(query: String, debounceMillis: Long = 400L) {
        val normalizedQuery = query.trim()
        val requestId = ++searchRequestId
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounceMillis > 0L) delay(debounceMillis)
            if (requestId != searchRequestId) return@launch

            _productSearchState.value = ProductSearchState.Loading
            Log.d("Search", "products request start q=${normalizedQuery.ifBlank { "<empty>" }}")
            try {
                val products = withTimeout(45_000L) {
                    productRepository.getProducts(normalizedQuery.ifBlank { null }).getOrThrow()
                }
                if (requestId == searchRequestId) {
                    _productSearchState.value = ProductSearchState.Success(products)
                    Log.d("Search", "products request success count=${products.size}")
                }
            } catch (timeout: TimeoutCancellationException) {
                Log.e("Search", "products request timed out q=${normalizedQuery.ifBlank { "<empty>" }}", timeout)
                if (requestId == searchRequestId) {
                    _productSearchState.value = ProductSearchState.Error("La recherche a expiré. Réessayez.")
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Log.e("Search", "products request failed q=${normalizedQuery.ifBlank { "<empty>" }}", error)
                if (requestId == searchRequestId) {
                    _productSearchState.value = ProductSearchState.Error(
                        error.message ?: "Impossible de charger les produits"
                    )
                }
            }
        }
    }

    fun cancelProductSearch() {
        searchRequestId++
        searchJob?.cancel()
        searchJob = null
    }

    fun loadVendorProducts(vendorId: Int, force: Boolean = false) {
        if (vendorId <= 0) {
            _vendorStoreState.value = VendorStoreState.Error("Vendeur introuvable")
            Log.e("Vendors", "store load rejected invalid vendorId=$vendorId")
            return
        }
        if (vendorStoreJob?.isActive == true && activeVendorStoreId == vendorId) return
        if (!force && vendorId in loadedVendorStoreIds) return

        vendorStoreJob?.cancel()
        activeVendorStoreId = vendorId
        vendorStoreJob = viewModelScope.launch {
            val key = "vendor:products:$vendorId"
            _vendorStoreState.value = VendorStoreState.Loading
            Log.d("Vendors", "store load start vendorId=$vendorId key=$key")
            try {
                val loaded = refreshCoordinator.refresh(
                    key = key,
                    ttlMillis = CATALOG_TTL_MS,
                    force = force || vendorId !in loadedVendorStoreIds,
                    debounceMillis = 0L
                ) {
                    Log.d("Vendors", "request GET /products started vendorId=$vendorId")
                    val products = try {
                        withTimeout(45_000L) {
                            productRepository.getProducts(null).getOrThrow()
                        }.filter { it.getActualSellerId() == vendorId }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Throwable) {
                        Log.e("Vendors", "store request failed vendorId=$vendorId endpoint=GET /products", error)
                        _vendorStoreState.value = VendorStoreState.Error(
                            error.message ?: "Impossible de charger cette boutique"
                        )
                        return@refresh false
                    }
                    loadedVendorStoreIds.add(vendorId)
                    _vendorStoreState.value = if (products.isEmpty()) {
                        VendorStoreState.Empty
                    } else {
                        VendorStoreState.Success(products)
                    }
                    Log.d("Vendors", "store load success vendorId=$vendorId count=${products.size}")
                    true
                }
                if (!loaded && _vendorStoreState.value is VendorStoreState.Loading) {
                    Log.e("Vendors", "store load did not run vendorId=$vendorId; coordinator skipped or timed out")
                    _vendorStoreState.value = VendorStoreState.Error("Impossible de charger cette boutique")
                }
            } catch (timeout: TimeoutCancellationException) {
                Log.e("Vendors", "store load timed out vendorId=$vendorId", timeout)
                _vendorStoreState.value = VendorStoreState.Error("Le chargement a expiré. Réessayez.")
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Log.e("Vendors", "store load crashed vendorId=$vendorId", error)
                _vendorStoreState.value = VendorStoreState.Error(
                    error.message ?: "Impossible de charger cette boutique"
                )
            }
        }
    }

    fun loadCategories(force: Boolean = false) {
        if (categoriesJob?.isActive == true) return
        categoriesJob = viewModelScope.launch {
            refreshCoordinator.refresh("home:categories", CATALOG_TTL_MS, force) {
                if (_categoriesState.value !is HomeCategoriesState.Success) _categoriesState.value = HomeCategoriesState.Loading
                val result = productRepository.getCategories()
                result.onSuccess { _categoriesState.value = HomeCategoriesState.Success(it) }
                    .onFailure {
                        if (_categoriesState.value !is HomeCategoriesState.Success) {
                            _categoriesState.value = HomeCategoriesState.Error(it.message ?: "Impossible de charger les catégories")
                        }
                    }
                result.isSuccess
            }
        }
    }

    fun loadPromotions(force: Boolean = false) {
        if (promotionsJob?.isActive == true) return
        promotionsJob = viewModelScope.launch {
            refreshCoordinator.refresh("home:promotions", CATALOG_TTL_MS, force) {
                if (_promotionsState.value !is PromotionsState.Success) _promotionsState.value = PromotionsState.Loading
                val result = productRepository.getActivePromotions()
                result.onSuccess { _promotionsState.value = PromotionsState.Success(it) }
                    .onFailure {
                        if (_promotionsState.value !is PromotionsState.Success) {
                            _promotionsState.value = PromotionsState.Error(it.message ?: "Impossible de charger les promotions")
                        }
                    }
                result.isSuccess
            }
        }
    }

    fun setFavorite(product: ProductDto, favorite: Boolean) {
        viewModelScope.launch {
            productRepository.setFavorite(product.id, favorite)
                .onSuccess {
                    val current = (_homeState.value as? HomeState.Success)?.products ?: return@onSuccess
                    _homeState.value = HomeState.Success(
                        current.map { item ->
                            if (item.id == product.id) item.copy(isFavorite = favorite) else item
                        }
                    )
                }
                .onFailure { e ->
                    _homeState.value = HomeState.Error(ApiErrorMapper.fromCaught(e, "Favori non mis à jour"))
                }
        }
    }

    private fun setupPusher() {
        pusherManager.init()
        pusherManager.subscribeToChannel("products-channel", "product-added") { loadProducts(currentProductQuery, force = true) }
        pusherManager.subscribeToChannel("products-channel", "product-updated") { loadProducts(currentProductQuery, force = true) }
        pusherManager.subscribeToChannel("products-channel", "product-deleted") { loadProducts(currentProductQuery, force = true) }
        pusherManager.subscribeToChannel("vendors-channel", "vendor-created") { loadProducts(currentProductQuery, force = true) }
        pusherManager.subscribeToChannel("promotions-channel", "promotion-updated") { loadPromotions(force = true) }
    }

    private companion object {
        const val CATALOG_TTL_MS = 45_000L
    }
}

sealed class HomeState {
    object Loading : HomeState()
    data class Success(val products: List<ProductDto>) : HomeState()
    data class Error(val error: AppError) : HomeState() {
        val message: String get() = error.message
    }
}

sealed class ProductSearchState {
    object Loading : ProductSearchState()
    data class Success(val products: List<ProductDto>) : ProductSearchState()
    data class Error(val message: String) : ProductSearchState()
}

sealed class VendorStoreState {
    object Loading : VendorStoreState()
    object Empty : VendorStoreState()
    data class Success(val products: List<ProductDto>) : VendorStoreState()
    data class Error(val message: String) : VendorStoreState()
}

sealed class HomeCategoriesState {
    object Loading : HomeCategoriesState()
    data class Success(val categories: List<CategoryDto>) : HomeCategoriesState()
    data class Error(val message: String) : HomeCategoriesState()
}

sealed class PromotionsState {
    object Loading : PromotionsState()
    data class Success(val promotions: List<PromotionDto>) : PromotionsState()
    data class Error(val message: String) : PromotionsState()
}

