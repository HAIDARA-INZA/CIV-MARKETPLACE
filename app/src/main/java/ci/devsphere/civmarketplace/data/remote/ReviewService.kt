package ci.devsphere.civmarketplace.data.remote

import ci.devsphere.civmarketplace.data.model.ReviewDto
import ci.devsphere.civmarketplace.data.model.ReviewableOrderDto
import ci.devsphere.civmarketplace.data.model.SubmitReviewRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ReviewService {
    @GET("products/{id}/reviews")
    suspend fun getReviewsForProduct(@Path("id") productId: Int): List<ReviewDto>

    @GET("orders/reviewable")
    suspend fun getReviewableOrders(): List<ReviewableOrderDto>

    @POST("reviews")
    suspend fun submitReview(@Body request: SubmitReviewRequest): ReviewDto
}

