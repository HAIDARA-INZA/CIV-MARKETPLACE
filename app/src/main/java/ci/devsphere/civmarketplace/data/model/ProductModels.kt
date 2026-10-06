package ci.devsphere.civmarketplace.data.model

import com.google.gson.annotations.SerializedName

data class ProductDto(
    val id: Int,
    val name: String,
    val description: String? = null,
    val price: String,
    @SerializedName(value = "image_url", alternate = ["imageUrl", "image"])
    val imageUrl: String? = null,
    val stock: Int = 0,
    val category: String? = null,
    @SerializedName(value = "type", alternate = ["item_type", "product_type"])
    val type: String? = "product",
    @SerializedName(value = "seller_id", alternate = ["sellerId"])
    val sellerId: Int? = null,
    @SerializedName(value = "seller_name", alternate = ["sellerName"])
    val sellerName: String? = null,
    @SerializedName(value = "seller_phone", alternate = ["sellerPhone", "phone"])
    val sellerPhone: String? = null,
    @SerializedName(value = "seller_city", alternate = ["sellerCity"])
    val sellerCity: String? = null,
    @SerializedName(value = "seller_commune", alternate = ["sellerCommune"])
    val sellerCommune: String? = null,
    @SerializedName(value = "seller_quarter", alternate = ["sellerQuarter"])
    val sellerQuarter: String? = null,
    @SerializedName(value = "seller_is_verified", alternate = ["sellerIsVerified"])
    val sellerIsVerified: Boolean = false,
    val rating: Double? = null,
    @SerializedName(value = "views_count", alternate = ["viewsCount"])
    val viewsCount: Int? = null,
    @SerializedName(value = "is_favorite", alternate = ["isFavorite"])
    val isFavorite: Boolean = false
) {
    fun getDisplayImageUrl(): String? = imageUrl
    fun getActualSellerId(): Int? = sellerId
    fun isService(): Boolean = type?.equals("service", ignoreCase = true) == true
    fun shouldShowStockStatus(): Boolean = !isService()
    fun typeLabel(): String = if (isService()) "Service" else "Produit"

    fun getSellerLocation(): String? = listOf(sellerQuarter, sellerCommune, sellerCity)
        .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
        .takeIf(List<String>::isNotEmpty)
        ?.joinToString(", ")

    fun getDialPhoneNumber(): String? {
        val raw = sellerPhone?.trim() ?: return null
        val sanitized = raw.filter { it.isDigit() || it == '+' }
        if (sanitized.isBlank()) return null
        return if (sanitized.startsWith("+")) "+" + sanitized.drop(1).filter { it.isDigit() } else sanitized.filter { it.isDigit() }
    }
}

data class ProductResponse(
    val products: List<ProductDto>
)

data class CategoryDto(
    val id: Int,
    val name: String,
    val slug: String? = null
)

data class PromotionDto(
    val id: Int,
    val title: String,
    val subtitle: String? = null,
    @SerializedName(value = "image_url", alternate = ["imageUrl"])
    val imageUrl: String? = null,
    val category: String? = null,
    @SerializedName(value = "product_id", alternate = ["productId"])
    val productId: Int? = null,
    @SerializedName(value = "cta_label", alternate = ["ctaLabel"])
    val ctaLabel: String? = null
)

data class FavoriteResponse(
    @SerializedName(value = "is_favorite", alternate = ["isFavorite"])
    val isFavorite: Boolean
)

