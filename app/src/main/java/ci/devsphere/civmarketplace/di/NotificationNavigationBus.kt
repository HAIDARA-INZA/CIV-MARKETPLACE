package ci.devsphere.civmarketplace.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class NotificationNavigationTarget {
    data object Orders : NotificationNavigationTarget()
    data object Notifications : NotificationNavigationTarget()
}

object NotificationNavigationBus {
    private val _target = MutableStateFlow<NotificationNavigationTarget?>(null)
    val target: StateFlow<NotificationNavigationTarget?> = _target

    fun openOrders() {
        _target.value = NotificationNavigationTarget.Orders
    }

    fun openNotifications() {
        _target.value = NotificationNavigationTarget.Notifications
    }

    fun consume() {
        _target.value = null
    }
}

