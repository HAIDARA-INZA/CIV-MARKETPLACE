package ci.devsphere.civmarketplace.domain.repository

import ci.devsphere.civmarketplace.data.model.CheckoutRequest
import ci.devsphere.civmarketplace.data.model.CheckoutResponse

interface PaymentRepository {
    suspend fun createCheckout(request: CheckoutRequest): Result<CheckoutResponse>
}

