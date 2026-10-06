package ci.devsphere.civmarketplace.ui.screens

import android.util.Log
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ci.devsphere.civmarketplace.data.model.LoginRequest
import ci.devsphere.civmarketplace.data.model.RegisterRequest
import ci.devsphere.civmarketplace.data.model.CategoryDto
import ci.devsphere.civmarketplace.data.model.VendorSummaryDto
import ci.devsphere.civmarketplace.domain.repository.AuthRepository
import ci.devsphere.civmarketplace.domain.repository.ProductRepository
import ci.devsphere.civmarketplace.domain.repository.UserRepository
import ci.devsphere.civmarketplace.util.RoleUtils
import ci.devsphere.civmarketplace.util.MessagingPresenceManager
import ci.devsphere.civmarketplace.util.FcmTokenSyncWorker
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val messagingPresenceManager: MessagingPresenceManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _authState = mutableStateOf<AuthState>(AuthState.Idle)
    val authState: State<AuthState> = _authState

    private val _vendorsState = mutableStateOf<VendorsState>(VendorsState.Idle)
    val vendorsState: State<VendorsState> = _vendorsState

    private val _categoriesState = mutableStateOf<CategoriesState>(CategoriesState.Idle)
    val categoriesState: State<CategoriesState> = _categoriesState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val cleanEmail = email.trim().lowercase()
            val result = authRepository.login(LoginRequest(cleanEmail, password))
            result.onSuccess { response ->
                val isVerified = response.user.emailVerifiedAt != null
                authRepository.saveAuthData(
                    userId = response.user.id,
                    token = response.token,
                    role = response.user.role,
                    name = response.user.name,
                    email = response.user.email,
                    isVerified = isVerified
                )
                registerFcmToken()
                messagingPresenceManager.onAuthenticated()
                _authState.value = if (isVerified) AuthState.Success else AuthState.NeedVerification
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Une erreur est survenue")
            }
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        phone: String,
        city: String,
        commune: String,
        quarter: String,
        address: String,
        sellerSelectionMode: String,
        preferredSellerIds: List<Int>,
        vendorCategories: List<String>
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val cleanName = name.trim()
            val cleanEmail = email.trim().lowercase()
            val cleanPhone = phone.trim()
            val cleanCity = city.trim()
            val cleanCommune = commune.trim()
            val cleanQuarter = quarter.trim()
            val cleanAddress = address.trim()

            when {
                cleanName.isBlank() -> {
                    _authState.value = AuthState.Error("Le nom complet est obligatoire")
                    return@launch
                }
                cleanEmail.isBlank() -> {
                    _authState.value = AuthState.Error("L'e-mail est obligatoire")
                    return@launch
                }
                password.length < 6 -> {
                    _authState.value = AuthState.Error("Le mot de passe doit contenir au moins 6 caractères")
                    return@launch
                }
                cleanCity.isBlank() || cleanCommune.isBlank() || cleanQuarter.isBlank() -> {
                    _authState.value = AuthState.Error("Renseignez votre ville, commune et quartier")
                    return@launch
                }
                RoleUtils.canBuy(role) && sellerSelectionMode == "specific" && preferredSellerIds.isEmpty() -> {
                    _authState.value = AuthState.Error("Choisissez au moins un vendeur ou prenez tous les vendeurs")
                    return@launch
                }
                RoleUtils.canSell(role) && vendorCategories.isEmpty() -> {
                    _authState.value = AuthState.Error("Choisissez au moins une catégorie de vente")
                    return@launch
                }
            }

            val request = RegisterRequest(
                name = cleanName,
                email = cleanEmail,
                password = password,
                passwordConfirmation = password,
                role = role,
                phone = cleanPhone.ifBlank { null },
                city = cleanCity,
                commune = cleanCommune,
                quarter = cleanQuarter,
                address = cleanAddress.ifBlank { null },
                sellerSelectionMode = if (RoleUtils.canBuy(role)) sellerSelectionMode else null,
                preferredSellerIds = if (RoleUtils.canBuy(role) && sellerSelectionMode == "specific") preferredSellerIds else emptyList(),
                vendorCategories = if (RoleUtils.canSell(role)) vendorCategories else emptyList()
            )
            val result = authRepository.register(request)
            result.onSuccess { response ->
                authRepository.saveAuthData(
                    userId = response.user.id,
                    token = response.token,
                    role = response.user.role,
                    name = response.user.name,
                    email = response.user.email,
                    isVerified = false
                )
                registerFcmToken()
                messagingPresenceManager.onAuthenticated()
                _authState.value = AuthState.NeedVerification
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Une erreur est survenue")
            }
        }
    }

    fun loadVendors() {
        if (_vendorsState.value is VendorsState.Loading ||
            _vendorsState.value is VendorsState.Success ||
            _vendorsState.value is VendorsState.Empty
        ) return

        viewModelScope.launch {
            _vendorsState.value = VendorsState.Loading
            Log.d("Vendors", "directory load start endpoint=GET /vendors")
            try {
                val vendors = withTimeout(45_000L) {
                    authRepository.getVendors().getOrThrow()
                }
                _vendorsState.value = if (vendors.isEmpty()) {
                    VendorsState.Empty
                } else {
                    VendorsState.Success(vendors)
                }
                Log.d("Vendors", "directory load success count=${vendors.size}")
            } catch (timeout: TimeoutCancellationException) {
                Log.e("Vendors", "directory load timed out endpoint=GET /vendors", timeout)
                _vendorsState.value = VendorsState.Error("Le chargement a expiré. Réessayez.")
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Log.e("Vendors", "directory load failed endpoint=GET /vendors", error)
                _vendorsState.value = VendorsState.Error(
                    error.message ?: "Impossible de charger les vendeurs"
                )
            }
        }
    }

    fun loadCategories() {
        if (_categoriesState.value is CategoriesState.Loading || _categoriesState.value is CategoriesState.Success) return

        viewModelScope.launch {
            _categoriesState.value = CategoriesState.Loading
            val result = productRepository.getCategories()
            result.onSuccess { categories ->
                _categoriesState.value = CategoriesState.Success(categories)
            }.onFailure { e ->
                _categoriesState.value = CategoriesState.Error(e.message ?: "Impossible de charger les catégories")
            }
        }
    }

    fun verifyEmail(email: String, otp: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.verifyEmail(email, otp)
            result.onSuccess { response ->
                authRepository.saveAuthData(
                    userId = response.user.id,
                    token = response.token,
                    role = response.user.role,
                    name = response.user.name,
                    email = response.user.email,
                    isVerified = true
                )
                registerFcmToken()
                messagingPresenceManager.onAuthenticated()
                _authState.value = AuthState.Success
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Code invalide")
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.forgotPassword(email.trim().lowercase())
            result.onSuccess {
                _authState.value = AuthState.OtpSent
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Erreur lors de l'envoi du code")
            }
        }
    }

    fun resetPassword(email: String, otp: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.resetPassword(email.trim().lowercase(), otp, password)
            result.onSuccess {
                _authState.value = AuthState.ResetSuccess
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Échec de la réinitialisation")
            }
        }
    }

    /** ✅ Renvoie l'OTP de vérification de compte → POST /email/resend */
    fun resendOtp(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.resendOtp(email.trim().lowercase())
            result.onSuccess {
                _authState.value = AuthState.OtpSent
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Impossible de renvoyer le code")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            withTimeoutOrNull(2_000L) {
                userRepository.deleteFcmToken()
            }
            FcmTokenSyncWorker.cancel(context)
            messagingPresenceManager.onLoggedOut()
            authRepository.logout()
        }
    }

    /** Enregistre aussi le token déjà existant, pas seulement ses futurs renouvellements. */
    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNotBlank()) {
                    FcmTokenSyncWorker.enqueue(context, token)
                }
            }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object NeedVerification : AuthState()
    object OtpSent : AuthState()
    object ResetSuccess : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class VendorsState {
    object Idle : VendorsState()
    object Loading : VendorsState()
    object Empty : VendorsState()
    data class Success(val vendors: List<VendorSummaryDto>) : VendorsState()
    data class Error(val message: String) : VendorsState()
}

sealed class CategoriesState {
    object Idle : CategoriesState()
    object Loading : CategoriesState()
    data class Success(val categories: List<CategoryDto>) : CategoriesState()
    data class Error(val message: String) : CategoriesState()
}

