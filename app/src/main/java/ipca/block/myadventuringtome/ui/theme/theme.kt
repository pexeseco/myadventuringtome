package ipca.block.myadventuringtome.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paper-inspired color palette
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF5D4037), // Dark brown (ink/leather)
    onPrimary = Color(0xFFFFF8E1), // Cream paper
    secondary = Color(0xFF8D6E63), // Medium brown
    onSecondary = Color(0xFFFFF8E1),
    tertiary = Color(0xFFD7CCC8), // Light brown/beige
    onTertiary = Color(0xFF3E2723),
    background = Color(0xFFFFF8E1), // Cream paper background
    onBackground = Color(0xFF3E2723), // Dark brown text
    surface = Color(0xFFFAF0E6), // Linen/paper surface
    onSurface = Color(0xFF3E2723),
    surfaceVariant = Color(0xFFF5F5DC), // Beige variant
    onSurfaceVariant = Color(0xFF5D4037),
    outline = Color(0xFFBCAAA4), // Light brown outline
    outlineVariant = Color(0xFFD7CCC8),
    error = Color(0xFFB71C1C), // Deep red (like ink stain)
    onError = Color(0xFFFFF8E1),
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFFB71C1C),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBCAAA4), // Light brown for dark mode
    onPrimary = Color(0xFF3E2723),
    secondary = Color(0xFF8D6E63),
    onSecondary = Color(0xFF3E2723),
    tertiary = Color(0xFFA1887F),
    onTertiary = Color(0xFF3E2723),
    background = Color(0xFF2C1810), // Dark parchment
    onBackground = Color(0xFFEFEBE9),
    surface = Color(0xFF3E2723), // Dark wood/leather
    onSurface = Color(0xFFEFEBE9),
    surfaceVariant = Color(0xFF4E342E),
    onSurfaceVariant = Color(0xFFD7CCC8),
    outline = Color(0xFF795548),
    outlineVariant = Color(0xFF5D4037),
    error = Color(0xFFFFCDD2),
    onError = Color(0xFFB71C1C),
    errorContainer = Color(0xFFB71C1C),
    onErrorContainer = Color(0xFFFFCDD2),
)

@Composable
fun MyAdventuringTomeTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}