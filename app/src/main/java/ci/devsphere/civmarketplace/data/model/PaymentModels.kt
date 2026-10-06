package ci.devsphere.civmarketplace.data.model

import com.google.gson.annotations.SerializedName

data class CheckoutRequest(
    val items: List<CartItemRequest>,
    @SerializedName("total_amount")
    val totalAmount: Double
)

data class CartItemRequest(
    @SerializedName("product_id")
    val productId: Int,
    val name: String,
    val quantity: Int,
    @SerializedName("unit_price")
    val unitPrice: Double,
    @SerializedName("total_price")
    val totalPrice: Double
)

data class CheckoutResponse(
    @SerializedName(value = "checkout_url", alternate = ["checkoutUrl", "payment_url", "url"])
    val checkoutUrl: String,
    @SerializedName(value = "token", alternate = ["invoice_token"])
    val token: String? = null
)

