package ci.devsphere.civmarketplace.data.model

import com.google.gson.annotations.SerializedName

data class ProfileDto(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    @SerializedName(value = "email_verified_at", alternate = ["emailVerifiedAt"])
    val emailVerifiedAt: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val commune: String? = null,
    val quarter: String? = null,
    val address: String? = null,
    @SerializedName(value = "seller_selection_mode", alternate = ["sellerSelectionMode"])
    val sellerSelectionMode: String? = null,
    @SerializedName(value = "vendor_categories", alternate = ["vendorCategories"])
    val vendorCategories: List<String>? = null
)

data class UpdateProfileRequest(
    val name: String,
    val email: String,
    val phone: String? = null,
    val city: String,
    val commune: String,
    val quarter: String,
    val address: String? = null,
    @SerializedName("vendor_categories")
    val vendorCategories: List<String> = emptyList()
)

data class UserSettingsDto(
    @SerializedName(value = "notifications_enabled", alternate = ["notificationsEnabled"])
    val notificationsEnabled: Boolean = true,
    @SerializedName(value = "dark_mode_enabled", alternate = ["darkModeEnabled"])
    val darkModeEnabled: Boolean = false,
    val language: String = "fr"
)

data class UpdateUserSettingsRequest(
    @SerializedName("notifications_enabled")
    val notificationsEnabled: Boolean,
    @SerializedName("dark_mode_enabled")
    val darkModeEnabled: Boolean,
    val language: String
)

data class UpdateFcmTokenRequest(
    @SerializedName("fcm_token") val fcmToken: String,
    @SerializedName("device_id") val deviceId: String,
    val platform: String = "android",
    @SerializedName("app_version") val appVersion: String
)

data class DeleteFcmTokenRequest(
    @SerializedName("device_id") val deviceId: String
)

data class NotificationDto(
    val id: Int,
    val type: String? = null,
    val title: String,
    val body: String,
    @SerializedName(value = "is_read", alternate = ["isRead"])
    val isRead: Boolean = false,
    @SerializedName(value = "created_at", alternate = ["createdAt"])
    val createdAt: String
)

