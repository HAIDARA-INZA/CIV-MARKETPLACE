package ci.devsphere.civmarketplace.ui.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ci.devsphere.civmarketplace.ui.screens.seller.components.SalesChart
import ci.devsphere.civmarketplace.ui.theme.*
import ci.devsphere.civmarketplace.ui.components.AutoRefreshEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDashboardScreen(
    onManageStore: () -> Unit,
    viewModel: SellerViewModel = hiltViewModel()
) {
    val state by viewModel.state
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    AutoRefreshEffect(key = "seller-dashboard", refreshIfStale = viewModel::refreshIfStale)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val sellerState = state) {
            is SellerState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is SellerState.Empty -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = sellerState.message,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { viewModel.refresh(force = true) }) {
                        Text("Réessayer")
                    }
                }
            }
            is SellerState.Success -> {
                val stats = sellerState.stats
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh(force = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                    item {
                        Text(
                            text = "Tableau de bord",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Suivez simplement l'activité de votre boutique.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Wallet Card avec style Glassmorphism amélioré
                    item {
                        StatCard(
                            title = "Chiffre d'affaires",
                            value = ci.devsphere.civmarketplace.util.PriceFormatter.format(stats.totalSales),
                            icon = Icons.Default.AccountBalanceWallet,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Chart Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Évolution des ventes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(16.dp))
                                SalesChart(data = stats.salesTrend, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatCard(
                                title = "Vues",
                                value = stats.viewsCount.toString(),
                                icon = Icons.Default.Visibility,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            StatCard(
                                title = "Ventes",
                                value = stats.recentOrders.size.toString(),
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    
                    item {
                        Text(
                            text = "Mes produits",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (stats.recentOrders.isEmpty() && sellerState.myProducts.isEmpty()) {
                        item {
                            Text(
                                text = "Aucun article pour le moment",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(sellerState.myProducts) { product ->
                            MyProductRow(product, onClick = onManageStore)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Dernières commandes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    items(stats.recentOrders) { order ->
                        RecentOrderRow("Commande #${order.id} - ${order.productName}")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    }
                    }
                }
            }
            is SellerState.Error -> {
                Text(sellerState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun MyProductRow(
    product: ci.devsphere.civmarketplace.data.model.ProductDto,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            coil.compose.AsyncImage(
                model = product.getDisplayImageUrl(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = product.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = ci.devsphere.civmarketplace.util.PriceFormatter.format(product.price), fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        }
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "En vente", 
                fontSize = 10.sp, 
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RecentOrderRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 14.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "Payé", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

