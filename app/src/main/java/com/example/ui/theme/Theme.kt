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

enum class AppThemeStyle(val displayName: String, val amharicName: String) {
    CLASSIC_GOLD("Classic Obsidian Gold", "ክላሲክ ወርቃማ"),
    NEON_RACER("Sporty Neon Racer", "ስፖርታዊ ኒዮን"),
    ROYAL_CARBON("Royal Blue Carbon", "ሮያል ሰማያዊ"),
    LUXURY_LEATHER("Luxury Leather Bronze", "ቅንጡ ሌዘር")
}

// 1. CLASSIC GOLD COLORS (Existing default)
private val ClassicDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB703),
    onPrimary = Color(0xFF3D2C00),
    secondary = Color(0xFF2C3E50),
    background = Color(0xFF0F1115),
    surface = Color(0xFF1B1E23),
    onBackground = Color(0xFFE9EDF0),
    onSurface = Color(0xFFECEFF1)
)

// 2. NEON RACER COLORS (Sporty Automotive look)
private val NeonDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00F0FF), // Electric Neon Cyan
    onPrimary = Color(0xFF00383D),
    secondary = Color(0xFF2B2B2B),
    background = Color(0xFF050709), // Ultimate Midnight
    surface = Color(0xFF12181F), // Sport Carbon Dark Card
    onBackground = Color(0xFFE0F7FA),
    onSurface = Color(0xFFECEFF1)
)

// 3. ROYAL CARBON COLORS (Chassis Royal Steel)
private val RoyalDarkColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6), // Royal Sapphire Blue
    onPrimary = Color(0xFF001E3D),
    secondary = Color(0xFF334155),
    background = Color(0xFF0B1120), // Royal Dark Chassis
    surface = Color(0xFF152033), // Metallic Steel Blue
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF1F5F9)
)

// 4. LUXURY LEATHER COLORS (Premium Dashboard Interior)
private val LeatherDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE5A93C), // Golden Bronze Leather
    onPrimary = Color(0xFF3A2300),
    secondary = Color(0xFF4A3E3D),
    background = Color(0xFF0D0A09), // Espresso Matte Background
    surface = Color(0xFF1C1614), // Premium Leather Card
    onBackground = Color(0xFFF5EFEA),
    onSurface = Color(0xFFFAF7F2)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD48B00), // Amber gold accent
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF3E4E50), // Carbon Grey
    background = Color(0xFFFAFAFC), // Clean Light Grey
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1E2229),
    onSurface = Color(0xFF1E2229)
)

@Composable
fun AbrahamDecorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeStyle: AppThemeStyle = AppThemeStyle.CLASSIC_GOLD,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        !darkTheme -> LightColorScheme
        else -> {
            when (themeStyle) {
                AppThemeStyle.CLASSIC_GOLD -> ClassicDarkColorScheme
                AppThemeStyle.NEON_RACER -> NeonDarkColorScheme
                AppThemeStyle.ROYAL_CARBON -> RoyalDarkColorScheme
                AppThemeStyle.LUXURY_LEATHER -> LeatherDarkColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
