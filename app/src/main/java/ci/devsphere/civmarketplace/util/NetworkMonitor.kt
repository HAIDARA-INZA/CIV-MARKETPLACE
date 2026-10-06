package ci.devsphere.civmarketplace.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.SystemClock
import android.util.Log
import ci.devsphere.civmarketplace.BuildConfig
import ci.devsphere.civmarketplace.di.NetworkStatusBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Détection globale de l'état de connexion réseau (Bug 2 & 4).
 *
 * - [isOnline] suit la validation ConnectivityManager, avec les échecs API
 *   utilisés uniquement pour déclencher une vérification HTTP secondaire.
 * - [showOffline] n'est activé qu'après une coupure confirmée et amortit le retour.
 * - Émet [NetworkStatusBus.reconnected] une fois après une coupure confirmée.
 *
 * Singleton Hilt : une seule instance/un seul callback pour toute l'app.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val OFFLINE_CONFIRMATION_MS = 6_000L
        private const val RECOVERY_CONFIRMATION_MS = 1_000L
        private const val API_FAILURE_WINDOW_MS = 10_000L
        private const val API_FAILURE_THRESHOLD = 3
        private const val MAX_API_PROBE_BACKOFF_MS = 30_000L
        private const val TAG = "NetworkMonitor"
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val stateLock = Any()
    private val apiFailureLock = Any()

    private val initialNetwork = connectivityManager.activeNetwork
    private val _isOnline = MutableStateFlow(initialNetwork?.let(::currentlyConnected) == true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    private val _showOffline = MutableStateFlow(false)
    val showOffline: StateFlow<Boolean> = _showOffline.asStateFlow()

    private var currentNetwork: Network? = initialNetwork
    private var apiUnavailable = false
    private var offlineVisibilityJob: Job? = null
    private var onlineVisibilityJob: Job? = null
    private var apiProbeJob: Job? = null
    private var apiFailureCount = 0
    private var apiFailureWindowStartedAt = 0L

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (isValidated(capabilities)) {
                onValidatedNetworkAvailable(network)
            } else if (connectivityManager.activeNetwork == network) {
                setConnectivityOnline(false)
            }
        }

        override fun onLost(network: Network) {
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null && currentlyConnected(activeNetwork)) {
                onValidatedNetworkAvailable(activeNetwork)
            } else {
                synchronized(stateLock) {
                    currentNetwork = activeNetwork
                    apiUnavailable = false
                }
                setConnectivityOnline(false)
            }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            if (isValidated(networkCapabilities)) {
                onValidatedNetworkAvailable(network)
            } else if (connectivityManager.activeNetwork == network) {
                synchronized(stateLock) {
                    currentNetwork = network
                    apiUnavailable = false
                }
                setConnectivityOnline(false)
            }
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching {
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }
        if (_isOnline.value) {
            synchronized(stateLock) { currentNetwork = initialNetwork }
        } else {
            scheduleOfflineVisibility()
        }
    }

    fun refresh() {
        val network = connectivityManager.activeNetwork
        if (network == null || !currentlyConnected(network)) {
            setConnectivityOnline(false)
            return
        }

        val shouldProbeApi = synchronized(stateLock) {
            currentNetwork = network
            apiUnavailable
        }
        if (shouldProbeApi) {
            startApiReachabilityChecks(network)
        } else {
            setConnectivityOnline(true)
        }
    }

    fun recordApiFailure() {
        val now = SystemClock.elapsedRealtime()
        val (failureCount, shouldProbe) = synchronized(apiFailureLock) {
            if (now - apiFailureWindowStartedAt > API_FAILURE_WINDOW_MS) {
                apiFailureWindowStartedAt = now
                apiFailureCount = 0
            }

            apiFailureCount++
            if (apiFailureCount < API_FAILURE_THRESHOLD) {
                apiFailureCount to false
            } else {
                apiFailureCount = 0
                apiFailureWindowStartedAt = 0L
                API_FAILURE_THRESHOLD to true
            }
        }

        Log.w(TAG, "Consecutive API network failures: $failureCount/$API_FAILURE_THRESHOLD")
        if (shouldProbe) {
            Log.w(TAG, "API failure threshold reached; checking endpoint reachability")
            val network = connectivityManager.activeNetwork ?: return
            if (currentlyConnected(network)) {
                startApiReachabilityChecks(network)
            } else {
                setConnectivityOnline(false)
            }
        }
    }

    fun recordApiResponse() {
        synchronized(apiFailureLock) {
            apiFailureCount = 0
            apiFailureWindowStartedAt = 0L
        }

        val network = connectivityManager.activeNetwork ?: return
        if (!currentlyConnected(network)) {
            return
        }

        synchronized(stateLock) {
            apiUnavailable = false
            currentNetwork = network
            apiProbeJob?.cancel()
            apiProbeJob = null
        }
        setConnectivityOnline(true)
    }

    private fun onValidatedNetworkAvailable(network: Network) {
        val networkChanged = synchronized(stateLock) {
            val changed = currentNetwork != network
            currentNetwork = network
            if (changed) {
                apiUnavailable = false
                apiProbeJob?.cancel()
                apiProbeJob = null
            }
            changed
        }

        if (networkChanged) {
            synchronized(apiFailureLock) {
                apiFailureCount = 0
                apiFailureWindowStartedAt = 0L
            }
        }

        val isApiUnavailable = synchronized(stateLock) { apiUnavailable }
        if (isApiUnavailable) {
            startApiReachabilityChecks(network)
        } else {
            setConnectivityOnline(true)
        }
    }

    private fun startApiReachabilityChecks(network: Network) {
        synchronized(stateLock) {
            if (apiProbeJob?.isActive == true) return

            apiProbeJob = scope.launch {
                var backoffMillis = 1_000L
                while (isActive && connectivityManager.activeNetwork == network && currentlyConnected(network)) {
                    val reachable = withContext(Dispatchers.IO) { isApiReachable(network) }
                    if (!isActive) return@launch

                    if (reachable) {
                        synchronized(stateLock) {
                            apiUnavailable = false
                            apiProbeJob = null
                        }
                        synchronized(apiFailureLock) {
                            apiFailureCount = 0
                            apiFailureWindowStartedAt = 0L
                        }
                        setConnectivityOnline(true)
                        return@launch
                    }

                    synchronized(stateLock) { apiUnavailable = true }
                    setConnectivityOnline(false)
                    delay(backoffMillis)
                    backoffMillis = minOf(backoffMillis * 2, MAX_API_PROBE_BACKOFF_MS)
                }

                synchronized(stateLock) { apiProbeJob = null }
            }
        }
    }

    private fun isApiReachable(network: Network): Boolean {
        val connection = runCatching {
            network.openConnection(URL("${BuildConfig.BASE_API_URL.trimEnd('/')}/health"))
                as HttpURLConnection
        }.getOrNull() ?: return false

        return try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.requestMethod = "GET"
            connection.useCaches = false
            connection.responseCode > 0
        } catch (_: Exception) {
            false
        } finally {
            connection.disconnect()
        }
    }

    private fun currentlyConnected(network: Network): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return isValidated(capabilities)
    }

    private fun isValidated(capabilities: NetworkCapabilities?): Boolean =
        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    private fun setConnectivityOnline(online: Boolean) {
        synchronized(stateLock) {
            if (_isOnline.value == online) {
                if (!online) scheduleOfflineVisibility()
                return
            }

            _isOnline.value = online
            if (!online) {
                onlineVisibilityJob?.cancel()
                onlineVisibilityJob = null
                scheduleOfflineVisibility()
                Log.d(TAG, "Connectivity unavailable; confirming offline state")
            } else {
                offlineVisibilityJob?.cancel()
                offlineVisibilityJob = null
                onlineVisibilityJob?.cancel()
                onlineVisibilityJob = scope.launch {
                    delay(RECOVERY_CONFIRMATION_MS)
                    if (_isOnline.value) {
                        val wasOfflineVisible = _showOffline.value
                        _showOffline.value = false
                        if (wasOfflineVisible) {
                            Log.d(TAG, "Connectivity restored")
                            NetworkStatusBus.emitReconnected()
                        }
                    }
                }
            }
        }
    }

    private fun scheduleOfflineVisibility() {
        if (_showOffline.value || offlineVisibilityJob?.isActive == true) return

        offlineVisibilityJob = scope.launch {
            delay(OFFLINE_CONFIRMATION_MS)
            if (!_isOnline.value) {
                _showOffline.value = true
                Log.d(TAG, "Offline state confirmed")
            }
        }
    }
}

