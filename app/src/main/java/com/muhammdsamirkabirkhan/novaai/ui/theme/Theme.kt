package com.muhammdsamirkabirkhan.novaai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.muhammdsamirkabirkhan.novaai.findActivity

private val LightColors = lightColorScheme(
    primary = Color(0xFF6C4CF1), onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E0FF), onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF00857F), background = Color(0xFFFBF9FF), surface = Color(0xFFFBF9FF),
    surfaceVariant = Color(0xFFEFEAFB), onSurfaceVariant = Color(0xFF49454F),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7A6FF), onPrimary = Color(0xFF2A0F8F),
    primaryContainer = Color(0xFF4B2FCB), onPrimaryContainer = Color(0xFFE7E0FF),
    secondary = Color(0xFF4DD9D9), background = Color(0xFF0E0B1A), surface = Color(0xFF0E0B1A),
    surfaceVariant = Color(0xFF241F38), onSurfaceVariant = Color(0xFFCAC4D0),
)

@Composable
fun NovaTheme(dark: Boolean, content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val w = view.context.findActivity().window
        WindowCompat.getInsetsController(w, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        shapes = Shapes(medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp)),
        content = content,
    )
}
