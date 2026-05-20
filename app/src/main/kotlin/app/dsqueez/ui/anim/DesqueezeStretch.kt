package app.dsqueez.ui.anim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import app.dsqueez.ui.theme.Dsq
import app.dsqueez.ui.theme.DsqMotion

/**
 * The signature stretch animation. On first appearance the wrapped content is
 * driven from a squeezed ratio of 1.0 up to [ratio] over [DsqMotion.DurSlow]
 * using [DsqMotion.EaseStretch]. Subsequent changes to [ratio] animate at
 * springChrome (no bounce, slightly slower than instant).
 *
 * The wrapped content is passed the *current* (animated) ratio so it can apply
 * it to a graphicsLayer scaleX.
 */
@Composable
fun DesqueezeStretch(
    ratio: Float,
    playOnAppear: Boolean = true,
    content: @Composable (currentRatio: Float) -> Unit,
) {
    val haptics = Dsq.haptics
    val animated = remember { Animatable(if (playOnAppear) 1.0f else ratio) }
    val hasPlayed = remember { BoolHolder(!playOnAppear) }

    LaunchedEffect(ratio, playOnAppear) {
        if (playOnAppear && !hasPlayed.value) {
            // Initial reveal: 1.0 → ratio over DurSlow, then a haptic kiss
            animated.snapTo(1.0f)
            animated.animateTo(
                targetValue = ratio,
                animationSpec = tween(DsqMotion.DurSlow, easing = DsqMotion.EaseStretch),
            )
            haptics.stretchComplete()
            hasPlayed.value = true
        } else {
            animated.animateTo(
                targetValue = ratio,
                animationSpec = DsqMotion.springChrome(),
            )
        }
    }

    content(animated.value)
}

private class BoolHolder(var value: Boolean)
