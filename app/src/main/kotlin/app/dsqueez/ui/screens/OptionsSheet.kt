package app.dsqueez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.dsqueez.R
import app.dsqueez.ui.components.SegmentedControl
import app.dsqueez.ui.theme.Dsq
import app.dsqueez.ui.theme.DsqSpacing

enum class OutputFormatChoice(val label: String) {
    MatchInput("Match Input"),
    AlwaysJpeg("Always JPEG"),
}

@Composable
fun OptionsSheet(
    exportAsJpeg: Boolean,
    onExportAsJpegChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Dsq.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface1,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
        scrimColor = colors.bg.copy(alpha = 0.7f),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(DsqSpacing.lg),
        ) {
            Text(
                text = stringResource(R.string.options_title),
                style = Dsq.type.title,
                color = colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(DsqSpacing.xl))

            OptionRow(label = stringResource(R.string.option_output_format)) {
                val selected = if (exportAsJpeg) OutputFormatChoice.AlwaysJpeg else OutputFormatChoice.MatchInput
                SegmentedControl(
                    options = OutputFormatChoice.entries,
                    selected = selected,
                    onSelected = { onExportAsJpegChange(it == OutputFormatChoice.AlwaysJpeg) },
                    labelFor = { it.label },
                    labelStyle = Dsq.type.label,
                )
            }

            Spacer(modifier = Modifier.height(DsqSpacing.lg))
            Divider()
            Spacer(modifier = Modifier.height(DsqSpacing.lg))

            OptionRow(label = stringResource(R.string.option_exif)) {
                Text(
                    text = stringResource(R.string.option_exif_value),
                    style = Dsq.type.body,
                    color = colors.textSecondary,
                )
            }

            Spacer(modifier = Modifier.height(DsqSpacing.lg))
        }
    }
}

@Composable
private fun OptionRow(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            style = Dsq.type.micro,
            color = Dsq.colors.textTertiary,
        )
        Spacer(modifier = Modifier.height(DsqSpacing.sm))
        content()
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(DsqSpacing.hairline)
            .background(Dsq.colors.divider),
    )
}
