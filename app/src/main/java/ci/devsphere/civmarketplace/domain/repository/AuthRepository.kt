package ci.devsphere.civmarketplace.domain.repository

import ci.devsphere.civmarketplace.data.model.AuthResponse
import ci.devsphere.civmarketplace.data.model.LoginRequest
import ci.devsphere.civmarketplace.data.model.RegisterRequest
import ci.devsphere.civmarketplace.data.model.VendorSummaryDto

interface AuthRepository {
    suspend fun login(request: LoginRequest): Result<AuthResponse>
    suspend fun register(request: RegisterRequest): Result<AuthResponse>
    suspend fun verifyEmail(email: String, otp: String): Result<AuthResponse>
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun resendOtp(email: String): Result<Unit>
    suspend fun resetPassword(email: String, otp: String, password: String): Result<Unit>
    suspend fun getVendors(): Result<List<VendorSummaryDto>>
    suspend fun saveAuthData(userId: Int, token: String, role: String, name: String, email: String, isVerified: Boolean)
    suspend fun getUserId(): Int?
    suspend fun getName(): String?
    suspend fun getToken(): String?
    suspend fun getRole(): String?
    suspend fun getEmail(): String?
    suspend fun isEmailVerified(): Boolean
    suspend fun logout()
}

