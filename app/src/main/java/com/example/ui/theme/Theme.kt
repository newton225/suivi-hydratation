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

private val DarkColorScheme = darkColorScheme(
  primary = TurquoisePrimary,
  secondary = TurquoiseSecondary,
  tertiary = TurquoiseTertiary,
  background = DarkBackground,
  surface = DarkSurface,
  onPrimary = androidx.compose.ui.graphics.Color(0xFF00363A),
  onSecondary = androidx.compose.ui.graphics.Color(0xFF003538),
  onTertiary = androidx.compose.ui.graphics.Color(0xFF003538),
  onBackground = OnDarkSurface,
  onSurface = OnDarkSurface,
  surfaceVariant = DarkSurface,
  onSurfaceVariant = OnDarkSurface
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme as requested by user
  dynamicColor: Boolean = false, // Set to false to prioritize our beautiful turquoise theme colors
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      else -> DarkColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
