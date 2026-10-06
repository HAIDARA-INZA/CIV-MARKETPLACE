package ci.devsphere.civmarketplace.di

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bus global émettant un pulse à chaque fois que la connexion réseau revient
 * après une coupure (Bug 4 — auto-refresh à la reconnexion).
 *
 * Les ViewModels qui affichent des données pouvant échouer hors-ligne
 * (Home, Messages, Commandes...) collectent [reconnected] et relancent
 * leur propre fonction de rafraîchissement.
 *
 * Émis par [ci.devsphere.civmarketplace.util.NetworkMonitor] uniquement,
 * jamais directement par l'UI.
 */
object NetworkStatusBus {
    private val _reconnected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reconnected: SharedFlow<Unit> = _reconnected.asSharedFlow()

    suspend fun emitReconnected() {
        _reconnected.emit(Unit)
    }
}

