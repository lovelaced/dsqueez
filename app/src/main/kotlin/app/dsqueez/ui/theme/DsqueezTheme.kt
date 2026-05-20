package app.dsqueez.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalDsqColors = staticCompositionLocalOf { DarkColors }
val LocalDsqTypography = staticCompositionLocalOf { DsqTypographyDefault }

@Composable
fun DsqueezTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dsq = if (dark) DarkColors else LightColors

    // Feed Material 3's ColorScheme with our values where they overlap. We mostly
    // bypass M3 colors at call sites (using LocalDsqColors), but some primitives
    // (ripples, sheet scrim) pull from MaterialTheme.colorScheme.
    val m3 = if (dark) {
        darkColorScheme(
            primary = dsq.accent,
            onPrimary = dsq.accentOn,
            secondary = dsq.accent,
            background = dsq.bg,
            onBackground = dsq.textPrimary,
            surface = dsq.surface1,
            onSurface = dsq.textPrimary,
            surfaceVariant = dsq.surface2,
            onSurfaceVariant = dsq.textSecondary,
            outline = dsq.stroke,
            outlineVariant = dsq.divider,
            error = dsq.error,
        )
    } else {
        lightColorScheme(
            primary = dsq.accent,
            onPrimary = dsq.accentOn,
            secondary = dsq.accent,
            background = dsq.bg,
            onBackground = dsq.textPrimary,
            surface = dsq.surface1,
            onSurface = dsq.textPrimary,
            surfaceVariant = dsq.surface2,
            onSurfaceVariant = dsq.textSecondary,
            outline = dsq.stroke,
            outlineVariant = dsq.divider,
            error = dsq.error,
        )
    }

    // System bar appearance must match the theme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    val haptics = rememberDsqHaptics()

    CompositionLocalProvider(
        LocalDsqColors provides dsq,
        LocalDsqTypography provides DsqTypographyDefault,
        LocalDsqHaptics provides haptics,
    ) {
        MaterialTheme(
            colorScheme = m3,
            content = content,
        )
    }
}

object Dsq {
    val colors: DsqColors @Composable get() = LocalDsqColors.current
    val type:   DsqTypography @Composable get() = LocalDsqTypography.current
    val haptics: DsqHaptics @Composable get() = LocalDsqHaptics.current
}
