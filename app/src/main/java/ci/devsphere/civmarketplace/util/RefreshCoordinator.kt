package ci.devsphere.civmarketplace.util

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshCoordinator @Inject constructor() {
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val lastSuccessAt = ConcurrentHashMap<String, Long>()
    private val lastStartedAt = ConcurrentHashMap<String, Long>()
    private val _activeKeys = MutableStateFlow<Set<String>>(emptySet())
    val activeKeys: StateFlow<Set<String>> = _activeKeys.asStateFlow()

    suspend fun refresh(
        key: String,
        ttlMillis: Long,
        force: Boolean = false,
        debounceMillis: Long = DEFAULT_DEBOUNCE_MS,
        operation: suspend () -> Boolean
    ): Boolean {
        val mutex = locks.computeIfAbsent(key) { Mutex() }
        val now = SystemClock.elapsedRealtime()
        Log.d("Refresh", "refresh start key=$key force=$force ttl=${ttlMillis}ms debounce=${debounceMillis}ms now=$now")

        if (!mutex.tryLock()) {
            Log.d("Refresh", "refresh skipped key=$key reason=lock-held")
            return false
        }

        try {
            val lastStarted = lastStartedAt[key] ?: 0L
            if (now - lastStarted < debounceMillis) {
                Log.d("Refresh", "refresh skipped key=$key reason=debounce lastStarted=$lastStarted now=$now")
                return false
            }

            val lastSuccess = lastSuccessAt[key] ?: 0L
            if (!force && now - lastSuccess < ttlMillis) {
                Log.d("Refresh", "refresh skipped key=$key reason=ttl lastSuccess=$lastSuccess now=$now ttl=$ttlMillis")
                return false
            }

            lastStartedAt[key] = now
            _activeKeys.value = _activeKeys.value + key
            Log.d("Refresh", "refresh running key=$key")

            val succeeded = try {
                withTimeout(45_000L) {
                    operation()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                Log.e("Refresh", "refresh failed key=$key", t)
                false
            }

            if (succeeded) {
                lastSuccessAt[key] = SystemClock.elapsedRealtime()
                Log.d("Refresh", "refresh success key=$key")
            } else {
                Log.d("Refresh", "refresh failed key=$key result=false")
            }
            return succeeded
        } finally {
            _activeKeys.value = _activeKeys.value - key
            mutex.unlock()
            Log.d("Refresh", "refresh end key=$key")
        }
    }

    fun invalidate(key: String) {
        lastSuccessAt.remove(key)
    }

    private companion object {
        const val DEFAULT_DEBOUNCE_MS = 1_000L
    }
}
