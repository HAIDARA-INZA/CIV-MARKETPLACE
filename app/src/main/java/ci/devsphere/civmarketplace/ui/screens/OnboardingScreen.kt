package ci.devsphere.civmarketplace.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import ci.devsphere.civmarketplace.R
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ci.devsphere.civmarketplace.ui.components.BlueLiquidGlass
import ci.devsphere.civmarketplace.ui.theme.*
import kotlinx.coroutines.launch

@Composable
private fun SimpleOnboardingBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.completeOnboarding(onFinish)
    }
    val finishOnboarding = {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else viewModel.completeOnboarding(onFinish)
    }
    val pages = listOf(
        OnboardingPage(
            title = "Bienvenue sur CIV Marketplace",
            description = "La marketplace qui rapproche clients et vendeurs locaux dans toute la Côte d'Ivoire.",
            icon = Icons.Default.ShoppingBag,
            points = listOf(
                "Expérience client & vendeur",
                "Interface liquide iOS 26",
                "Filtrage local et catégories"
            )
        ),
        OnboardingPage(
            title = "Trois rôles, une seule app",
            description = "Client, vendeur ou client-vendeur : CIV adapte l'interface à votre usage.",
            icon = Icons.Default.Storefront,
            points = listOf(
                "Compte client simplifié",
                "Espace vendeur complet",
                "Mode mixte disponible"
            )
        ),
        OnboardingPage(
            title = "Temps réel instantané",
            description = "Pusher et Firebase gardent vos messages, commandes et notifications à jour en continu.",
            icon = Icons.Default.Chat,
            points = listOf(
                "Chats synchronisés",
                "Notifications immédiates",
                "Statuts de commandes en direct"
            )
        ),
        OnboardingPage(
            title = "Paiement mobile sécurisé",
            description = "Wave et PayDunya sont intégrés pour un checkout fluide et fiable.",
            icon = Icons.Default.Security,
            points = listOf(
                "Wave et PayDunya",
                "Redirection sécurisée",
                "Suivi de paiement automatique"
            )
        ),
        OnboardingPage(
            title = "Pensée pour la Côte d'Ivoire",
            description = "Une marketplace locale qui met en valeur produits, vendeurs et promotions proches.",
            icon = Icons.Default.LocationOn,
            points = listOf(
                "Vendeurs proches de vous",
                "Produits locaux mis en avant",
                "Recherche par quartier et catégorie"
            )
        )
    )

    var currentPage by rememberSaveable { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        SimpleOnboardingBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            OnboardingHeader(onSkip = finishOnboarding)

            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
            ) {
                OnboardingPageContent(
                    page = pages[currentPage],
                    pageIndex = currentPage,
                    pageCount = pages.size
                )
            }

            OnboardingFooter(
                pageCount = pages.size,
                currentPage = currentPage,
                onNext = {
                    currentPage = (currentPage + 1).coerceAtMost(pages.size - 1)
                },
                onFinish = finishOnboarding
            )
        }
    }
}

@Composable
private fun OnboardingHeader(onSkip: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.inza),
                contentDescription = "Logo",
                modifier = Modifier.size(28.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
            Text(
                text = "CIV Marketplace",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            Text(
                text = "Votre marketplace locale.",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        TextButton(onClick = onSkip) {
            Text(
                text = "Ignorer",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, pageIndex: Int, pageCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = page.title,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = page.description,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            page.points.forEach { point ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = point,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingFooter(
    pageCount: Int,
    currentPage: Int,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(if (index == currentPage) 22.dp else 8.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(if (index == currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = if (currentPage == pageCount - 1) onFinish else onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(if (currentPage == pageCount - 1) "Commencer" else "Suivant", fontWeight = FontWeight.Bold)
        }
    }
}

private data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val points: List<String>
)

