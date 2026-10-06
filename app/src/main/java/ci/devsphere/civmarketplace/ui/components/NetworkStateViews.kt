package ci.devsphere.civmarketplace.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ci.devsphere.civmarketplace.util.AppError

/**
 * Bannière "hors ligne" globale (Bug 2).
 * À placer une seule fois, en haut de [ci.devsphere.civmarketplace.ui.navigation.AppNavigation]
 * (donc visible au-dessus de n'importe quel écran), pilotée par
 * [ci.devsphere.civmarketplace.util.NetworkMonitor.isOnline].
 *
 * Propre et discrète : pas d'erreur brute, juste un état clair pour l'utilisateur.
 */
@Composable
fun OfflineBanner(isOffline: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = isOffline,
        enter = slideInVertically { -it / 2 } + fadeIn(),
        exit = slideOutVertically { -it / 2 } + fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .statusBarsPadding()
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Color(0xFFE3A33C),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Connexion en cours…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
                TextButton(onClick = onRetry) {
                    Text("Réessayer", color = Color(0xFF9A6415), fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Vue d'état d'erreur centralisée (Bug 3) : icône + message cohérents avec le design,
 * à utiliser à la place des messages bruts rouges partout dans l'app
 * (écran vide, échec de chargement, etc.). L'icône est résolue via
 * [ci.devsphere.civmarketplace.util.ApiErrorMapper.iconFor] — une seule source de vérité.
 */
@Composable
fun ErrorStateView(
    error: AppError,
    modifier: Modifier = Modifier,
    action: @Composable (RowScope.() -> Unit)? = null
) {
    val icon = ci.devsphere.civmarketplace.util.ApiErrorMapper.iconFor(error.type)
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = error.message,
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        if (action != null) {
            Row(
                modifier = Modifier.padding(top = 16.dp),
                content = action
            )
        }
    }
}

