package app.dsqueez.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.dsqueez.ui.theme.Dsq
import app.dsqueez.ui.theme.DsqMotion
import app.dsqueez.ui.theme.DsqSpacing

@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    labelFor: (T) -> String,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle? = null,
) {
    val colors = Dsq.colors
    val haptics = Dsq.haptics
    val shape = RoundedCornerShape(2.dp)
    val style = labelStyle ?: Dsq.type.numBody

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DsqSpacing.touch)
            .clip(shape)
            .background(colors.surface1)
            .border(DsqSpacing.hairline, colors.stroke, shape),
    ) {
        options.forEachIndexed { idx, opt ->
            val isSelected = opt == selected
            val bg by animateColorAsState(
                if (isSelected) colors.surface3 else colors.surface1,
                animationSpec = DsqMotion.springChrome(),
                label = "segBg",
            )
            val fg by animateColorAsState(
                if (isSelected) colors.accent else colors.textSecondary,
                animationSpec = DsqMotion.springChrome(),
                label = "segFg",
            )
            val interaction = remember { MutableInteractionSource() }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(bg)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                    ) {
                        if (!isSelected) {
                            haptics.detent()
                            onSelected(opt)
                        }
                    }
                    .padding(horizontal = DsqSpacing.xs),
            ) {
                Text(
                    text = labelFor(opt),
                    style = style,
                    maxLines = 1,
                    softWrap = false,
                    color = fg,
                    textAlign = TextAlign.Center,
                )
            }
            if (idx != options.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(DsqSpacing.hairline)
                        .background(colors.stroke),
                )
            }
        }
    }
}
