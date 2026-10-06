package ci.devsphere.civmarketplace.ui.screens.client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ci.devsphere.civmarketplace.ui.theme.glassCard
import ci.devsphere.civmarketplace.util.RoleUtils

@Composable
fun ClientProfileScreen(
    role: String,
    onNavigateToOrders: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val canSell = RoleUtils.canSell(role)
    val canBuy = RoleUtils.canBuy(role)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = when {
                    canSell && canBuy -> "Mon espace"
                    canSell -> "Espace vendeur"
                    else -> "Mon profil"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            ProfileMenuItem(
                title = "Modifier mon profil",
                icon = Icons.Default.Edit,
                onClick = onNavigateToEditProfile
            )

            ProfileMenuItem(
                title = "Notifications",
                icon = Icons.Default.Notifications,
                onClick = onNavigateToNotifications
            )

            if (canSell) {
                ProfileMenuItem(
                    title = "Mon dashboard vendeur",
                    icon = Icons.Default.Dashboard,
                    onClick = onNavigateToDashboard
                )
                ProfileMenuItem(
                    title = "Statistiques de vente",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    onClick = onNavigateToDashboard
                )
            }

            if (canBuy) {
                ProfileMenuItem(
                    title = "Mes commandes",
                    icon = Icons.AutoMirrored.Filled.List,
                    onClick = onNavigateToOrders
                )
                ProfileMenuItem(
                    title = "Mes favoris",
                    icon = Icons.Default.Favorite,
                    onClick = onNavigateToFavorites
                )
            }

            ProfileMenuItem(
                title = "Paramètres du compte",
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        var showLogoutConfirm by remember { mutableStateOf(false) }

        OutlinedButton(
            onClick = { showLogoutConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Se déconnecter")
        }

        if (showLogoutConfirm) {
            ci.devsphere.civmarketplace.ui.components.ConfirmationDialog(
                title = "Se déconnecter ?",
                message = "Tu devras te reconnecter avec ton e-mail et ton mot de passe pour revenir sur ton compte.",
                confirmLabel = "Se déconnecter",
                onConfirm = {
                    showLogoutConfirm = false
                    onLogout()
                },
                onDismiss = { showLogoutConfirm = false }
            )
        }
    }
}

@Composable
fun ProfileMenuItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .glassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick = onClick
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

