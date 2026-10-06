package ci.devsphere.civmarketplace.domain.repository

import ci.devsphere.civmarketplace.data.model.NotificationDto
import ci.devsphere.civmarketplace.data.model.ProfileDto
import ci.devsphere.civmarketplace.data.model.UpdateProfileRequest
import ci.devsphere.civmarketplace.data.model.UpdateUserSettingsRequest
import ci.devsphere.civmarketplace.data.model.UserSettingsDto

interface UserRepository {
    suspend fun getProfile(): Result<ProfileDto>
    suspend fun updateProfile(request: UpdateProfileRequest): Result<ProfileDto>
    suspend fun updateFcmToken(token: String): Result<Unit>
    suspend fun deleteFcmToken(): Result<Unit>
    suspend fun getSettings(): Result<UserSettingsDto>
    suspend fun updateSettings(request: UpdateUserSettingsRequest): Result<UserSettingsDto>
    suspend fun getNotifications(): Result<List<NotificationDto>>
    suspend fun markNotificationRead(id: Int): Result<NotificationDto>
    suspend fun markAllNotificationsRead(): Result<Unit>
}

