package ci.devsphere.civmarketplace.ui.screens.seller.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SalesChart(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Pas de vraie tendance à montrer (aucune vente, ou une seule valeur à 0) :
    // mieux vaut un message clair qu'une ligne plate qui ressemble à un bug.
    val hasRealTrend = data.size >= 2 && data.any { it > 0f }

    if (!hasRealTrend) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.height(28.dp)
                )
                Text(
                    text = "Pas encore de ventes à afficher",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(8.dp)
    ) {
        // Capturée ici (hors du Canvas, qui n'a pas de contexte @Composable) pour que
        // la ligne de base reste visible aussi bien en mode clair qu'en mode sombre.
        val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val padding = 10.dp.toPx()
            val chartWidth = (width - (padding * 2)).coerceAtLeast(1f)
            val chartHeight = (height - (padding * 2)).coerceAtLeast(1f)
            val safeData = if (data.isEmpty()) listOf(0f) else data.map { it.coerceAtLeast(0f) }
            val minData = safeData.minOrNull() ?: 0f
            val maxData = safeData.maxOrNull() ?: 0f
            val range = (maxData - minData).takeIf { it > 0f } ?: 1f
            val spacing = if (safeData.size > 1) chartWidth / (safeData.size - 1) else 0f

            drawLine(
                color = axisColor,
                start = androidx.compose.ui.geometry.Offset(padding, height - padding),
                end = androidx.compose.ui.geometry.Offset(width - padding, height - padding),
                strokeWidth = 1.dp.toPx()
            )

            if (data.isEmpty()) return@Canvas
            
            val path = Path().apply {
                safeData.forEachIndexed { index, value ->
                    val x = padding + (index * spacing)
                    val normalized = if (maxData == minData) 0.5f else (value - minData) / range
                    val y = padding + (chartHeight * (1f - normalized))
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 3.dp.toPx())
            )
            
            if (safeData.size == 1) {
                drawCircle(color = color, radius = 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(padding, padding + chartHeight / 2))
            }
        }
    }
}

