package com.talent.monthlybudgetkeeper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Snow,
    primaryContainer = NavySoft,
    onPrimaryContainer = InkDeep,
    secondary = Emerald,
    onSecondary = Snow,
    secondaryContainer = EmeraldSoft,
    onSecondaryContainer = InkDeep,
    tertiary = Brass,
    onTertiary = Snow,
    tertiaryContainer = BrassSoft,
    onTertiaryContainer = InkDeep,
    error = Rose,
    onError = Snow,
    errorContainer = RoseSoft,
    onErrorContainer = InkDeep,
    background = Ivory,
    onBackground = Ink,
    surface = Snow,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Slate,
    outline = OutlineLight,
    outlineVariant = Cloud,
    scrim = InkDeep.copy(alpha = 0.38f),
    inverseSurface = Ink,
    inverseOnSurface = Snow,
    inversePrimary = Ice,
    surfaceTint = Navy
)

private val DarkColors = darkColorScheme(
    primary = Ice,
    onPrimary = InkDeep,
    primaryContainer = Color(0xFF173450),
    onPrimaryContainer = Frost,
    secondary = Mint,
    onSecondary = InkDeep,
    secondaryContainer = Color(0xFF193A33),
    onSecondaryContainer = Color(0xFFD5EEE7),
    tertiary = Color(0xFFE4C988),
    onTertiary = InkDeep,
    tertiaryContainer = Color(0xFF3A2F16),
    onTertiaryContainer = Color(0xFFF4E8C9),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD5),
    background = Carbon,
    onBackground = Frost,
    surface = Graphite,
    onSurface = Frost,
    surfaceVariant = GraphiteSoft,
    onSurfaceVariant = FrostDeep,
    outline = OutlineDark,
    outlineVariant = GraphiteElevated,
    scrim = Color.Black.copy(alpha = 0.44f),
    inverseSurface = Snow,
    inverseOnSurface = Ink,
    inversePrimary = NavySoft,
    surfaceTint = Ice
)

@Composable
fun MonthlyBudgetKeeperTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
