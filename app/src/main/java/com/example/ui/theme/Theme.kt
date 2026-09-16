package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val ImmersiveDarkColorScheme = darkColorScheme(
    primary = ImmersivePrimaryDark,
    onPrimary = ImmersiveOnPrimaryDark,
    primaryContainer = ImmersivePrimaryContainerDark,
    onPrimaryContainer = ImmersiveOnPrimaryContainerDark,
    secondary = ImmersiveSecondaryDark,
    onSecondary = ImmersiveOnSecondaryDark,
    secondaryContainer = ImmersiveSecondaryContainerDark,
    onSecondaryContainer = ImmersiveOnSecondaryContainerDark,
    tertiary = ImmersiveTertiaryDark,
    onTertiary = ImmersiveOnTertiaryDark,
    tertiaryContainer = ImmersiveTertiaryContainerDark,
    onTertiaryContainer = ImmersiveOnTertiaryContainerDark,
    background = ImmersiveBackgroundDark,
    onBackground = ImmersiveOnSurfaceDark,
    surface = ImmersiveSurfaceDark,
    onSurface = ImmersiveOnSurfaceDark,
    surfaceVariant = ImmersiveSurfaceVariantDark,
    onSurfaceVariant = ImmersiveOnSurfaceVariantDark,
    outline = ImmersiveOutlineDark
)

private val ImmersiveOledColorScheme = ImmersiveDarkColorScheme.copy(
    background = BackgroundOled,
    surface = SurfaceOled,
    surfaceVariant = ImmersiveSurfaceDark
)

private val ImmersiveLightColorScheme = lightColorScheme(
    primary = ImmersivePrimaryLight,
    onPrimary = ImmersiveOnPrimaryLight,
    primaryContainer = ImmersivePrimaryContainerLight,
    onPrimaryContainer = ImmersiveOnPrimaryContainerLight,
    secondary = ImmersiveSecondaryLight,
    onSecondary = ImmersiveOnSecondaryLight,
    secondaryContainer = ImmersiveSecondaryContainerLight,
    onSecondaryContainer = ImmersiveOnSecondaryContainerLight,
    tertiary = ImmersiveTertiaryLight,
    onTertiary = ImmersiveOnTertiaryLight,
    tertiaryContainer = ImmersiveTertiaryContainerLight,
    onTertiaryContainer = ImmersiveOnTertiaryContainerLight,
    background = ImmersiveBackgroundLight,
    onBackground = ImmersiveOnSurfaceLight,
    surface = ImmersiveSurfaceLight,
    onSurface = ImmersiveOnSurfaceLight,
    surfaceVariant = ImmersiveSurfaceVariantLight,
    onSurfaceVariant = ImmersiveOnSurfaceVariantLight,
    outline = ImmersiveOutlineLight
)

@Composable
fun WaterTrackerTheme(
    darkModeSetting: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK", "OLED"
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (darkModeSetting) {
        "LIGHT" -> false
        "DARK", "OLED" -> true
        else -> systemDark
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkModeSetting == "OLED" -> ImmersiveOledColorScheme
        isDark -> ImmersiveDarkColorScheme
        else -> ImmersiveLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


