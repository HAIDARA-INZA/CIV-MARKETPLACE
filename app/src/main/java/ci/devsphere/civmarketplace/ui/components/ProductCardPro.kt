package ci.devsphere.civmarketplace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ci.devsphere.civmarketplace.data.model.ProductDto
import ci.devsphere.civmarketplace.ui.theme.AccentContent
import ci.devsphere.civmarketplace.ui.theme.BackgroundDark
import ci.devsphere.civmarketplace.ui.theme.LocalAppDarkTheme

@Composable
fun BlueLiquidGlass(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val isDarkTheme = LocalAppDarkTheme.current

    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                spotColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(
                if (isDarkTheme)
                    BackgroundDark.copy(alpha = 0.70f) 
                else 
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.95f), 
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.95f)
                    )
                ),
                shape = shape
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.07f),
                            Color.Transparent
                        )
                    )
                )
        )
        content()
    }
}

@Composable
fun ProductCardPro(
    product: ProductDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFavoriteClick: ((Boolean) -> Unit)? = null
) {
    var isFavorite by remember(product.id, product.isFavorite) { mutableStateOf(product.isFavorite) }
    val tag = product.category?.takeIf { it.isNotBlank() }

    BlueLiquidGlass(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 24.dp,
        borderWidth = 1.5.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(AccentContent.copy(alpha = 0.04f))
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f),
                            spotColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.32f)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = product.getDisplayImageUrl(),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                        error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }

                if (product.shouldShowStockStatus() && product.stock <= 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Rupture de stock",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = product.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ci.devsphere.civmarketplace.util.PriceFormatter.format(product.price),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (tag != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag.uppercase(),
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = {
                    val nextValue = !isFavorite
                    isFavorite = nextValue
                    onFavoriteClick?.invoke(nextValue)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favori",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

