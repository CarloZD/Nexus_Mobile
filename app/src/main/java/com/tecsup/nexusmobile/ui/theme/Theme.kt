package com.tecsup.nexusmobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.app.Activity

private val DarkColorScheme = darkColorScheme(
    primary = NexusPurpleLight,
    onPrimary = Color.White,
    primaryContainer = NexusPurpleDark,
    onPrimaryContainer = NexusPurpleLight,

    secondary = NexusAccentLight,
    onSecondary = Color.White,
    secondaryContainer = NexusAccentDark,
    onSecondaryContainer = NexusAccentLight,

    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),

    background = BackgroundDark,
    onBackground = Color(0xFFE6E1E5),

    surface = SurfaceDark,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),

    error = ErrorDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = ErrorDark,

    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

private val LightColorScheme = lightColorScheme(
    primary = NexusPurple,
    onPrimary = Color.White,
    primaryContainer = NexusPurpleLight,
    onPrimaryContainer = NexusPurpleDark,

    secondary = NexusAccent,
    onSecondary = Color.White,
    secondaryContainer = NexusAccentLight,
    onSecondaryContainer = NexusAccentDark,

    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,

    background = BackgroundLight,
    onBackground = Color(0xFF1C1B1F),

    surface = SurfaceLight,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),

    error = ErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

@Composable
fun Nexus_mobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Deshabilitado por defecto para usar nuestros colores
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

    // Configurar los colores de la barra de estado y navegación
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val colorValue = if (darkTheme) 0xFF1C1B1F else 0xFFFAF9FC
            window.statusBarColor = colorValue.toInt()
            window.navigationBarColor = colorValue.toInt()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}