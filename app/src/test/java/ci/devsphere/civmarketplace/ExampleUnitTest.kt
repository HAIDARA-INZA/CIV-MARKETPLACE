package ci.devsphere.civmarketplace

import ci.devsphere.civmarketplace.util.PusherManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun duplicate_binding_is_detected() {
        val registry = mapOf(
            "private-chat.42" to mapOf(
                "message.sent" to Any()
            )
        )

        assertTrue(PusherManager.isDuplicateBinding("private-chat.42", "message.sent", registry))
        assertFalse(PusherManager.isDuplicateBinding("private-chat.42", "message.read", registry))
    }
}
