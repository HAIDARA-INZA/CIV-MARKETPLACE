package ci.devsphere.civmarketplace.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChatNavigationTarget(
    val userId: Int,
    val userName: String,
    val productId: Int?
)

object ChatNavigationBus {
    private val _target = MutableStateFlow<ChatNavigationTarget?>(null)
    val target = _target.asStateFlow()

    fun open(userId: Int, userName: String, productId: Int? = null) {
        if (userId > 0) {
            _target.value = ChatNavigationTarget(userId, userName, productId)
        }
    }

    fun consume() {
        _target.value = null
    }
}
