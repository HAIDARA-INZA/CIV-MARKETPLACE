package ci.devsphere.civmarketplace.data.remote

import ci.devsphere.civmarketplace.data.model.CheckoutRequest
import ci.devsphere.civmarketplace.data.model.CheckoutResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface PaymentService {
    @POST("checkout")
    suspend fun createCheckout(@Body request: CheckoutRequest): CheckoutResponse
}

