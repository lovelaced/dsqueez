package app.dsqueez.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import app.dsqueez.ui.theme.Dsq
import app.dsqueez.ui.theme.DsqSpacing

/**
 * A 1dp indeterminate progress bar. Pro photo apps use this in place of
 * spinners — it lives at the bottom of the photo frame and never spins.
 */
@Composable
fun HairlineProgress(
    modifier: Modifier = Modifier,
    color: Color = Dsq.colors.accent,
) {
    val transition = rememberInfiniteTransition(label = "hairline")
    val pos by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "hairlinePos",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(DsqSpacing.hairline),
    ) {
        val w = size.width
        val barW = w * 0.35f
        val x = pos * (w + barW) - barW
        drawRect(
            color = color,
            topLeft = Offset(x, 0f),
            size = Size(barW, size.height),
        )
    }
}
