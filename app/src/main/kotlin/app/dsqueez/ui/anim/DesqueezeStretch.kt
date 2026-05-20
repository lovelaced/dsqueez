package app.dsqueez.ui.anim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import app.dsqueez.ui.theme.Dsq
import app.dsqueez.ui.theme.DsqMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * The signature stretch animation. On first appearance the wrapped content is
 * driven from a squeezed ratio of 1.0 up to [ratio] over [DsqMotion.DurSlow]
 * using [DsqMotion.EaseStretch]. In parallel, a calibration-line progress value
 * grows from 0 to 1 alongside the stretch, then fades back to 0 over 200ms once
 * the stretch settles — visualizing the desqueeze in the same way a film
 * cartridge's footage frame would.
 *
 * Subsequent changes to [ratio] animate at springChrome (no bounce); the
 * calibration line does not replay.
 *
 * The wrapped content is passed both the current animated ratio and the
 * current line opacity/width so it can draw the line at the correct moment.
 */
@Composable
fun DesqueezeStretch(
    ratio: Float,
    playOnAppear: Boolean = true,
    content: @Composable (currentRatio: Float, lineProgress: Float) -> Unit,
) {
    val haptics = Dsq.haptics
    val animated = remember { Animatable(if (playOnAppear) 1.0f else ratio) }
    val lineProgress = remember { Animatable(0f) }
    val hasPlayed = remember { BoolHolder(!playOnAppear) }

    LaunchedEffect(ratio, playOnAppear) {
        if (playOnAppear && !hasPlayed.value) {
            animated.snapTo(1.0f)
            lineProgress.snapTo(0f)

            coroutineScope {
                launch {
                    animated.animateTo(
                        targetValue = ratio,
                        animationSpec = tween(DsqMotion.DurSlow, easing = DsqMotion.EaseStretch),
                    )
                }
                launch {
                    lineProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(DsqMotion.DurSlow, easing = DsqMotion.EaseStretch),
                    )
                }
            }

            haptics.stretchComplete()
            lineProgress.animateTo(0f, animationSpec = tween(200))
            hasPlayed.value = true
        } else {
            animated.animateTo(
                targetValue = ratio,
                animationSpec = DsqMotion.springChrome(),
            )
        }
    }

    content(animated.value, lineProgress.value)
}

private class BoolHolder(var value: Boolean)
