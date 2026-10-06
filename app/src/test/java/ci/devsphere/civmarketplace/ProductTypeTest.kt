package ci.devsphere.civmarketplace

import ci.devsphere.civmarketplace.data.model.ProductDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProductTypeTest {
    @Test
    fun product_type_is_normalized_for_display() {
        val product = ProductDto(
            id = 7,
            name = "Service de livraison",
            price = "2500",
            type = "service"
        )

        assertEquals("service", product.type)
        assertEquals("Service", product.typeLabel())
        assertFalse(product.shouldShowStockStatus())
    }

    @Test
    fun seller_location_joins_only_non_blank_parts() {
        val product = ProductDto(
            id = 8,
            name = "Réparation",
            price = "5000",
            sellerQuarter = "Cocody",
            sellerCommune = "Riviera",
            sellerCity = "Abidjan"
        )

        assertEquals("Cocody, Riviera, Abidjan", product.getSellerLocation())
    }
}

