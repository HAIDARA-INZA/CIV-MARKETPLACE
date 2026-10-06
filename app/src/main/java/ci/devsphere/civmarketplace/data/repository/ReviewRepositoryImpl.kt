package ci.devsphere.civmarketplace.data.repository

import ci.devsphere.civmarketplace.data.model.ReviewDto
import ci.devsphere.civmarketplace.data.model.ReviewableOrderDto
import ci.devsphere.civmarketplace.data.model.SubmitReviewRequest
import ci.devsphere.civmarketplace.data.remote.ReviewService
import ci.devsphere.civmarketplace.domain.repository.ReviewRepository
import ci.devsphere.civmarketplace.util.ApiErrorMapper
import javax.inject.Inject

class ReviewRepositoryImpl @Inject constructor(
    private val reviewService: ReviewService
) : ReviewRepository {

    override suspend fun getReviewsForProduct(productId: Int): Result<List<ReviewDto>> {
        return try {
            Result.success(reviewService.getReviewsForProduct(productId))
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    override suspend fun getReviewableOrders(): Result<List<ReviewableOrderDto>> {
        return try {
            Result.success(reviewService.getReviewableOrders())
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    override suspend fun submitReview(orderId: Int, rating: Int, comment: String?): Result<ReviewDto> {
        return try {
            Result.success(reviewService.submitReview(SubmitReviewRequest(orderId, rating, comment)))
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }
}

