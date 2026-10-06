package ci.devsphere.civmarketplace.data.repository

import ci.devsphere.civmarketplace.data.model.CheckoutRequest
import ci.devsphere.civmarketplace.data.model.CheckoutResponse
import ci.devsphere.civmarketplace.data.remote.PaymentService
import ci.devsphere.civmarketplace.domain.repository.PaymentRepository
import ci.devsphere.civmarketplace.util.ApiErrorMapper
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

class PaymentRepositoryImpl @Inject constructor(
    private val paymentService: PaymentService
) : PaymentRepository {
    override suspend fun createCheckout(request: CheckoutRequest): Result<CheckoutResponse> {
        return try {
            // Ajout d'un log ou vérification ici si nécessaire
            val response = paymentService.createCheckout(request)
            if (response.checkoutUrl.isNotEmpty()) {
                Result.success(response)
            } else {
                Result.failure(Exception("URL de paiement non générée"))
            }
        } catch (e: Exception) {
            val error = ApiErrorMapper.toException(e)
            if (error.message?.contains("Unable to resolve host") == true) {
                Result.failure(Exception("Serveur PayDunya injoignable. Vérifiez votre connexion."))
            } else {
                Result.failure(error)
            }
        }
    }

    private fun handleError(e: Exception): Exception {
        if (e is HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            if (errorBody != null) {
                try {
                    val jsonObject = JSONObject(errorBody)
                    return Exception(jsonObject.getString("message"))
                } catch (jsonException: Exception) {
                }
            }
        }
        return e
    }
}

