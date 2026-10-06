package ci.devsphere.civmarketplace.di

import ci.devsphere.civmarketplace.data.model.ChatMessageDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class PendingActionStatus(
    val actionId: Long,
    val type: String,
    val status: String,
    val message: ChatMessageDto? = null,
    val clientMessageId: String? = null
)

object PendingActionStatusBus {
    private val _events = MutableSharedFlow<PendingActionStatus>(extraBufferCapacity = 16)
    val events: SharedFlow<PendingActionStatus> = _events.asSharedFlow()

    fun emit(
        actionId: Long,
        type: String,
        status: String,
        message: ChatMessageDto? = null,
        clientMessageId: String? = null
    ) {
        _events.tryEmit(PendingActionStatus(actionId, type, status, message, clientMessageId))
    }
}

