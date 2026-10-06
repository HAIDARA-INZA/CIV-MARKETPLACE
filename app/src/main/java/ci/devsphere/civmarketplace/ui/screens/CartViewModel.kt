package ci.devsphere.civmarketplace.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ci.devsphere.civmarketplace.data.local.CartEntity
import ci.devsphere.civmarketplace.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    val cartItems: StateFlow<List<CartEntity>> = productRepository.getCartItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFromCart(item: CartEntity) {
        viewModelScope.launch {
            productRepository.removeFromCart(item)
        }
    }

    fun getTotalPrice(items: List<CartEntity>): Double {
        return items.sumOf { it.price * it.quantity }
    }
}

