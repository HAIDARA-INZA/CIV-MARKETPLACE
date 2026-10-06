package ci.devsphere.civmarketplace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ci.devsphere.civmarketplace.ui.components.ProductCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val state by viewModel.state

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Mes favoris", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    windowInsets = WindowInsets.statusBars
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (val favoritesState = state) {
                    FavoritesState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    is FavoritesState.Error -> Text(favoritesState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                    is FavoritesState.Success -> {
                        if (favoritesState.products.isEmpty()) {
                            ci.devsphere.civmarketplace.ui.components.EmptyStateView(
                                icon = Icons.Default.FavoriteBorder,
                                title = "Aucun favori pour l'instant",
                                subtitle = "Tape sur le cœur d'un produit pour le retrouver ici.",
                                actionLabel = "Découvrir des produits",
                                onAction = onBack,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(favoritesState.products) { product ->
                                    ProductCard(
                                        product = product.copy(isFavorite = true),
                                        onClick = { onNavigateToDetail(product.id.toString()) },
                                        onFavoriteClick = { favorite -> viewModel.setFavorite(product, favorite) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

