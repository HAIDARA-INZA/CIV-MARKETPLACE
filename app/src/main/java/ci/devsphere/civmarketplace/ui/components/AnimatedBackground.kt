package ci.devsphere.civmarketplace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Fond volontairement immobile : aucun élément ne se déplace derrière le
 * contenu, afin de conserver la lisibilité des formulaires et des commandes.
 * Le nom est conservé pour ne pas casser les écrans existants.
 */
@Composable
fun AnimatedBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    )
}

