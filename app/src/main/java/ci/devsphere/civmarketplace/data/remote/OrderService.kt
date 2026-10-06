package ci.devsphere.civmarketplace.data.remote

import ci.devsphere.civmarketplace.data.model.OrderDto
import ci.devsphere.civmarketplace.data.model.SellerStatsDto
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface OrderService {
    @GET("orders")
    suspend fun getOrders(): List<OrderDto>

    @PATCH("orders/{id}")
    suspend fun updateOrderStatus(@Path("id") id: Int, @Query("status") status: String): OrderDto

    @GET("seller/stats")
    suspend fun getSellerStats(): SellerStatsDto
}

