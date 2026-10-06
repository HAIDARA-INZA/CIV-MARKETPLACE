package ci.devsphere.civmarketplace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import ci.devsphere.civmarketplace.data.local.CartEntity
import ci.devsphere.civmarketplace.ui.components.CIVButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onNavigateToCheckout: () -> Unit,
    viewModel: CartViewModel = hiltViewModel()
) {
    val cartItems by viewModel.cartItems.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = "Mon Panier", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    windowInsets = WindowInsets.statusBars
                )
            },
            bottomBar = {
                if (cartItems.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .navigationBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Total à payer", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = ci.devsphere.civmarketplace.util.PriceFormatter.format(viewModel.getTotalPrice(cartItems)),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            CIVButton(
                                text = "Passer à la caisse",
                                onClick = onNavigateToCheckout
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            if (cartItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    ci.devsphere.civmarketplace.ui.components.EmptyStateView(
                        icon = Icons.Default.ShoppingCartCheckout,
                        title = "Votre panier est vide",
                        subtitle = "Les articles que tu ajoutes apparaîtront ici."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cartItems) { item ->
                        CartItemRow(item = item, onDelete = { viewModel.removeFromCart(item) })
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(item: CartEntity, onDelete: () -> Unit) {
    var showDeleteConfirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            ,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = ci.devsphere.civmarketplace.util.PriceFormatter.format(item.price),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            IconButton(
                onClick = { showDeleteConfirm = true },
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer")
            }
        }
    }

    if (showDeleteConfirm) {
        ci.devsphere.civmarketplace.ui.components.ConfirmationDialog(
            title = "Retirer cet article ?",
            message = "« ${item.name} » sera retiré de votre panier.",
            confirmLabel = "Retirer",
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

