package ci.devsphere.civmarketplace.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ci.devsphere.civmarketplace.ui.components.CIVTextField
import ci.devsphere.civmarketplace.ui.components.ErrorStateView
import ci.devsphere.civmarketplace.ui.components.AutoRefreshEffect
import ci.devsphere.civmarketplace.ui.components.ProductCard
import ci.devsphere.civmarketplace.ui.components.ShimmerLoadingCard
import ci.devsphere.civmarketplace.ui.theme.AccentContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToChat: (Int, String, Int?) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    val homeState by viewModel.homeState
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val profileState by userViewModel.profileState
    val categoriesState by viewModel.categoriesState
    var searchQuery by remember { mutableStateOf("") }
    
    val userName = when (val state = profileState) {
        is ProfileState.Success -> state.profile.name
        is ProfileState.Updated -> state.profile.name
        else -> ""
    }

    AutoRefreshEffect(key = "home-catalog", refreshIfStale = viewModel::refreshIfStale)

    var selectedCategory by remember { mutableStateOf("Tout") }
    val categories = listOf("Tout") + ((categoriesState as? HomeCategoriesState.Success)?.categories?.map { it.name } ?: emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Bonjour 👋",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = userName.ifBlank { "Chargement..." },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onNavigateToCart) {
                                BadgedBox(badge = {
                                    if (cartItems.isNotEmpty()) Badge { Text(cartItems.sumOf { it.quantity }.toString()) }
                                }) {
                                    Icon(Icons.Default.ShoppingCart, "Panier", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        windowInsets = WindowInsets.statusBars
                    )
                    
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        CIVTextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it
                                viewModel.loadProducts(it)
                            },
                            label = "Rechercher un vendeur, un produit…",
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) }
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh(force = true) },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = padding.calculateTopPadding(),
                        bottom = 100.dp // Espace pour la barre de navigation flottante
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                item(span = { GridItemSpan(2) }) {
                    PromoBanner()
                }

                item(span = { GridItemSpan(2) }) {
                    CategorySelector(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    Text(
                        "Articles populaires",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                when (val state = homeState) {
                    is HomeState.Loading -> {
                        items(6) {
                            ShimmerLoadingCard()
                        }
                    }
                    is HomeState.Success -> {
                        items(state.products) { product ->
                            ProductCard(
                                product = product,
                                onClick = { onNavigateToDetail(product.id.toString()) },
                                onFavoriteClick = { /* Géré via ViewModel */ }
                            )
                        }
                    }
                    is HomeState.Error -> {
                        item(span = { GridItemSpan(2) }) {
                            ErrorStateView(error = state.error)
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
fun PromoBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Box {
            val colors = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
            Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(colors)))
            Column(
                modifier = Modifier.padding(24.dp).align(Alignment.CenterStart)
            ) {
                Text("PROMO DE LA SEMAINE", color = AccentContent.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("-50% sur l'électronique", color = AccentContent, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("En profiter", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
                        items(categories) { category ->
            val isSelected = selectedCategory == category
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = AccentContent,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onBackground
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

