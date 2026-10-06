package ci.devsphere.civmarketplace.data.model

import com.google.gson.annotations.SerializedName

data class OrderDto(
    val id: Int,
    @SerializedName("product_name")
    val productName: String,
    val price: String,
    val status: String, // "EN_ATTENTE", "ACCEPTE", "EXPEDIE", "LIVRE"
    @SerializedName("client_name")
    val clientName: String? = null,
    @SerializedName("client_id")
    val clientId: Int? = null,
    @SerializedName("seller_id")
    val sellerId: Int? = null,
    @SerializedName("product_id")
    val productId: Int? = null,
    val quantity: Int? = null,
    @SerializedName("payment_status")
    val paymentStatus: String? = null, // "pending", "paid", "failed"
    @SerializedName("created_at")
    val createdAt: String
)

data class SellerStatsDto(
    @SerializedName(value = "total_sales", alternate = ["totalSales"])
    val totalSales: Double,
    @SerializedName(value = "views_count", alternate = ["viewsCount"])
    val viewsCount: Int,
    @SerializedName(value = "sales_trend", alternate = ["salesTrend"])
    val salesTrend: List<Float>,
    @SerializedName(value = "recent_orders", alternate = ["recentOrders"])
    val recentOrders: List<OrderDto>
)

