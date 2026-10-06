package ci.devsphere.civmarketplace

import ci.devsphere.civmarketplace.util.MarketplaceDeepLinkParser
import ci.devsphere.civmarketplace.util.SlugUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketplaceDeepLinkParserTest {
    @Test
    fun parse_public_shop_and_product_paths() {
        assertEquals(42, MarketplaceDeepLinkParser.parseShopId("/shop/42"))
        assertEquals(120, MarketplaceDeepLinkParser.parseProductId("/shop/42/120"))
        assertEquals(42, MarketplaceDeepLinkParser.parseShopId("/laravel/public/shop/42"))
        assertEquals(120, MarketplaceDeepLinkParser.parseProductId("/laravel/public/public-product/120"))
        assertEquals(42, MarketplaceDeepLinkParser.parseShopId("/shop/nom-utilisateur-42"))
        assertEquals(120, MarketplaceDeepLinkParser.parseProductId("/shop/nom-utilisateur-42/service-120"))
        assertEquals(42, MarketplaceDeepLinkParser.parseShopId("/nom-utilisateur-42"))
        assertEquals(120, MarketplaceDeepLinkParser.parseProductId("/service-120"))
        assertEquals("creme-eclaircie-42", SlugUtils.routeSlug("Crème éclaircie", 42))
    }
}

