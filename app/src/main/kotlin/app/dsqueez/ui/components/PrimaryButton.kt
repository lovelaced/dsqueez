package app.dsqueez.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.dsqueez.ui.theme.Dsq
import app.dsqueez.ui.theme.DsqSpacing

enum class PrimaryButtonStyle { Solid, Outlined }

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: PrimaryButtonStyle = PrimaryButtonStyle.Solid,
) {
    val colors = Dsq.colors
    val haptics = Dsq.haptics
    val shape = RoundedCornerShape(2.dp)
    val interaction = remember { MutableInteractionSource() }

    val (bg, fg, border) = when (style) {
        PrimaryButtonStyle.Solid -> Triple(colors.accent, colors.accentOn, null)
        PrimaryButtonStyle.Outlined -> Triple(colors.bg, colors.textPrimary, colors.stroke)
    }

    val opacity = if (enabled) 1f else 0.4f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minHeight = DsqSpacing.touch)
            .height(DsqSpacing.touch)
            .clip(shape)
            .background(bg.copy(alpha = opacity))
            .let { if (border != null) it.border(DsqSpacing.hairline, border, shape) else it }
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = {
                    haptics.tapConfirm()
                    onClick()
                },
            )
            .padding(horizontal = DsqSpacing.lg),
    ) {
        Text(
            text = label,
            style = Dsq.type.label,
            color = fg.copy(alpha = opacity),
            textAlign = TextAlign.Center,
        )
    }
}
