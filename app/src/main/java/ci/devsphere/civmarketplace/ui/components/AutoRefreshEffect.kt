package ci.devsphere.civmarketplace.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun AutoRefreshEffect(
    key: String,
    intervalMillis: Long = 60_000L,
    refreshIfStale: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentRefresh = rememberUpdatedState(refreshIfStale)

    LaunchedEffect(lifecycleOwner, key, intervalMillis) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            currentRefresh.value()
            while (isActive) {
                delay(intervalMillis)
                currentRefresh.value()
            }
        }
    }
}
