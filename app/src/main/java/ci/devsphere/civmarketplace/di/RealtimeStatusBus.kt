package ci.devsphere.civmarketplace.di

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object RealtimeStatusBus {
    private val _reconnected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reconnected: SharedFlow<Unit> = _reconnected.asSharedFlow()

    suspend fun emitReconnected() {
        _reconnected.emit(Unit)
    }
}

