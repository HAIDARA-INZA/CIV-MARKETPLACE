package ci.devsphere.civmarketplace.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import ci.devsphere.civmarketplace.util.SlugUtils
import ci.devsphere.civmarketplace.ui.components.CIVButton
import ci.devsphere.civmarketplace.ui.components.AutoRefreshEffect
import ci.devsphere.civmarketplace.ui.components.ErrorStateView
import kotlinx.coroutines.launch

@Composable
fun ProductDetailScreen(
    productId: Int,
    onBack: () -> Unit,
    onNavigateToChat: (Int, String, Int?) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state
    val canBuy by viewModel.canBuy
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    AutoRefreshEffect(key = "product-detail-$productId") {
        viewModel.refreshProductIfStale(productId)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val productState = state) {
            is ProductDetailState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is ProductDetailState.Success -> {
                val product = productState.product
                var photoViewerOpen by remember(product.id) { mutableStateOf(false) }
                val productImageUrl = product.getDisplayImageUrl()
                val productShareLink = product.getActualSellerId()?.let { sellerId ->
                    val sellerSlug = SlugUtils.routeSlug(product.sellerName, sellerId)
                    val productSlug = SlugUtils.routeSlug(product.name, product.id)
                    "https://haidara.devsphere.ci/shop/$sellerSlug/$productSlug"
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Box {
                        AsyncImage(
                            model = productImageUrl,
                            contentDescription = product.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .clickable(enabled = !productImageUrl.isNullOrBlank()) {
                                    photoViewerOpen = true
                                },
                            contentScale = ContentScale.Crop,
                            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                            error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
                        )

                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(16.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), RoundedCornerShape(24.dp))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = {
                                val link = productShareLink
                                if (link == null) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Lien de partage indisponible")
                                    }
                                    return@IconButton
                                }

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, product.name)
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Découvre ce produit sur CIV Marketplace : $link"
                                    )
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Partager le produit")
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(top = 16.dp, end = 72.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), RoundedCornerShape(24.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Partager",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { viewModel.setFavorite(product, !product.isFavorite) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(16.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), RoundedCornerShape(24.dp))
                        ) {
                            Icon(
                                imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favori",
                                tint = if (product.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (photoViewerOpen && !productImageUrl.isNullOrBlank()) {
                        FullScreenPhotoViewer(
                            imageUrl = productImageUrl,
                            contentDescription = product.name,
                            onDismiss = { photoViewerOpen = false }
                        )
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = product.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = ci.devsphere.civmarketplace.util.PriceFormatter.format(product.price),
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        product.category?.takeIf { it.isNotBlank() }?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Catégorie : $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        product.sellerName?.takeIf { it.isNotBlank() }?.let { name ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Vendu par $name", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                if (product.sellerIsVerified) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ci.devsphere.civmarketplace.ui.components.VerifiedSellerBadge()
                                }
                            }
                        }
                        val sellerLocation = product.getSellerLocation()
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Localisation : ${sellerLocation ?: "Non renseignée"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        if (product.shouldShowStockStatus()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val stockText = when {
                                product.stock <= 0 -> "Rupture de stock"
                                product.stock < 10 -> "Stock faible : ${product.stock}"
                                else -> "En stock : ${product.stock}"
                            }
                            Text(
                                text = stockText,
                                color = if (product.stock <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Description",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = product.description ?: "Aucune description disponible",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        ProductReviewsSection(reviews = viewModel.reviews.value)

                        Spacer(modifier = Modifier.height(32.dp))

                        if (canBuy && product.stock > 0) {
                            CIVButton(
                                text = "Ajouter au panier",
                                onClick = {
                                    viewModel.addToCart(product)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Produit ajouté au panier")
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        val sellerPhone = product.getDialPhoneNumber()

                        if (!sellerPhone.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = android.net.Uri.parse("tel:$sellerPhone")
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Appeler le vendeur")
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedButton(
                            onClick = {
                                product.getActualSellerId()?.let { sellerId ->
                                    onNavigateToChat(sellerId, product.sellerName ?: "Vendeur #$sellerId", product.id)
                                } ?: coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Vendeur inconnu")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Contacter le vendeur")
                        }

                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
            }
            is ProductDetailState.Error -> {
                ErrorStateView(
                    error = productState.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) { data ->
            Snackbar(snackbarData = data)
        }
    }
}

/** Étape 2 lancement : vrais avis clients, sous la description du produit. */
@Composable
private fun FullScreenPhotoViewer(
    imageUrl: String,
    contentDescription: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = nextScale
        offset = if (nextScale <= 1.01f) {
            Offset.Zero
        } else {
            offset + panChange
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .transformable(transformableState),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fermer",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ProductReviewsSection(reviews: List<ci.devsphere.civmarketplace.data.model.ReviewDto>) {
    Text(
        text = "Avis clients (${reviews.size})",
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))

    if (reviews.isEmpty()) {
        Text(
            text = "Aucun avis pour l'instant — sois le premier à acheter et à donner ton avis !",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column {
        reviews.forEach { review ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "★".repeat(review.rating) + "☆".repeat(5 - review.rating),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = review.clientName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (!review.comment.isNullOrBlank()) {
                    Text(
                        text = review.comment,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }
    }
}

