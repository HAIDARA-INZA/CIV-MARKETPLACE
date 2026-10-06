package ci.devsphere.civmarketplace.ui.theme

import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Le thème choisi dans l'application, distinct du thème du téléphone. */
val LocalAppDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    secondary = CyanSecondary,
    tertiary = BlueTertiary,
    background = BackgroundDark, // #000000
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onPrimary = AccentContent,
    onSecondary = AccentContent,
    onBackground = TextDarkPrimary,    // Blanc 100%
    onSurface = TextDarkPrimary,       // Blanc 100%
    onSurfaceVariant = TextDarkSecondary, // Blanc 72%
    outline = Color.White.copy(alpha = 0.24f),
    error = Color(0xFFFF453A)
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = CyanSecondary,
    tertiary = BlueTertiary,
    background = BackgroundLight, // #F2F2F7
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onPrimary = AccentContent,
    onSecondary = AccentContent,
    onBackground = TextLightPrimary,    // Noir 100%
    onSurface = TextLightPrimary,       // Noir 100%
    onSurfaceVariant = TextLightSecondary, // Noir 72%
    outline = Color.Black.copy(alpha = 0.12f),
    error = Color(0xFFFF3B30)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalAbsoluteTonalElevation provides 0.dp,
        LocalAppDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

