package ci.devsphere.civmarketplace.di

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object TypingLifecycleBus {
    private val _stop = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val stop = _stop.asSharedFlow()

    fun stopAll() {
        _stop.tryEmit(Unit)
    }
}

