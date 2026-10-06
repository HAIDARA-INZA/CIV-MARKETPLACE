package ci.devsphere.civmarketplace.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerializedName("password_confirmation")
    val passwordConfirmation: String,
    val role: String,
    val phone: String? = null,
    val city: String? = null,
    val commune: String? = null,
    val quarter: String? = null,
    val address: String? = null,
    @SerializedName("seller_selection_mode")
    val sellerSelectionMode: String? = null,
    @SerializedName("preferred_seller_ids")
    val preferredSellerIds: List<Int> = emptyList(),
    @SerializedName("vendor_categories")
    val vendorCategories: List<String> = emptyList()
)

data class AuthResponse(
    @SerializedName(value = "token", alternate = ["access_token"])
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    @SerializedName("email_verified_at")
    val emailVerifiedAt: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val commune: String? = null,
    val quarter: String? = null,
    val address: String? = null,
    @SerializedName(value = "seller_selection_mode", alternate = ["sellerSelectionMode"])
    val sellerSelectionMode: String? = null
)

data class VendorSummaryDto(
    val id: Int,
    val name: String,
    val city: String? = null,
    val commune: String? = null,
    val quarter: String? = null,
    @SerializedName(value = "is_verified", alternate = ["isVerified"])
    val isVerified: Boolean = false,
    @SerializedName(value = "vendor_categories", alternate = ["vendorCategories"])
    val vendorCategories: List<String>? = null
)

