package com.sdkecuador.gemininano.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    palette: String = "DEFAULT",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    MaterialTheme(
        colorScheme = when (palette) {
            "FOREST" -> paletteScheme(Color(0xFF2E7D32), Color(0xFFA5D6A7), useDarkTheme)
            "DOMINICAN_REPUBLIC" -> paletteScheme(Color(0xFF003DA5), Color(0xFFCE1126), useDarkTheme)
            "RAIN" -> paletteScheme(Color(0xFF546E7A), Color(0xFFB0BEC5), useDarkTheme)
            "OCEAN" -> paletteScheme(Color(0xFF006C80), Color(0xFF9CD9E8), useDarkTheme)
            "SUNSET" -> paletteScheme(Color(0xFFA34720), Color(0xFFFFC3A3), useDarkTheme)
            "LAVENDER" -> paletteScheme(Color(0xFF74529A), Color(0xFFD8C4F0), useDarkTheme)
            else -> if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        },
        typography = appTypography,
        content = content
    )
}

private fun paletteScheme(primary: Color, soft: Color, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = soft,
        onPrimary = Color(0xFF172019),
        primaryContainer = primary,
        onPrimaryContainer = Color.White,
        background = Color(0xFF121713),
        surface = Color(0xFF1C241E)
    )
} else {
    lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = soft,
        onPrimaryContainer = Color(0xFF142018),
        background = Color(0xFFF8FBF7),
        surface = Color.White
    )
}
