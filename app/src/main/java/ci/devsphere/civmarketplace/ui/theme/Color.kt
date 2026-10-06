package ci.devsphere.civmarketplace.ui.theme

import androidx.compose.ui.graphics.Color

// --- SYSTÈME DE COULEURS "LANDING PAGE" ---

// Fours de base (Pure Black & Pure Light Gray)
val BackgroundDark = Color(0xFF000000)
val BackgroundLight = Color(0xFFF2F2F7)

// Surfaces
val SurfaceDark = Color(0xFF0A0A0A)
val SurfaceVariantDark = Color(0xFF121212)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE8E8ED)

// Couleurs Accent (iOS 26 Style)
val BluePrimary = Color(0xFF007AFF)   // Bleu iOS standard
val CyanSecondary = Color(0xFF00D4FF) // Cyan électrique
val BlueTertiary = Color(0xFF5856D6)  // Indigo/Purple léger

// Textes Mode Sombre (Opacités sur Blanc Pur)
val TextDarkPrimary = Color.White               // High Emphasis (100%)
val TextDarkSecondary = Color.White.copy(alpha = 0.72f) // Medium Emphasis
val TextDarkTertiary = Color.White.copy(alpha = 0.44f)  // Low Emphasis

// Textes Mode Clair (Opacités sur Noir Pur)
val TextLightPrimary = Color.Black               // High Emphasis (100%)
val TextLightSecondary = Color.Black.copy(alpha = 0.72f) // Medium Emphasis
val TextLightTertiary = Color.Black.copy(alpha = 0.44f)  // Low Emphasis

// Couleurs sémantiques réutilisables. Les écrans ne définissent pas leur propre palette.
val AccentContent = Color.White
val OnlineStatus = Color(0xFF34C759)
val OfflineStatus = Color(0xFFB3261E)

