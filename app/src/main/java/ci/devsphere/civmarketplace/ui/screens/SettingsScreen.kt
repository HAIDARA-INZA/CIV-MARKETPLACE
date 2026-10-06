package ci.devsphere.civmarketplace.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ci.devsphere.civmarketplace.data.model.UserSettingsDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToLegal: (LegalType) -> Unit,
    viewModel: UserViewModel = hiltViewModel()
) {
    val state by viewModel.settingsState
    val context = LocalContext.current
    var showBackgroundHelp by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (val settingsState = state) {
                SettingsState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is SettingsState.Error -> {
                    Text(settingsState.message, color = MaterialTheme.colorScheme.error)
                }
                is SettingsState.Saving -> {
                    SettingsContent(settingsState.settings, saving = true, onChange = viewModel::updateSettings, onNavigateToLegal = onNavigateToLegal)
                }
                is SettingsState.Success -> {
                    SettingsContent(settingsState.settings, saving = false, onChange = viewModel::updateSettings, onNavigateToLegal = onNavigateToLegal)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
            ListItem(
                headlineContent = { Text("Notifications en arrière-plan") },
                supportingContent = { Text("Vérifier les réglages de batterie et de démarrage automatique") },
                modifier = Modifier.clickable { showBackgroundHelp = true }
            )
        }
    }

    if (showBackgroundHelp) {
        AlertDialog(
            onDismissRequest = { showBackgroundHelp = false },
            title = { Text("Recevoir les messages en arrière-plan") },
            text = {
                Text(
                    "Autorisez les notifications et l'activité en arrière-plan dans les réglages de l'application. " +
                        "Sur certains téléphones (Xiaomi, Tecno, Infinix ou Samsung), désactivez aussi " +
                        "l'optimisation batterie et autorisez le démarrage automatique. L'arrêt forcé Android " +
                        "bloque les notifications jusqu'à la prochaine ouverture manuelle de l'application."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBackgroundHelp = false
                    runCatching {
                        val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", "ci.devsphere.civmarketplace", null)
                        }
                        context.startActivity(settingsIntent)
                    }
                }) {
                    Text("Réglages de l'application")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundHelp = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}

@Composable
private fun SettingsContent(
    settings: UserSettingsDto,
    saving: Boolean,
    onChange: (UserSettingsDto) -> Unit,
    onNavigateToLegal: (LegalType) -> Unit
) {
    Text(text = "Préférences du compte", style = MaterialTheme.typography.titleMedium)

    ListItem(
        headlineContent = { Text("Notifications") },
        supportingContent = { Text("Recevoir les messages, commandes et nouveaux vendeurs") },
        trailingContent = {
            Switch(
                checked = settings.notificationsEnabled,
                enabled = !saving,
                onCheckedChange = { enabled ->
                    onChange(settings.copy(notificationsEnabled = enabled))
                }
            )
        }
    )
    HorizontalDivider()

    ListItem(
        headlineContent = { Text("Mode sombre") },
        supportingContent = { Text("Préférence enregistrée côté serveur") },
        trailingContent = {
            Switch(
                checked = settings.darkModeEnabled,
                enabled = !saving,
                onCheckedChange = { enabled ->
                    onChange(settings.copy(darkModeEnabled = enabled))
                }
            )
        }
    )
    HorizontalDivider()

    Text(
        text = "Langue",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )

    LanguageRow(
        label = "Français",
        selected = settings.language == "fr",
        enabled = !saving,
        onClick = { onChange(settings.copy(language = "fr")) }
    )
    // Le multilingue n'est pas encore implémenté (aucun texte de l'app n'est traduit) :
    // plutôt que de laisser un sélecteur qui "marche" sans rien changer à l'écran,
    // on l'affiche désactivé avec un badge, le temps de vraiment l'implémenter.
    LanguageRow(
        label = "English",
        selected = false,
        enabled = false,
        badge = "Bientôt",
        onClick = {}
    )

    Text(
        text = "Légal",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )

    ListItem(
        headlineContent = { Text("Conditions d'Utilisation") },
        modifier = Modifier.clickable { onNavigateToLegal(LegalType.TERMS) }
    )
    HorizontalDivider()

    ListItem(
        headlineContent = { Text("Politique de Confidentialité") },
        modifier = Modifier.clickable { onNavigateToLegal(LegalType.PRIVACY) }
    )
}

@Composable
private fun LanguageRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                if (badge != null) {
                    androidx.compose.material3.Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        trailingContent = {
            RadioButton(
                selected = selected,
                enabled = enabled,
                onClick = onClick
            )
        }
    )
}

