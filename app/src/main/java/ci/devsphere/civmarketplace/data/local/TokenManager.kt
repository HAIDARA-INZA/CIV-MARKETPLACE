package ci.devsphere.civmarketplace.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("devsphere_prefs")

@Singleton
class TokenManager @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private val USER_ID_KEY = intPreferencesKey("user_id")
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val ROLE_KEY = stringPreferencesKey("user_role")
        private val NAME_KEY = stringPreferencesKey("user_name")
        private val EMAIL_KEY = stringPreferencesKey("user_email")
        private val VERIFIED_KEY = booleanPreferencesKey("email_verified")
        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_pref")
        private val LAST_SYNC_AT_KEY = stringPreferencesKey("last_sync_at")
        private val IGNORED_APP_UPDATE_VERSION_KEY = intPreferencesKey("ignored_app_update_version_code")
        private val SPACE_SWITCH_IS_LEFT_KEY = booleanPreferencesKey("space_switch_is_left")
        private val SPACE_SWITCH_Y_RATIO_KEY = androidx.datastore.preferences.core.floatPreferencesKey("space_switch_y_ratio")
    }

    fun getUserId(): Flow<Int?> = context.dataStore.data.map { it[USER_ID_KEY] }
    fun getToken(): Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    fun getRole(): Flow<String?> = context.dataStore.data.map { it[ROLE_KEY] }
    fun getName(): Flow<String?> = context.dataStore.data.map { it[NAME_KEY] }
    fun getEmail(): Flow<String?> = context.dataStore.data.map { it[EMAIL_KEY] }
    fun isVerified(): Flow<Boolean> = context.dataStore.data.map { it[VERIFIED_KEY] ?: false }
    fun isOnboardingCompleted(): Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_COMPLETED_KEY] ?: false }
    fun getDarkMode(): Flow<Boolean?> = context.dataStore.data.map { it[DARK_MODE_KEY] }
    fun getLastSyncAt(): Flow<String?> = context.dataStore.data.map { it[LAST_SYNC_AT_KEY] }
    fun getIgnoredAppUpdateVersionCode(): Flow<Int?> = context.dataStore.data.map { it[IGNORED_APP_UPDATE_VERSION_KEY] }

    suspend fun setIgnoredAppUpdateVersionCode(versionCode: Int?) {
        context.dataStore.edit { preferences ->
            if (versionCode == null) {
                preferences.remove(IGNORED_APP_UPDATE_VERSION_KEY)
            } else {
                preferences[IGNORED_APP_UPDATE_VERSION_KEY] = versionCode
            }
        }
    }

    /** Bouton flottant "changer d'espace" : position mémorisée (côté gauche/droit + hauteur relative). */
    fun getSpaceSwitchIsLeft(): Flow<Boolean> = context.dataStore.data.map { it[SPACE_SWITCH_IS_LEFT_KEY] ?: true }
    fun getSpaceSwitchYRatio(): Flow<Float> = context.dataStore.data.map { it[SPACE_SWITCH_Y_RATIO_KEY] ?: 0.08f }

    suspend fun saveSpaceSwitchPosition(isLeft: Boolean, yRatio: Float) {
        context.dataStore.edit { preferences ->
            preferences[SPACE_SWITCH_IS_LEFT_KEY] = isLeft
            preferences[SPACE_SWITCH_Y_RATIO_KEY] = yRatio.coerceIn(0f, 1f)
        }
    }

    suspend fun saveAuthData(userId: Int, token: String, role: String, name: String, email: String, isVerified: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[TOKEN_KEY] = token
            preferences[ROLE_KEY] = role
            preferences[NAME_KEY] = name
            preferences[EMAIL_KEY] = email
            preferences[VERIFIED_KEY] = isVerified
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    suspend fun saveLastSyncAt(value: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SYNC_AT_KEY] = value
        }
    }

    suspend fun clearAuthData() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_ID_KEY)
            preferences.remove(TOKEN_KEY)
            preferences.remove(ROLE_KEY)
            preferences.remove(NAME_KEY)
            preferences.remove(EMAIL_KEY)
            preferences.remove(VERIFIED_KEY)
            preferences.remove(LAST_SYNC_AT_KEY)
        }
    }
}

