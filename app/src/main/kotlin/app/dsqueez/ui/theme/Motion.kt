package app.dsqueez.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object DsqMotion {
    // Durations (ms)
    const val DurInstant = 80
    const val DurQuick   = 180
    const val DurNormal  = 280
    const val DurSlow    = 520
    const val DurReveal  = 720

    // Easings — custom; M3 defaults read too "consumer"
    val EaseStandard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EaseExit     = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val EaseStretch  = CubicBezierEasing(0.65f, 0.0f, 0.35f, 1.0f)

    // Springs (factory functions — Compose requires a fresh AnimationSpec per use)
    fun <T> springChrome() = spring<T>(dampingRatio = 1f,    stiffness = 800f)
    fun <T> springSheet()  = spring<T>(dampingRatio = 0.9f,  stiffness = 500f)
    fun <T> springDial()   = spring<T>(dampingRatio = 0.75f, stiffness = 400f)
    fun <T> springSettle() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
}
