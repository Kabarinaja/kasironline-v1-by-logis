package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = BrandToska,
    onPrimary = Color.White,
    primaryContainer = BrandToskaContainer,
    onPrimaryContainer = BrandToskaDark,
    
    secondary = BrandSage,
    onSecondary = Color.White,
    secondaryContainer = BrandSageContainer,
    onSecondaryContainer = Color(0xFF283B1A),
    
    tertiary = BrandOrange,
    onTertiary = Color.White,
    tertiaryContainer = BrandOrangeContainer,
    onTertiaryContainer = BrandOrangeDark,
    
    background = BackgroundLight,
    onBackground = BrandGrayDark,
    surface = SurfaceLight,
    onSurface = BrandGrayDark,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = BrandGray,
    outline = BrandGrayBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandToskaDarkTheme,
    onPrimary = Color(0xFF00363B),
    primaryContainer = BrandToskaDark,
    onPrimaryContainer = Color(0xFFA5EDF4),
    
    secondary = BrandSageDarkTheme,
    onSecondary = Color(0xFF233516),
    secondaryContainer = Color(0xFF394D2B),
    onSecondaryContainer = Color(0xFFD6E4C8),
    
    tertiary = BrandOrangeDarkTheme,
    onTertiary = Color(0xFF4A2300),
    tertiaryContainer = Color(0xFF703800),
    onTertiaryContainer = Color(0xFFFFDCC1),
    
    background = BackgroundDark,
    onBackground = Color(0xFFE2E7EA),
    surface = SurfaceDark,
    onSurface = Color(0xFFE2E7EA),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF3F4E55)
)

@Composable
fun KasirDuniaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep brand identity dominant
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
