package app.dsqueez.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import app.dsqueez.ui.theme.Dsq
import app.dsqueez.ui.theme.DsqSpacing

/**
 * The Halide-style metadata strip. A single row of monospace key:value pairs,
 * with preamble glyphs in [Dsq.colors.textTertiary] and values in [textPrimary],
 * separated by middle-dot glyphs.
 *
 * Example: SOURCE 6000×4000 · RATIO 1.33× · OUTPUT 7980×4000 · JPEG
 */
@Composable
fun MetadataStrip(
    items: List<MetadataItem>,
    modifier: Modifier = Modifier,
) {
    val colors = Dsq.colors

    val text = buildAnnotatedString {
        items.forEachIndexed { idx, item ->
            withStyle(SpanStyle(color = colors.textTertiary)) {
                append(item.label)
                append(" ")
            }
            withStyle(SpanStyle(color = colors.textPrimary)) {
                append(item.value)
            }
            if (idx != items.lastIndex) {
                withStyle(SpanStyle(color = colors.textTertiary)) {
                    append("  ·  ")
                }
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DsqSpacing.screenH, vertical = DsqSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = Dsq.type.numBody,
        )
    }
}

data class MetadataItem(val label: String, val value: String)
