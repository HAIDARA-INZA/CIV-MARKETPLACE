package ci.devsphere.civmarketplace.di

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ProductRefreshBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun emit() {
        _events.tryEmit(Unit)
    }
}

