package ci.devsphere.civmarketplace.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Boîte de dialogue de confirmation pour toute action sensible/irréversible
 * (suppression, déconnexion...). Une seule source de vérité pour garder un
 * ton et un style cohérents partout dans l'app plutôt que de dupliquer des
 * AlertDialog un peu différents à chaque endroit.
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirmer",
    dismissLabel: String = "Annuler",
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        }
    )
}

