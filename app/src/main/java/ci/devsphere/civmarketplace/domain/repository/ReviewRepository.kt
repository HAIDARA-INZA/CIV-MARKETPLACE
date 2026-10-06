package ci.devsphere.civmarketplace.domain.repository

import ci.devsphere.civmarketplace.data.model.ReviewDto
import ci.devsphere.civmarketplace.data.model.ReviewableOrderDto

interface ReviewRepository {
    suspend fun getReviewsForProduct(productId: Int): Result<List<ReviewDto>>
    suspend fun getReviewableOrders(): Result<List<ReviewableOrderDto>>
    suspend fun submitReview(orderId: Int, rating: Int, comment: String?): Result<ReviewDto>
}

