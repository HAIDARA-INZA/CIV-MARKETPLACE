package ci.devsphere.civmarketplace.data.remote

import ci.devsphere.civmarketplace.data.model.NotificationDto
import ci.devsphere.civmarketplace.data.model.ProfileDto
import ci.devsphere.civmarketplace.data.model.UpdateProfileRequest
import ci.devsphere.civmarketplace.data.model.UpdateUserSettingsRequest
import ci.devsphere.civmarketplace.data.model.UserSettingsDto
import ci.devsphere.civmarketplace.data.model.UpdateFcmTokenRequest
import ci.devsphere.civmarketplace.data.model.DeleteFcmTokenRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserService {
    @GET("me")
    suspend fun getProfile(): ProfileDto

    @PUT("me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ProfileDto

    @POST("fcm-token")
    suspend fun updateFcmToken(@Body request: UpdateFcmTokenRequest)

    @HTTP(method = "DELETE", path = "fcm-token", hasBody = true)
    suspend fun deleteFcmToken(@Body request: DeleteFcmTokenRequest)

    @GET("settings")
    suspend fun getSettings(): UserSettingsDto

    @PUT("settings")
    suspend fun updateSettings(@Body request: UpdateUserSettingsRequest): UserSettingsDto

    @GET("notifications")
    suspend fun getNotifications(): List<NotificationDto>

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Int): NotificationDto

    @PATCH("notifications/read-all")
    suspend fun markAllNotificationsRead(): Map<String, String>
}

