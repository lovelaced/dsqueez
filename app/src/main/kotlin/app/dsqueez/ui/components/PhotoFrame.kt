package app.dsqueez.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import app.dsqueez.ui.theme.Dsq
import app.dsqueez.ui.theme.DsqMotion
import app.dsqueez.ui.theme.DsqSpacing
import kotlinx.coroutines.delay

@Composable
fun PhotoFrame(
    bitmap: Bitmap?,
    sourceWidth: Int,
    sourceHeight: Int,
    ratio: Float,
    modifier: Modifier = Modifier,
    revealAnimated: Boolean = true,
    calibrationLineProgress: Float = 0f,
    // True for portrait captures: the anamorphic squeeze sits on the vertical
    // axis, so the desqueeze stretches height rather than width. [sourceWidth]/
    // [sourceHeight] are the upright (orientation-applied) dimensions.
    stretchVertical: Boolean = false,
) {
    val colors = Dsq.colors
    val imageBitmap: ImageBitmap? = remember(bitmap) { bitmap?.asImageBitmap() }
    val desqAspect = when {
        sourceHeight <= 0 || sourceWidth <= 0 -> 1f
        stretchVertical -> sourceWidth / (sourceHeight * ratio)
        else            -> (sourceWidth * ratio) / sourceHeight
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(colors.bg),
        contentAlignment = Alignment.Center,
    ) {
        // Fit the framed photo within the available space, preserving its
        // desqueezed aspect. Landscape stays width-bound (as before); a tall
        // portrait desqueeze is height-bound so it never overflows the frame.
        val heightBound = constraints.hasBoundedHeight &&
            (maxWidth.value / maxHeight.value) > desqAspect
        val frameModifier = if (heightBound) {
            Modifier.fillMaxHeight().aspectRatio(desqAspect)
        } else {
            Modifier.fillMaxWidth().aspectRatio(desqAspect)
        }
        Box(
            modifier = frameModifier
                .border(DsqSpacing.hairline, colors.stroke.copy(alpha = 0.6f)),
        ) {
            if (imageBitmap != null) {
                val alpha = remember { Animatable(if (revealAnimated) 0f else 1f) }
                LaunchedEffect(imageBitmap, revealAnimated) {
                    if (revealAnimated) {
                        alpha.snapTo(0f)
                        delay(120)
                        alpha.animateTo(
                            1f,
                            animationSpec = tween(DsqMotion.DurNormal, easing = DsqMotion.EaseStandard),
                        )
                    } else {
                        alpha.snapTo(1f)
                    }
                }
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (stretchVertical) scaleY = ratio else scaleX = ratio
                            this.alpha = alpha.value
                        },
                    contentScale = ContentScale.Fit,
                )
            }

            if (calibrationLineProgress > 0f) {
                if (stretchVertical) {
                    // Vertical hairline tracing up the start edge as height grows.
                    CalibrationLine(
                        progress = calibrationLineProgress,
                        vertical = true,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(DsqSpacing.hairline)
                            .align(Alignment.CenterStart),
                    )
                } else {
                    CalibrationLine(
                        progress = calibrationLineProgress,
                        vertical = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(DsqSpacing.hairline)
                            .align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

/**
 * The 1px amber hairline that traces alongside the photo during the desqueeze.
 * Grows from center outward along the stretch axis, matching the visual metaphor:
 * horizontal for a landscape desqueeze, [vertical] for a portrait one.
 */
@Composable
private fun CalibrationLine(progress: Float, vertical: Boolean, modifier: Modifier = Modifier) {
    val p = progress.coerceIn(0f, 1f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = (if (vertical) {
                Modifier.fillMaxHeight(p).width(DsqSpacing.hairline)
            } else {
                Modifier.fillMaxWidth(p).height(DsqSpacing.hairline)
            }).background(Dsq.colors.accent),
        )
    }
}
