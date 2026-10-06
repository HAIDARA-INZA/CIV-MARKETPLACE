package ci.devsphere.civmarketplace.ui.screens

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ci.devsphere.civmarketplace.data.model.CartItemRequest
import ci.devsphere.civmarketplace.data.model.CheckoutRequest
import ci.devsphere.civmarketplace.di.PaymentResultBus
import ci.devsphere.civmarketplace.domain.repository.PaymentRepository
import ci.devsphere.civmarketplace.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _state = mutableStateOf<CheckoutState>(CheckoutState.Idle)
    val state: State<CheckoutState> = _state

    init {
        // Écouter le bus Wave dès que le ViewModel est créé
        observePaymentResult()
    }

    fun startCheckout() {
        viewModelScope.launch {
            _state.value = CheckoutState.Loading

            val cartItems = productRepository.getCartItems().first()
            val totalAmount = cartItems.sumOf { it.price.coerceAtLeast(0.0) * it.quantity.coerceAtLeast(1) }

            if (cartItems.isEmpty()) {
                _state.value = CheckoutState.Error("Votre panier est vide")
                return@launch
            }

            if (totalAmount <= 0.0) {
                _state.value = CheckoutState.Error("Le montant du panier est invalide")
                return@launch
            }

            val request = CheckoutRequest(
                items = cartItems.map {
                    CartItemRequest(
                        productId = it.id,
                        name = it.name,
                        quantity = it.quantity,
                        unitPrice = it.price,
                        totalPrice = it.price * it.quantity
                    )
                },
                totalAmount = totalAmount
            )

            val result = paymentRepository.createCheckout(request)
            result.onSuccess { response ->
                // Le panier est conservé tant que Wave n'a pas confirmé le paiement.
                _state.value = CheckoutState.WaitingForPayment(response.checkoutUrl)
            }.onFailure { e ->
                _state.value = CheckoutState.Error(e.message ?: "Erreur lors de la création du paiement")
            }
        }
    }

    /**
     * Écoute le bus PaymentResultBus et met à jour l'état selon
     * le résultat renvoyé par Wave via deep link.
     */
    private fun observePaymentResult() {
        viewModelScope.launch {
            PaymentResultBus.result.collect { status ->
                when (status) {
                    PaymentResultBus.Status.SUCCESS -> {
                        _state.value = CheckoutState.PaymentPendingConfirmation
                    }

                    PaymentResultBus.Status.FAILED ->
                        _state.value = CheckoutState.PaymentFailed("Le paiement a échoué. Veuillez réessayer.")

                    PaymentResultBus.Status.CANCELLED ->
                        _state.value = CheckoutState.PaymentCancelled
                }
            }
        }
    }

    fun resetToIdle() {
        _state.value = CheckoutState.Idle
    }
}

sealed class CheckoutState {
    /** État initial */
    object Idle : CheckoutState()

    /** Création de la session en cours */
    object Loading : CheckoutState()

    /**
     * Session Wave créée — l'URL est ouverte dans le navigateur.
     * L'app attend le deep link de retour Wave.
     */
    data class WaitingForPayment(val url: String) : CheckoutState()

    /** Deep link reçu : paiement confirmé ✅ */
    object PaymentPendingConfirmation : CheckoutState()

    /** Deep link reçu : paiement échoué ❌ */
    data class PaymentFailed(val reason: String) : CheckoutState()

    /** Utilisateur a annulé (retour sans payer) */
    object PaymentCancelled : CheckoutState()

    /** Erreur technique (réseau, backend…) */
    data class Error(val message: String) : CheckoutState()
}

