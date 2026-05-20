package app.dsqueez.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.dsqueez.ui.theme.Dsq
import app.dsqueez.ui.theme.DsqSpacing

/**
 * Segmented selector over the supported squeeze ratios. The set matches the
 * Lumix S9's native video desqueeze modes — same number you set in-camera
 * for video applies here for stills.
 *
 * Behavior:
 *   - tap            : selects the ratio for this photo (transient)
 *   - long-press     : persists as your default + selects (via onSetDefault)
 *
 * Visual cues:
 *   - selected ratio : accent-filled chip (existing segmented behavior)
 *   - default ratio  : small dot under the chip, shown only when the default
 *                      is NOT the current selection (the accent fill is the
 *                      stronger signal when both states coincide)
 *   - detected lens  : a "DETECTED · {lens}" line above the row, when the
 *                      photo's EXIF announces an anamorphic ratio
 */
@Composable
fun RatioControl(
    selected: Float,
    onSelected: (Float) -> Unit,
    onSetDefault: (Float) -> Unit,
    modifier: Modifier = Modifier,
    defaultRatio: Float? = null,
    detectedLensModel: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DsqSpacing.screenH),
    ) {
        if (detectedLensModel != null) {
            Text(
                text = "DETECTED · ${detectedLensModel.uppercase()}",
                style = Dsq.type.micro,
                color = Dsq.colors.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(DsqSpacing.sm))
        }

        Text(
            text = "RATIO",
            style = Dsq.type.micro,
            color = Dsq.colors.textTertiary,
        )
        Spacer(modifier = Modifier.height(DsqSpacing.sm))
        SegmentedControl(
            options = SUPPORTED_RATIOS,
            selected = selected,
            onSelected = onSelected,
            labelFor = { formatRatio(it) },
            markedOption = defaultRatio,
            onLongPress = onSetDefault,
        )
    }
}

val SUPPORTED_RATIOS: List<Float> = listOf(1.30f, 1.33f, 1.50f, 1.60f, 1.80f, 2.00f)

/** Two-decimal precision keeps all labels at equal width — tabular numerals
 *  in the segmented control depend on it for the engraved-tool look. */
fun formatRatio(r: Float): String = "%.2f×".format(r)
