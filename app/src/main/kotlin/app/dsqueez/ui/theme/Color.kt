package app.dsqueez.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class DsqColors(
    val bg: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,

    val accent: Color,
    val accentMuted: Color,
    val accentOn: Color,

    val armed: Color,

    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,

    val divider: Color,
    val stroke: Color,

    val success: Color,
    val error: Color,
    val warning: Color,

    val isDark: Boolean,
)

internal val DarkColors = DsqColors(
    bg            = Color(0xFF0A0A0B),
    surface1      = Color(0xFF131315),
    surface2      = Color(0xFF1C1C1F),
    surface3      = Color(0xFF26262A),

    accent        = Color(0xFFE8B23A),
    accentMuted   = Color(0xFF8A6A22),
    accentOn      = Color(0xFF1A1304),

    armed         = Color(0xFFE5484D),

    textPrimary   = Color(0xFFF2F2F3),
    textSecondary = Color(0xFFA1A1A6),
    textTertiary  = Color(0xFF6B6B70),

    divider       = Color(0xFF2A2A2E),
    stroke        = Color(0xFF3A3A3F),

    success       = Color(0xFF7BC47F),
    error         = Color(0xFFE5484D),
    warning       = Color(0xFFE8B23A),

    isDark        = true,
)

internal val LightColors = DsqColors(
    bg            = Color(0xFFF5F4F0),
    surface1      = Color(0xFFFFFFFF),
    surface2      = Color(0xFFEDEBE5),
    surface3      = Color(0xFFE2E0D9),

    accent        = Color(0xFFB8821C),
    accentMuted   = Color(0xFFD9C68C),
    accentOn      = Color(0xFFFFFFFF),

    armed         = Color(0xFFC0292E),

    textPrimary   = Color(0xFF14130F),
    textSecondary = Color(0xFF55534D),
    textTertiary  = Color(0xFF8C8A82),

    divider       = Color(0xFFD9D6CE),
    stroke        = Color(0xFFC4C0B5),

    success       = Color(0xFF3E8E45),
    error         = Color(0xFFC0292E),
    warning       = Color(0xFFB8821C),

    isDark        = false,
)
