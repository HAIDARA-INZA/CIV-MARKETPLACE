package ci.devsphere.civmarketplace.domain.repository

import ci.devsphere.civmarketplace.data.model.OrderDto
import ci.devsphere.civmarketplace.data.model.SellerStatsDto

interface OrderRepository {
    suspend fun getOrders(): Result<List<OrderDto>>
    suspend fun updateOrderStatus(id: Int, status: String): Result<OrderDto>
    suspend fun getSellerStats(): Result<SellerStatsDto>
}

