package ci.devsphere.civmarketplace.ui.screens

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CheckoutScreen(
    onPaymentFinished: () -> Unit,
    onRetry: () -> Unit = {},
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state

    if (state is CheckoutState.PaymentPendingConfirmation) {
        PaymentConfirmationPendingScreen(onBack = onPaymentFinished)
        return
    }

    // Démarrer le checkout automatiquement à l'ouverture
    LaunchedEffect(Unit) {
        viewModel.startCheckout()
    }

    // Quand l'URL Wave est prête → ouvrir Chrome Custom Tab
    LaunchedEffect(state) {
        if (state is CheckoutState.WaitingForPayment) {
            val url = (state as CheckoutState.WaitingForPayment).url
            try {
                val intent = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                intent.launchUrl(context, Uri.parse(url))
            } catch (e: Exception) {
                // Fallback si Chrome Custom Tabs n'est pas disponible
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "checkout_state"
        ) { currentState ->
            when (currentState) {

                // ─── Chargement de la session ─────────────────────────────
                is CheckoutState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Préparation du paiement...",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // ─── URL Wave ouverte — attente du retour ─────────────────
                is CheckoutState.WaitingForPayment -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "En attente de Wave...",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Complétez le paiement dans le navigateur.\nL'app sera mise à jour automatiquement.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedButton(onClick = onPaymentFinished) {
                            Text("Revenir à l'accueil")
                        }
                    }
                }

                // ─── ✅ Paiement réussi ─────────────────────────────────
                is CheckoutState.PaymentPendingConfirmation -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(96.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Paiement réussi ! 🎉",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Votre commande a bien été enregistrée.\nVous pouvez suivre son état dans \"Mes commandes\".",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                        Button(
                            onClick = onPaymentFinished,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Revenir au panier", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ─── ❌ Paiement échoué ───────────────────────────────────
                is CheckoutState.PaymentFailed -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(96.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Paiement échoué",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentState.reason,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                        Button(
                            onClick = {
                                viewModel.resetToIdle()
                                onRetry()
                            }
                        ) {
                            Text("Réessayer", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onPaymentFinished) {
                            Text("Revenir au panier")
                        }
                    }
                }

                // ─── Annulé ───────────────────────────────────────────────
                is CheckoutState.PaymentCancelled -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Paiement annulé",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Vous avez annulé le paiement.\nVotre panier a déjà été vidé — contactez le support si besoin.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                        OutlinedButton(onClick = onPaymentFinished) {
                            Text("Retour")
                        }
                    }
                }

                // ─── Erreur technique ─────────────────────────────────────
                is CheckoutState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = currentState.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedButton(onClick = onPaymentFinished) {
                            Text("Retour au panier")
                        }
                    }
                }

                else -> {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun PaymentConfirmationPendingScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text("Paiement en cours de confirmation", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Le paiement doit être confirmé par le serveur. Votre panier est conservé jusque-là.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            OutlinedButton(onClick = onBack) { Text("Revenir au panier") }
        }
    }
}

