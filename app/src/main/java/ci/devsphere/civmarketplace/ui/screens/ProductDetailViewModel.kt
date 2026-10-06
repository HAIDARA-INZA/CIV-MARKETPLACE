package ci.devsphere.civmarketplace.ui.screens

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ci.devsphere.civmarketplace.data.model.ProductDto
import ci.devsphere.civmarketplace.data.model.ReviewDto
import ci.devsphere.civmarketplace.domain.repository.AuthRepository
import ci.devsphere.civmarketplace.domain.repository.ProductRepository
import ci.devsphere.civmarketplace.domain.repository.ReviewRepository
import ci.devsphere.civmarketplace.util.ApiErrorMapper
import ci.devsphere.civmarketplace.util.AppError
import ci.devsphere.civmarketplace.util.RoleUtils
import ci.devsphere.civmarketplace.util.RefreshCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository,
    private val refreshCoordinator: RefreshCoordinator
) : ViewModel() {

    private val _state = mutableStateOf<ProductDetailState>(ProductDetailState.Loading)
    val state: State<ProductDetailState> = _state

    private val _canBuy = mutableStateOf(true)
    val canBuy: State<Boolean> = _canBuy

    // Étape 2 lancement : vrais avis, chargés séparément pour ne pas bloquer
    // l'affichage du produit si jamais ça échoue.
    private val _reviews = mutableStateOf<List<ReviewDto>>(emptyList())
    val reviews: State<List<ReviewDto>> = _reviews
    private var productRefreshJob: Job? = null
    private var reviewsJob: Job? = null

    init {
        viewModelScope.launch {
            _canBuy.value = RoleUtils.canBuy(authRepository.getRole() ?: "client")
        }
    }

    fun loadProduct(id: Int) {
        refreshProduct(id, force = false)
    }

    fun refreshProductIfStale(id: Int) {
        refreshProduct(id, force = false)
    }

    fun refreshProduct(id: Int, force: Boolean) {
        if (productRefreshJob?.isActive == true) return
        productRefreshJob = viewModelScope.launch {
            val previous = _state.value as? ProductDetailState.Success
            val hasCurrentProduct = previous?.product?.id == id
            if (!hasCurrentProduct) _state.value = ProductDetailState.Loading

            refreshCoordinator.refresh("product-detail:$id", PRODUCT_TTL_MS, force) {
                val result = productRepository.getProductDetails(id)
                result.onSuccess { _state.value = ProductDetailState.Success(it) }
                    .onFailure { error ->
                        if (!hasCurrentProduct) {
                            _state.value = ProductDetailState.Error(
                                ApiErrorMapper.fromCaught(error, "Une erreur est survenue")
                            )
                        }
                    }
                result.isSuccess
            }
            loadReviews(id)
        }
    }

    private fun loadReviews(productId: Int) {
        if (reviewsJob?.isActive == true) return
        reviewsJob = viewModelScope.launch {
            refreshCoordinator.refresh("product-reviews:$productId", REVIEW_TTL_MS, force = false) {
                val result = reviewRepository.getReviewsForProduct(productId)
                result.onSuccess { _reviews.value = it }
                result.isSuccess
            }
        }
    }

    private companion object {
        const val PRODUCT_TTL_MS = 45_000L
        const val REVIEW_TTL_MS = 45_000L
    }

    fun addToCart(product: ProductDto) {
        viewModelScope.launch {
            productRepository.addToCart(product)
        }
    }

    fun setFavorite(product: ProductDto, favorite: Boolean) {
        viewModelScope.launch {
            productRepository.setFavorite(product.id, favorite)
                .onSuccess {
                    _state.value = ProductDetailState.Success(product.copy(isFavorite = favorite))
                }
                .onFailure { e ->
                    _state.value = ProductDetailState.Error(ApiErrorMapper.fromCaught(e, "Favori non mis à jour"))
                }
        }
    }
}

sealed class ProductDetailState {
    object Loading : ProductDetailState()
    data class Success(val product: ProductDto) : ProductDetailState()
    data class Error(val error: AppError) : ProductDetailState() {
        val message: String get() = error.message
    }
}

