package app.dsqueez.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.dsqueez.ui.theme.Dsq
import app.dsqueez.ui.theme.DsqSpacing

/**
 * Segmented selector over the supported squeeze ratios.
 *
 * v1 ships with a single detent (1.33×) — the data type is already plural so
 * adding 1.5×, 1.55×, 2.0× later is a one-line change to [SUPPORTED_RATIOS].
 */
@Composable
fun RatioControl(
    selected: Float,
    onSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DsqSpacing.screenH),
    ) {
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
            labelFor = { "${formatRatio(it)}×" },
        )
    }
}

val SUPPORTED_RATIOS: List<Float> = listOf(1.33f)

private fun formatRatio(r: Float): String = "%.2f".format(r)
