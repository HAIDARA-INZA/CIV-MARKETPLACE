package ci.devsphere.civmarketplace.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class MarketplaceDeepLinkTarget {
    data class Shop(val shopId: Int) : MarketplaceDeepLinkTarget()
    data class Product(val productId: Int) : MarketplaceDeepLinkTarget()
}

object MarketplaceDeepLinkBus {
    private val _target = MutableStateFlow<MarketplaceDeepLinkTarget?>(null)
    val target = _target.asStateFlow()

    fun openShop(shopId: Int) {
        if (shopId > 0) {
            _target.value = MarketplaceDeepLinkTarget.Shop(shopId)
        }
    }

    fun openProduct(productId: Int) {
        if (productId > 0) {
            _target.value = MarketplaceDeepLinkTarget.Product(productId)
        }
    }

    fun consume() {
        _target.value = null
    }
}

