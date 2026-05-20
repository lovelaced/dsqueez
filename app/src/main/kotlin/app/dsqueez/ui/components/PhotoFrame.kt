package app.dsqueez.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
) {
    val colors = Dsq.colors
    val imageBitmap: ImageBitmap? = remember(bitmap) { bitmap?.asImageBitmap() }
    val desqAspect = if (sourceHeight > 0) (sourceWidth * ratio) / sourceHeight else 1f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(colors.bg),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(desqAspect)
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
                            scaleX = ratio
                            this.alpha = alpha.value
                        },
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}
