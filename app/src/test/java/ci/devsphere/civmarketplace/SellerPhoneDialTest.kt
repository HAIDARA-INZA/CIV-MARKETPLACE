package ci.devsphere.civmarketplace

import ci.devsphere.civmarketplace.data.model.ProductDto
import org.junit.Assert.assertEquals
import org.junit.Test

class SellerPhoneDialTest {
    @Test
    fun seller_phone_is_cleaned_for_dialer() {
        val product = ProductDto(
            id = 4,
            name = "Produit test",
            price = "1200",
            sellerPhone = " +221 77 123 45 67 "
        )

        assertEquals("+221771234567", product.getDialPhoneNumber())
    }
}

