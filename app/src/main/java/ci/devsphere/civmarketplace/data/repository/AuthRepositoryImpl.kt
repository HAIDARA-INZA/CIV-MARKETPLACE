package ci.devsphere.civmarketplace.data.repository

import ci.devsphere.civmarketplace.data.local.TokenManager
import ci.devsphere.civmarketplace.data.model.AuthResponse
import ci.devsphere.civmarketplace.data.model.LoginRequest
import ci.devsphere.civmarketplace.data.model.RegisterRequest
import ci.devsphere.civmarketplace.data.model.VendorSummaryDto
import ci.devsphere.civmarketplace.data.remote.AuthService
import ci.devsphere.civmarketplace.data.remote.VerifyEmailRequest
import ci.devsphere.civmarketplace.domain.repository.AuthRepository
import ci.devsphere.civmarketplace.util.ApiErrorMapper
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            Result.success(authService.login(request))
        } catch (e: Exception) {
            Result.failure(handleError(e))
        }
    }

    override suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return try {
            Result.success(authService.register(request))
        } catch (e: Exception) {
            Result.failure(handleError(e))
        }
    }

    override suspend fun verifyEmail(email: String, otp: String): Result<AuthResponse> {
        return try {
            Result.success(authService.verifyEmail(VerifyEmailRequest(email, otp)))
        } catch (e: Exception) {
            Result.failure(handleError(e))
        }
    }

    override suspend fun forgotPassword(email: String): Result<Unit> {
        return try {
            val response = authService.forgotPassword(mapOf("email" to email))
            if (response["otp_sent"].equals("true", ignoreCase = true)) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response["message"] ?: "Le code n'a pas pu être envoyé. Réessayez."))
            }
        } catch (e: Exception) {
            Result.failure(handleError(e))
        }
    }

    /** ✅ Renvoyer OTP de vérification → POST /email/resend (≠ /password/forgot) */
    override suspend fun resendOtp(email: String): Result<Unit> {
        return try {
            val response = authService.resendOtp(mapOf("email" to email))
            if (response["otp_sent"].equals("true", ignoreCase = true)) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response["message"] ?: "Le code n'a pas pu être envoyé. Réessayez."))
            }
        } catch (e: Exception) {
            Result.failure(handleError(e))
        }
    }

    override suspend fun resetPassword(email: String, otp: String, password: String): Result<Unit> {
        return try {
            authService.resetPassword(mapOf(
                "email"                 to email,
                "otp"                   to otp,
                "password"              to password,
                "password_confirmation" to password
            ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(handleError(e))
        }
    }

    override suspend fun getVendors(): Result<List<VendorSummaryDto>> {
        return try {
            Result.success(authService.getVendors())
        } catch (e: Exception) {
            Result.failure(handleError(e))
        }
    }

    override suspend fun saveAuthData(
        userId: Int, token: String, role: String,
        name: String, email: String, isVerified: Boolean
    ) {
        tokenManager.saveAuthData(userId, token, role, name, email, isVerified)
        // Mark onboarding completed when user logs in or registers successfully
        tokenManager.setOnboardingCompleted(true)
    }

    override suspend fun getUserId(): Int?      = tokenManager.getUserId().first()
    override suspend fun getName(): String?      = tokenManager.getName().first()
    override suspend fun getToken(): String?    = tokenManager.getToken().first()
    override suspend fun getRole(): String?     = tokenManager.getRole().first()
    override suspend fun getEmail(): String?    = tokenManager.getEmail().first()
    override suspend fun isEmailVerified(): Boolean = tokenManager.isVerified().first()

    override suspend fun logout() {
        tokenManager.clearAuthData()
    }

    private fun oldHandleError(e: Exception): Exception {
        if (e is HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            if (errorBody != null) {
                return try {
                    val msg = JSONObject(errorBody).getString("message")
                    Exception(msg)
                } catch (_: Exception) { e }
            }
        }
        return e
    }

    private fun handleError(e: Exception): Exception = ApiErrorMapper.toException(e)
}

