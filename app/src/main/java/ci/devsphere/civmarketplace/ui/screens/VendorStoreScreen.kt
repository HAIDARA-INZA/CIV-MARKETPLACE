package ci.devsphere.civmarketplace.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ci.devsphere.civmarketplace.ui.components.ProductCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorStoreScreen(
    vendorId: Int,
    vendorName: String,
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.vendorStoreState
    LaunchedEffect(vendorId) {
        viewModel.loadVendorProducts(vendorId)
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(vendorName) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            }
        )
    }) { padding ->
        when (state) {
            VendorStoreState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            VendorStoreState.Empty -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Ce vendeur n'a pas encore de produits") }
            is VendorStoreState.Error -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text((state as VendorStoreState.Error).message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { viewModel.loadVendorProducts(vendorId, force = true) }) {
                        Text("Réessayer")
                    }
                }
            }
            is VendorStoreState.Success -> {
                val products = (state as VendorStoreState.Success).products
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onClick = { onNavigateToDetail(product.id.toString()) },
                            onFavoriteClick = { viewModel.setFavorite(product, it) }
                        )
                    }
                }
            }
        }
    }
}

