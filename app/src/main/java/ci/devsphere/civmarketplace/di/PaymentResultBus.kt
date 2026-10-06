package ci.devsphere.civmarketplace.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Bus global pour recevoir le résultat du paiement Wave via deep link.
 *
 * Flux complet :
 *   1. L'utilisateur paie sur Wave (Chrome Custom Tab)
 *   2. Wave redirige vers  civapp://payment/return?status=success|failed|cancelled
 *   3. MainActivity.onNewIntent() intercepte l'URI
 *   4. MainActivity appelle PaymentResultBus.emit(status)
 *   5. CheckoutScreen observe le bus et affiche le résultat
 *
 * Configuration Wave (backend Laravel) :
 *   → Dans l'appel à l'API Wave, passer :
 *       "success_url" => "civapp://payment/return?status=success",
 *       "error_url"   => "civapp://payment/return?status=failed"
 */
object PaymentResultBus {

    enum class Status { SUCCESS, FAILED, CANCELLED }

    private val _result = MutableSharedFlow<Status>(extraBufferCapacity = 1)
    val result: SharedFlow<Status> = _result.asSharedFlow()

    fun emit(status: Status) {
        CoroutineScope(Dispatchers.Main).launch {
            _result.emit(status)
        }
    }
}

