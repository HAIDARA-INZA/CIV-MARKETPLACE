package ci.devsphere.civmarketplace.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveConversationTracker @Inject constructor() {
    private val _activeConversationId = MutableStateFlow<Int?>(null)
    private val _appVisible = MutableStateFlow(false)

    val activeConversationId = _activeConversationId.asStateFlow()

    fun setAppVisible(visible: Boolean) {
        _appVisible.value = visible
    }

    fun setActiveConversation(conversationId: Int) {
        _activeConversationId.value = conversationId.takeIf { it > 0 }
    }

    fun clearActiveConversation(conversationId: Int? = null) {
        if (conversationId == null || _activeConversationId.value == conversationId) {
            _activeConversationId.value = null
        }
    }

    fun isConversationVisible(conversationId: Int): Boolean =
        conversationId > 0 && _appVisible.value && _activeConversationId.value == conversationId
}
