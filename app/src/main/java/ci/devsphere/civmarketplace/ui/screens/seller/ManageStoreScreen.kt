package ci.devsphere.civmarketplace.ui.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import android.content.Intent
import ci.devsphere.civmarketplace.data.model.ProductDto
import ci.devsphere.civmarketplace.di.ProductRefreshBus
import ci.devsphere.civmarketplace.ui.components.CIVTextField
import ci.devsphere.civmarketplace.util.SlugUtils

@Composable
fun ManageStoreScreen(
    onAddProduct: () -> Unit,
    viewModel: SellerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state
    val sellerId by viewModel.sellerId
    val sellerName by viewModel.sellerName
    var productToEdit by remember { mutableStateOf<ProductDto?>(null) }
    var productToDelete by remember { mutableStateOf<ProductDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadMyProducts()
        ProductRefreshBus.events.collect {
            viewModel.loadMyProducts()
        }
    }

    when (val current = state) {
        SellerState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is SellerState.Empty -> Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(current.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { viewModel.refresh(force = true) }) {
                    Text("Réessayer")
                }
            }
        }
        is SellerState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text(current.message, color = MaterialTheme.colorScheme.error) }
        is SellerState.Success -> LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Gérer ma boutique",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    sellerId?.let { id ->
                        IconButton(onClick = {
                            val shopSlug = SlugUtils.routeSlug(sellerName, id)
                            val shareUrl = "https://haidara.devsphere.ci/shop/$shopSlug"
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Ma boutique sur CIV Marketplace")
                                putExtra(Intent.EXTRA_TEXT, "Découvre ma boutique sur CIV Marketplace : $shareUrl")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Partager ma boutique"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Partager", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Text("Ajoutez et consultez tous les articles de votre boutique.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))

                Button(
                    onClick = onAddProduct,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ajouter un produit", fontWeight = FontWeight.Bold)
                }
            }
            if (current.myProducts.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Votre boutique ne contient aucun produit.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(current.myProducts, key = { it.id }) { product ->
                    StoreProductCard(product, onEdit = { productToEdit = product }, onDelete = { productToDelete = product })
                }
            }
        }
    }

    productToEdit?.let { product ->
        EditProductDialog(product, onDismiss = { productToEdit = null }) { name, description, price, stock, category ->
            viewModel.updateProduct(product, name, description, price, stock, category)
            productToEdit = null
        }
    }
    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Supprimer ce produit ?") },
            text = { Text("« ${product.name} » sera définitivement supprimé.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteProduct(product.id); productToDelete = null }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { productToDelete = null }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun StoreProductCard(product: ProductDto, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(product.getDisplayImageUrl(), null, Modifier.size(72.dp), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(ci.devsphere.civmarketplace.util.PriceFormatter.format(product.price), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text("Stock : ${product.stock}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Row {
                    TextButton(onClick = onEdit) { Text("Modifier", color = MaterialTheme.colorScheme.primary) }
                    TextButton(onClick = onDelete) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun EditProductDialog(product: ProductDto, onDismiss: () -> Unit, onSave: (String, String, String, String, String) -> Unit) {
    var name by remember(product.id) { mutableStateOf(product.name) }
    var description by remember(product.id) { mutableStateOf(product.description.orEmpty()) }
    var price by remember(product.id) { mutableStateOf(product.price) }
    var stock by remember(product.id) { mutableStateOf(product.stock.toString()) }
    var category by remember(product.id) { mutableStateOf(product.category.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le produit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CIVTextField(name, { name = it }, label = "Nom")
                CIVTextField(description, { description = it }, label = "Description", singleLine = false)
                CIVTextField(price, { price = it }, label = "Prix (FCFA)")
                CIVTextField(stock, { stock = it }, label = "Stock")
                CIVTextField(category, { category = it }, label = "Catégorie")
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, description, price, stock, category) }) { Text("Enregistrer", color = MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

