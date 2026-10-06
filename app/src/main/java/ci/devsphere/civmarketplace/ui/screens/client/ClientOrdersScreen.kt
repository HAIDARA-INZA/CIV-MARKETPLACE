package ci.devsphere.civmarketplace.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ci.devsphere.civmarketplace.data.model.OrderDto
import ci.devsphere.civmarketplace.ui.components.AutoRefreshEffect
import ci.devsphere.civmarketplace.ui.components.AnimatedBackground
import ci.devsphere.civmarketplace.ui.theme.glassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientOrdersScreen(
    onBack: () -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val state by viewModel.state
    val reviewableOrderIds by viewModel.reviewableOrderIds
    val reviewSubmitState by viewModel.reviewSubmitState
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var reviewDialogOrderId by remember { mutableStateOf<Int?>(null) }

    AutoRefreshEffect(key = "client-orders", refreshIfStale = viewModel::refreshIfStale)

    // Ferme la boîte de dialogue automatiquement une fois l'avis bien envoyé.
    androidx.compose.runtime.LaunchedEffect(reviewSubmitState) {
        if (reviewSubmitState is ReviewSubmitState.Success) {
            reviewDialogOrderId = null
            viewModel.resetReviewSubmitState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedBackground()

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Mes commandes", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    windowInsets = WindowInsets.statusBars
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh(force = true) },
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (val ordersState = state) {
                        OrdersState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        is OrdersState.Empty -> {
                            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(ordersState.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(onClick = { viewModel.refresh(force = true) }) {
                                    Text("Réessayer")
                                }
                            }
                        }
                        is OrdersState.Error -> Text(
                            ordersState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        is OrdersState.Success -> {
                            if (ordersState.orders.isEmpty()) {
                                Text("Aucune commande", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(ordersState.orders) { order ->
                                        OrderItem(
                                            order = order,
                                            canReview = reviewableOrderIds.contains(order.id),
                                            onLeaveReview = { reviewDialogOrderId = order.id }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        reviewDialogOrderId?.let { orderId ->
            LeaveReviewDialog(
                isSubmitting = reviewSubmitState is ReviewSubmitState.Submitting,
                errorMessage = (reviewSubmitState as? ReviewSubmitState.Error)?.message,
                onDismiss = {
                    reviewDialogOrderId = null
                    viewModel.resetReviewSubmitState()
                },
                onSubmit = { rating, comment -> viewModel.submitReview(orderId, rating, comment) }
            )
        }
    }
}

@Composable
private fun LeaveReviewDialog(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String?) -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Laisser un avis") },
        text = {
            Column {
                Text("Comment s'est passée cette commande ?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$star étoile(s)",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { rating = star }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Ton commentaire (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(rating, comment.trim().ifBlank { null }) },
                enabled = !isSubmitting
            ) {
                Text(if (isSubmitting) "Envoi..." else "Envoyer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Annuler") }
        }
    )
}

@Composable
fun OrderItem(order: OrderDto, canReview: Boolean = false, onLeaveReview: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Commande #${order.id}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(text = order.price, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            }
            Text(text = order.productName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Quantité
            order.quantity?.let {
                Text(
                    text = "Quantité : $it",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Statut commande
            Text(
                text = readableOrderStatus(order.status),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp)
            )

            // Statut paiement
            order.paymentStatus?.let { ps ->
                val (label, color) = when (ps.lowercase()) {
                    "paid"    -> "Paiement confirmé ✅" to MaterialTheme.colorScheme.secondary
                    "failed"  -> "Paiement échoué ❌" to MaterialTheme.colorScheme.error
                    else      -> "Paiement en attente ⏳" to MaterialTheme.colorScheme.primary
                }
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            OrderStatusTimeline(status = order.status)

            // Étape 2 lancement : proposer de laisser un avis uniquement si la commande
            // est vraiment livrée ET pas déjà notée (liste calculée côté backend).
            if (canReview) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onLeaveReview, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.StarBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Laisser un avis")
                }
            }
        }
    }
}

@Composable
fun OrderStatusTimeline(status: String) {
    val steps = listOf("Acceptée", "Expédiée", "Livrée")
    val currentIndex = when (status.uppercase()) {
        "EXPEDIE", "SHIPPED" -> 1
        "LIVRE", "DELIVERED" -> 2
        else -> 0
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { index, step ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            if (index <= currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index <= currentIndex) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Text(
                    text = step, 
                    fontSize = 10.sp, 
                    fontWeight = if (index <= currentIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (index <= currentIndex) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (index < steps.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.weight(0.5f).padding(bottom = 12.dp),
                    thickness = 2.dp,
                    color = if (index < currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }
}

private fun readableOrderStatus(status: String): String {
    return when (status.uppercase()) {
        "EN_ATTENTE", "PENDING" -> "Paiement ou validation en attente"
        "ACCEPTE", "ACCEPTED" -> "Commande acceptée"
        "EXPEDIE", "SHIPPED" -> "Commande expédiée"
        "LIVRE", "DELIVERED" -> "Commande livrée"
        else -> status
    }
}

