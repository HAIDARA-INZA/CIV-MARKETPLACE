package ci.devsphere.civmarketplace.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ci.devsphere.civmarketplace.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import ci.devsphere.civmarketplace.data.local.TokenManager
import kotlinx.coroutines.flow.first

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager // AJOUTÉ
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<SplashEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            delay(2000)
            
            // On vérifie d'abord si l'onboarding est terminé
            val onboardingCompleted = tokenManager.isOnboardingCompleted().first()
            if (!onboardingCompleted) {
                _eventFlow.emit(SplashEvent.NavigateToOnboarding)
                return@launch
            }

            val token = authRepository.getToken()
            if (token != null) {
                val isVerified = authRepository.isEmailVerified()
                if (isVerified) {
                    val role = authRepository.getRole() ?: "client"
                    _eventFlow.emit(SplashEvent.NavigateToHome(role))
                } else {
                    val email = authRepository.getEmail() ?: ""
                    _eventFlow.emit(SplashEvent.NavigateToVerification(email))
                }
            } else {
                _eventFlow.emit(SplashEvent.NavigateToLogin)
            }
        }
    }

    fun completeOnboarding(onCompleted: () -> Unit) {
        viewModelScope.launch {
            tokenManager.setOnboardingCompleted(true)
            onCompleted()
        }
    }

    suspend fun isOnboardingCompletedOnce(): Boolean {
        return tokenManager.isOnboardingCompleted().first()
    }

    sealed class SplashEvent {
        object NavigateToOnboarding : SplashEvent() // AJOUTÉ
        object NavigateToLogin : SplashEvent()
        data class NavigateToHome(val role: String) : SplashEvent()
        data class NavigateToVerification(val email: String) : SplashEvent()
    }
}

