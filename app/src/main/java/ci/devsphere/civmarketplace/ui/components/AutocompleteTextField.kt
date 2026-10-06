package ci.devsphere.civmarketplace.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Champ de saisie avec recherche intelligente (Ville / Commune / Quartier à l'inscription
 * et sur le profil). Texte libre toujours accepté — ce n'est pas bloquant si la ville/le
 * quartier de l'utilisateur n'est pas dans la liste — mais une liste de suggestions
 * s'affiche sous le champ dès qu'il y a des correspondances, et un tap sur une suggestion
 * la sélectionne directement.
 *
 * Affichée "en flux normal" sous le champ (pas en popup superposé) : plus simple et fiable
 * sur toutes les tailles d'écran qu'un ancrage de popup.
 *
 * @param suggestions liste déjà filtrée à afficher, calculée par l'appelant
 * (ex: LocationData.searchCities(value)).
 */
@Composable
fun AutocompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    // Repasse à true dès que l'utilisateur retape après avoir choisi une suggestion,
    // pour ne pas rouvrir la liste juste après une sélection.
    var justPicked by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        CIVTextField(
            value = value,
            onValueChange = {
                justPicked = false
                onValueChange(it)
            },
            label = label,
            leadingIcon = leadingIcon?.let { icon ->
                { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            modifier = Modifier.fillMaxWidth()
        )

        val showSuggestions = value.isNotBlank() && !justPicked && suggestions.isNotEmpty()
        if (showSuggestions) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .heightIn(max = 220.dp),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp,
                shadowElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                LazyColumn {
                    items(suggestions.take(30)) { suggestion ->
                        Text(
                            text = suggestion,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    justPicked = true
                                    onValueChange(suggestion)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

