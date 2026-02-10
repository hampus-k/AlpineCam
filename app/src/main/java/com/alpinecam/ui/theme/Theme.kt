package com.alpinecam.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AlpineBlue,
    onPrimary = AlpineSnow,
    secondary = AlpineAccent,
    surface = AlpineSurface,
    background = AlpineDark,
    onBackground = AlpineSnow,
    onSurface = AlpineSnow,
)

private val LightColorScheme = lightColorScheme(
    primary = AlpineBlue,
    onPrimary = AlpineSnow,
    secondary = AlpineAccent,
    surface = AlpineSurfaceLight,
    background = AlpineSnow,
    onBackground = AlpineDark,
    onSurface = AlpineDark,
)

@Composable
fun AlpineCamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
