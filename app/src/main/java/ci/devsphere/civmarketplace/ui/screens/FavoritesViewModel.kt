package ci.devsphere.civmarketplace.ui.screens

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ci.devsphere.civmarketplace.data.model.ProductDto
import ci.devsphere.civmarketplace.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _state = mutableStateOf<FavoritesState>(FavoritesState.Loading)
    val state: State<FavoritesState> = _state

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _state.value = FavoritesState.Loading
            productRepository.getFavoriteProducts()
                .onSuccess { _state.value = FavoritesState.Success(it) }
                .onFailure { _state.value = FavoritesState.Error(it.message ?: "Impossible de charger les favoris") }
        }
    }

    fun setFavorite(product: ProductDto, favorite: Boolean) {
        viewModelScope.launch {
            productRepository.setFavorite(product.id, favorite)
                .onSuccess { loadFavorites() }
                .onFailure { _state.value = FavoritesState.Error(it.message ?: "Favori non mis à jour") }
        }
    }
}

sealed class FavoritesState {
    object Loading : FavoritesState()
    data class Success(val products: List<ProductDto>) : FavoritesState()
    data class Error(val message: String) : FavoritesState()
}

