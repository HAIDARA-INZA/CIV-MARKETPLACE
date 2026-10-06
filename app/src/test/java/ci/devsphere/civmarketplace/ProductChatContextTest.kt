package ci.devsphere.civmarketplace

import ci.devsphere.civmarketplace.data.model.ChatMessageDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductChatContextTest {
    @Test
    fun product_context_is_attached_to_message_and_filterable() {
        val message = ChatMessageDto(
            id = 9,
            senderId = 2,
            receiverId = 1,
            clientMessageId = "abc",
            message = "Bonjour, est-ce disponible ?",
            timestamp = "2026-09-26T10:00:00Z",
            productId = 12,
            productName = "Smartphone X",
            productImageUrl = "https://example.com/image.jpg"
        )

        assertEquals(12, message.productId)
        assertEquals("Smartphone X", message.productName)
        assertTrue(message.matchesProductFilter(12))
        assertFalse(message.matchesProductFilter(99))
    }
}

