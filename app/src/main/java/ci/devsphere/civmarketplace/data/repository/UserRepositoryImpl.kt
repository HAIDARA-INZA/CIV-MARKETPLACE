package ci.devsphere.civmarketplace.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import ci.devsphere.civmarketplace.BuildConfig
import ci.devsphere.civmarketplace.data.local.TokenManager
import ci.devsphere.civmarketplace.data.model.DeleteFcmTokenRequest
import ci.devsphere.civmarketplace.data.model.NotificationDto
import ci.devsphere.civmarketplace.data.model.ProfileDto
import ci.devsphere.civmarketplace.data.model.UpdateProfileRequest
import ci.devsphere.civmarketplace.data.model.UpdateUserSettingsRequest
import ci.devsphere.civmarketplace.data.model.UpdateFcmTokenRequest
import ci.devsphere.civmarketplace.data.model.UserSettingsDto
import ci.devsphere.civmarketplace.data.remote.UserService
import ci.devsphere.civmarketplace.domain.repository.UserRepository
import ci.devsphere.civmarketplace.util.ApiErrorMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userService: UserService,
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager
) : UserRepository {

    override suspend fun getProfile(): Result<ProfileDto> {
        return runCatching { userService.getProfile() }.mapError()
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): Result<ProfileDto> {
        return runCatching { userService.updateProfile(request) }.mapError()
    }

    override suspend fun updateFcmToken(token: String): Result<Unit> {
        return runCatching {
            userService.updateFcmToken(
                UpdateFcmTokenRequest(
                    fcmToken = token,
                    deviceId = deviceId(),
                    appVersion = BuildConfig.VERSION_NAME
                )
            )
            Unit
        }.mapError()
    }

    override suspend fun deleteFcmToken(): Result<Unit> {
        return runCatching {
            userService.deleteFcmToken(DeleteFcmTokenRequest(deviceId()))
            Unit
        }.mapError()
    }

    override suspend fun getSettings(): Result<UserSettingsDto> {
        return runCatching { userService.getSettings() }.mapError()
    }

    override suspend fun updateSettings(request: UpdateUserSettingsRequest): Result<UserSettingsDto> {
        return runCatching { userService.updateSettings(request) }.mapError()
    }

    override suspend fun getNotifications(): Result<List<NotificationDto>> {
        return runCatching { userService.getNotifications() }.mapError()
    }

    override suspend fun markNotificationRead(id: Int): Result<NotificationDto> {
        return runCatching { userService.markNotificationRead(id) }.mapError()
    }

    override suspend fun markAllNotificationsRead(): Result<Unit> {
        return runCatching {
            userService.markAllNotificationsRead()
            Unit
        }.mapError()
    }

    private fun <T> Result<T>.mapError(): Result<T> {
        return fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(ApiErrorMapper.toException(it)) }
        )
    }

    private fun handleError(error: Throwable): Exception {
        if (error is HttpException) {
            if (error.code() >= 500) {
                return Exception("Le service est temporairement indisponible. Réessayez dans un instant.")
            }
            val errorBody = error.response()?.errorBody()?.string()
            if (errorBody != null) {
                try {
                    val jsonObject = JSONObject(errorBody)
                    val message = jsonObject.optString("message")
                    val errors = jsonObject.optJSONObject("errors")
                    val fieldMessage = errors?.keys()?.asSequence()?.firstOrNull()?.let { key ->
                        errors.optJSONArray(key)?.optString(0)
                    }
                    return Exception(fieldMessage?.takeUnless { it.startsWith("validation.") } ?: message.ifBlank { "La demande n'a pas pu être traitée." })
                } catch (_: Exception) {
                }
            }
        }
        return Exception(error.message ?: "Erreur reseau")
    }

    private suspend fun deviceId(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf(String::isNotBlank)
            ?: "${Build.MANUFACTURER}-${Build.MODEL}-${tokenManager.getUserId().first().orZero()}"

    private fun Int?.orZero(): Int = this ?: 0
}

