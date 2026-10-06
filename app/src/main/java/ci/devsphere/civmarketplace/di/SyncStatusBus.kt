package ci.devsphere.civmarketplace.di

import ci.devsphere.civmarketplace.data.model.SyncResponseDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SyncStatusBus {
    private val _synced = MutableSharedFlow<SyncResponseDto>(extraBufferCapacity = 1)
    val synced: SharedFlow<SyncResponseDto> = _synced.asSharedFlow()

    fun emit(response: SyncResponseDto) {
        _synced.tryEmit(response)
    }
}

