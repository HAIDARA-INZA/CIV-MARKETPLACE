package ci.devsphere.civmarketplace.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ci.devsphere.civmarketplace.BuildConfig
import ci.devsphere.civmarketplace.data.model.AppVersionDto
import ci.devsphere.civmarketplace.util.AppUpdateManager
import ci.devsphere.civmarketplace.util.AppUpdateState
import ci.devsphere.civmarketplace.util.AppVersionPolicy

@Composable
fun AppUpdateDialogHost(viewModel: AppUpdateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    when (val updateState = state) {
        AppUpdateState.Hidden -> Unit
        is AppUpdateState.Available -> {
            val required = updateState.required
            AlertDialog(
                onDismissRequest = {
                    if (!required) viewModel.dismiss(updateState.version, required = false)
                },
                title = { Text("Mise à jour disponible") },
                text = {
                    UpdateDescription(
                        version = updateState.version,
                        required = required
                    )
                },
                confirmButton = {
                    Button(onClick = { viewModel.download(updateState.version) }) {
                        Text("Télécharger et installer")
                    }
                },
                dismissButton = {
                    if (required) {
                        TextButton(onClick = { openLanding(context) }) {
                            Text("Télécharger sur le site")
                        }
                    } else {
                        TextButton(onClick = { viewModel.dismiss(updateState.version, required = false) }) {
                            Text("Plus tard")
                        }
                    }
                },
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = !required,
                    dismissOnClickOutside = !required
                )
            )
        }
        is AppUpdateState.Downloading -> {
            val required = isRequired(updateState.version)
            AlertDialog(
                onDismissRequest = { if (!required) viewModel.dismiss(updateState.version, false) },
                title = { Text("Téléchargement de la mise à jour") },
                text = {
                    Column {
                        Text("Version ${updateState.version.versionName}")
                        LinearProgressIndicator(
                            progress = { (updateState.progress ?: 0) / 100f },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        )
                        Text(
                            text = updateState.progress?.let { "$it %" } ?: "Téléchargement en cours…",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    if (!required) {
                        TextButton(onClick = { viewModel.dismiss(updateState.version, false) }) {
                            Text("Masquer")
                        }
                    }
                },
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = !required,
                    dismissOnClickOutside = !required
                )
            )
        }
        is AppUpdateState.DownloadReady -> {
            val required = isRequired(updateState.version)
            AlertDialog(
                onDismissRequest = { if (!required) viewModel.dismiss(updateState.version, false) },
                title = { Text("Téléchargement terminé") },
                text = { Text("La version ${updateState.version.versionName} est prête à être installée.") },
                confirmButton = {
                    Button(onClick = { (context as? Activity)?.let(viewModel::install) }) {
                        Text("Installer")
                    }
                },
                dismissButton = {
                    if (required) {
                        TextButton(onClick = { openLanding(context) }) { Text("Page de téléchargement") }
                    } else {
                        TextButton(onClick = { viewModel.dismiss(updateState.version, false) }) {
                            Text("Plus tard")
                        }
                    }
                },
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = !required,
                    dismissOnClickOutside = !required
                )
            )
        }
        is AppUpdateState.DownloadError -> {
            val required = isRequired(updateState.version)
            AlertDialog(
                onDismissRequest = { if (!required) viewModel.dismiss(updateState.version, false) },
                title = { Text("Mise à jour impossible") },
                text = { Text(updateState.message) },
                confirmButton = {
                    Button(onClick = { viewModel.retry(updateState.version) }) {
                        Text("Réessayer")
                    }
                },
                dismissButton = {
                    if (required) {
                        TextButton(onClick = { openLanding(context) }) { Text("Ouvrir le site") }
                    } else {
                        TextButton(onClick = { viewModel.dismiss(updateState.version, false) }) {
                            Text("Plus tard")
                        }
                    }
                },
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = !required,
                    dismissOnClickOutside = !required
                )
            )
        }
    }
}

@Composable
private fun UpdateDescription(version: AppVersionDto, required: Boolean) {
    Column {
        Text("Version ${version.versionName}", style = MaterialTheme.typography.titleMedium)
        version.changelog?.takeIf(String::isNotBlank)?.let {
            Text(it, modifier = Modifier.padding(top = 10.dp))
        }
        if (required) {
            Text(
                "Cette mise à jour est nécessaire pour continuer.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

private fun isRequired(version: AppVersionDto): Boolean =
    AppVersionPolicy.isRequired(BuildConfig.VERSION_CODE, version)

private fun openLanding(context: android.content.Context) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppUpdateManager.LANDING_URL)))
}
