package app.dsqueez.ui.theme

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * Semantic haptic vocabulary. Map each *intent* to a constant — leaves room to
 * tune across devices without touching call sites.
 *
 * We use the View-based [View.performHapticFeedback] path so we can reach the
 * full Android constant set (SEGMENT_TICK, CONFIRM, GESTURE_END, REJECT, etc.)
 * which the Compose [androidx.compose.ui.hapticfeedback.HapticFeedback] API
 * doesn't fully expose yet.
 */
class DsqHaptics internal constructor(private val view: View) {

    fun tapConfirm() = perform(HapticFeedbackConstants.CONFIRM)

    fun detent() = perform(HapticFeedbackConstants.SEGMENT_TICK)

    fun stretchComplete() = perform(HapticFeedbackConstants.GESTURE_END)

    fun saveThunk() {
        perform(HapticFeedbackConstants.CONFIRM)
        view.postDelayed({ perform(HapticFeedbackConstants.CLOCK_TICK) }, 80L)
    }

    fun reject() = perform(HapticFeedbackConstants.REJECT)

    fun thresholdActivate() = perform(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE)

    private fun perform(constant: Int) {
        if (!view.isHapticFeedbackEnabled) return
        view.performHapticFeedback(constant, View.HAPTIC_FEEDBACK_ENABLED)
    }
}

internal val LocalDsqHaptics = staticCompositionLocalOf<DsqHaptics> {
    error("DsqHaptics not provided — wrap with DsqueezTheme {}")
}

@Composable
internal fun rememberDsqHaptics(): DsqHaptics {
    val view = LocalView.current
    return DsqHaptics(view)
}
