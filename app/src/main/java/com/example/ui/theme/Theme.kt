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

private val DarkColorScheme = darkColorScheme(
    primary = NaturalPrimary,
    onPrimary = Color.White,
    secondary = NaturalAccentBg,
    onSecondary = NaturalTitle,
    tertiary = NaturalStreak,
    background = Color(0xFF0F1B1C), // Deep dark organic teal/midnight
    onBackground = NaturalBg,
    surface = Color(0xFF192526),
    onSurface = NaturalBg,
    surfaceVariant = Color(0xFF243537),
    onSurfaceVariant = Color(0xFF90A1A3)
)

private val LightColorScheme = lightColorScheme(
    primary = NaturalPrimary,
    onPrimary = Color.White,
    secondary = NaturalAccentBg,
    onSecondary = NaturalTitle,
    tertiary = NaturalStreak,
    background = NaturalBg,
    onBackground = NaturalText,
    surface = NaturalCard,
    onSurface = NaturalText,
    surfaceVariant = NaturalAccentBg,
    onSurfaceVariant = NaturalSecondaryText
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false, // Set to false to prioritize our premium custom brand theme!
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

