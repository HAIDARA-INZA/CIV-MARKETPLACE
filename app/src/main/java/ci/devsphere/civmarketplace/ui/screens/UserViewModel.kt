package ci.devsphere.civmarketplace.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ci.devsphere.civmarketplace.data.local.TokenManager
import ci.devsphere.civmarketplace.data.model.NotificationDto
import ci.devsphere.civmarketplace.data.model.ProfileDto
import ci.devsphere.civmarketplace.data.model.UpdateProfileRequest
import ci.devsphere.civmarketplace.data.model.UpdateUserSettingsRequest
import ci.devsphere.civmarketplace.data.model.UserSettingsDto
import ci.devsphere.civmarketplace.di.NetworkStatusBus
import ci.devsphere.civmarketplace.di.RealtimeStatusBus
import ci.devsphere.civmarketplace.domain.repository.UserRepository
import ci.devsphere.civmarketplace.di.SyncStatusBus
import ci.devsphere.civmarketplace.util.PusherManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val tokenManager: TokenManager,
    private val pusherManager: PusherManager
) : ViewModel() {

    private val _profileState = mutableStateOf<ProfileState>(ProfileState.Loading)
    val profileState: State<ProfileState> = _profileState

    private val _settingsState = mutableStateOf<SettingsState>(SettingsState.Loading)
    val settingsState: State<SettingsState> = _settingsState

    private val _notificationsState =
        mutableStateOf<NotificationsState>(NotificationsState.Loading)
    val notificationsState: State<NotificationsState> = _notificationsState

    private var profileLoadJob: Job? = null
    private var settingsLoadJob: Job? = null
    private var notificationsLoadJob: Job? = null

    init {
        loadProfile()
        loadSettings()
        setupRealtime()
        observeReconnection()
        observeRealtimeReconnection()
    }

    private fun observeReconnection() {
        NetworkStatusBus.reconnected
            .onEach {
                loadProfile()
                loadSettings()
                loadNotifications()
            }
            .launchIn(viewModelScope)
    }

    private fun observeRealtimeReconnection() {
        RealtimeStatusBus.reconnected
            .onEach { setupRealtime() }
            .launchIn(viewModelScope)
    }

    fun loadProfile() {
        if (profileLoadJob?.isActive == true) return
        profileLoadJob = viewModelScope.launch {
            _profileState.value = ProfileState.Loading

            userRepository.getProfile()
                .onSuccess { profile ->
                    _profileState.value = ProfileState.Success(profile)
                }
                .onFailure { error ->
                    _profileState.value = ProfileState.Error(
                        error.message ?: "Impossible de charger le profil"
                    )
                }
        }
    }

    fun updateProfile(
        name: String,
        email: String,
        phone: String,
        city: String,
        commune: String,
        quarter: String,
        address: String,
        vendorCategories: List<String>
    ) {
        viewModelScope.launch {
            val cleanName = name.trim()
            val cleanEmail = email.trim().lowercase()
            val cleanCity = city.trim()
            val cleanCommune = commune.trim()
            val cleanQuarter = quarter.trim()

            when {
                cleanName.isBlank() -> {
                    _profileState.value = ProfileState.Error("Le nom est obligatoire")
                    return@launch
                }

                cleanEmail.isBlank() -> {
                    _profileState.value = ProfileState.Error("L'e-mail est obligatoire")
                    return@launch
                }

                cleanCity.isBlank() ||
                        cleanCommune.isBlank() ||
                        cleanQuarter.isBlank() -> {
                    _profileState.value = ProfileState.Error(
                        "Ville, commune et quartier sont obligatoires"
                    )
                    return@launch
                }
            }

            _profileState.value = ProfileState.Saving

            val request = UpdateProfileRequest(
                name = cleanName,
                email = cleanEmail,
                phone = phone.trim().ifBlank { null },
                city = cleanCity,
                commune = cleanCommune,
                quarter = cleanQuarter,
                address = address.trim().ifBlank { null },
                vendorCategories = vendorCategories
            )

            userRepository.updateProfile(request)
                .onSuccess { profile ->
                    _profileState.value = ProfileState.Updated(profile)
                }
                .onFailure { error ->
                    _profileState.value = ProfileState.Error(
                        error.message ?: "Modification impossible"
                    )
                }
        }
    }

    fun loadSettings() {
        if (settingsLoadJob?.isActive == true) return
        settingsLoadJob = viewModelScope.launch {
            _settingsState.value = SettingsState.Loading

            userRepository.getSettings()
                .onSuccess { settings ->
                    _settingsState.value = SettingsState.Success(settings)
                    tokenManager.setDarkMode(settings.darkModeEnabled)
                }
                .onFailure { error ->
                    _settingsState.value = SettingsState.Error(
                        error.message ?: "Impossible de charger les paramètres"
                    )
                }
        }
    }

    fun updateSettings(settings: UserSettingsDto) {
        val localeTag = if (settings.language == "en") "en" else "fr"
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(localeTag)
        )

        viewModelScope.launch {
            tokenManager.setDarkMode(settings.darkModeEnabled)
            _settingsState.value = SettingsState.Saving(settings)

            val request = UpdateUserSettingsRequest(
                notificationsEnabled = settings.notificationsEnabled,
                darkModeEnabled = settings.darkModeEnabled,
                language = settings.language
            )

            userRepository.updateSettings(request)
                .onSuccess { updatedSettings ->
                    _settingsState.value = SettingsState.Success(updatedSettings)
                }
                .onFailure { error ->
                    _settingsState.value = SettingsState.Error(
                        error.message ?: "Paramètres non enregistrés"
                    )
                }
        }
    }

    fun loadNotifications() {
        if (notificationsLoadJob?.isActive == true) return
        notificationsLoadJob = viewModelScope.launch {
            _notificationsState.value = NotificationsState.Loading

            userRepository.getNotifications()
                .onSuccess { notifications ->
                    _notificationsState.value =
                        NotificationsState.Success(notifications)
                }
                .onFailure { error ->
                    _notificationsState.value = NotificationsState.Error(
                        error.message ?: "Impossible de charger les notifications"
                    )
                }
        }
    }

    init {
        viewModelScope.launch {
            SyncStatusBus.synced.collect {
                loadNotifications()
            }
        }
    }

    fun markNotificationRead(id: Int) {
        viewModelScope.launch {
            userRepository.markNotificationRead(id)
                .onSuccess {
                    loadNotifications()
                }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            userRepository.markAllNotificationsRead()
                .onSuccess {
                    loadNotifications()
                }
        }
    }

    private fun setupRealtime() {
        pusherManager.init()

        viewModelScope.launch {
            val userId = tokenManager.getUserId().first() ?: return@launch
            pusherManager.subscribeToPrivateChannel(userId, "notification-created") {
                loadNotifications()
            }
        }
    }
}

sealed class ProfileState {
    object Loading : ProfileState()
    object Saving : ProfileState()
    data class Success(val profile: ProfileDto) : ProfileState()
    data class Updated(val profile: ProfileDto) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class SettingsState {
    object Loading : SettingsState()
    data class Saving(val settings: UserSettingsDto) : SettingsState()
    data class Success(val settings: UserSettingsDto) : SettingsState()
    data class Error(val message: String) : SettingsState()
}

sealed class NotificationsState {
    object Loading : NotificationsState()
    data class Success(val notifications: List<NotificationDto>) : NotificationsState()
    data class Error(val message: String) : NotificationsState()
}

