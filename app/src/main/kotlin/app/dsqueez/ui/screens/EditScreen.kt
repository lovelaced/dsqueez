package app.dsqueez.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.dsqueez.R
import app.dsqueez.photo.FailureReason
import app.dsqueez.photo.PhotoMetadata
import app.dsqueez.photo.PhotoSource
import app.dsqueez.photo.Pipeline
import app.dsqueez.photo.SaveResult
import app.dsqueez.photo.SourceFormat
import app.dsqueez.settings.UserPrefs
import app.dsqueez.ui.anim.DesqueezeStretch
import app.dsqueez.ui.components.HairlineProgress
import app.dsqueez.ui.components.MetadataItem
import app.dsqueez.ui.components.MetadataStrip
import app.dsqueez.ui.components.PhotoFrame
import app.dsqueez.ui.components.PrimaryButton
import app.dsqueez.ui.components.RatioControl
import app.dsqueez.ui.components.SUPPORTED_RATIOS
import app.dsqueez.ui.theme.Dsq
import app.dsqueez.ui.theme.DsqMotion
import app.dsqueez.ui.theme.DsqSpacing
import kotlinx.coroutines.launch

private sealed interface EditState {
    data object Loading : EditState
    data class Ready(val metadata: PhotoMetadata, val preview: Bitmap?) : EditState
    data class Saving(val metadata: PhotoMetadata, val preview: Bitmap?) : EditState
    data class Saved(val metadata: PhotoMetadata, val preview: Bitmap?, val result: SaveResult.Success) : EditState
    data class Error(val reason: FailureReason) : EditState
}

@Composable
fun EditScreen(
    uri: Uri,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember { UserPrefs(context.applicationContext) }
    val exportAsJpeg by prefs.exportAsJpeg.collectAsState(initial = true)

    var state by remember(uri) { mutableStateOf<EditState>(EditState.Loading) }
    var optionsOpen by remember { mutableStateOf(false) }
    var ratio by remember { mutableStateOf(SUPPORTED_RATIOS.first()) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(uri) {
        state = EditState.Loading
        runCatching {
            val md = PhotoSource.readMetadata(context, uri)
            val preview = if (md.supported) {
                PhotoSource.decodePreview(context, uri, md.pixelWidth, md.pixelHeight)
            } else null
            if (md.supported) EditState.Ready(md, preview) else EditState.Error(FailureReason.UNSUPPORTED_FORMAT)
        }.fold(
            onSuccess = { state = it },
            onFailure = { state = EditState.Error(FailureReason.READ_FAILED) },
        )
    }

    Box(modifier = modifier.fillMaxSize().background(Dsq.colors.bg)) {
        when (val s = state) {
            is EditState.Loading -> LoadingPanel(onBack)
            is EditState.Error -> ErrorPanel(s.reason, onBack)
            is EditState.Ready -> ReadyPanel(
                metadata = s.metadata,
                preview = s.preview,
                ratio = ratio,
                exportAsJpeg = exportAsJpeg,
                isSaving = false,
                onBack = onBack,
                onRatioChange = { ratio = it },
                onOpenOptions = { optionsOpen = true },
                onSave = {
                    val md = s.metadata
                    val preview = s.preview
                    state = EditState.Saving(md, preview)
                    scope.launch {
                        val result = Pipeline.process(context, md, ratio, exportAsJpeg)
                        state = when (result) {
                            is SaveResult.Success -> EditState.Saved(md, preview, result)
                            is SaveResult.Failure -> EditState.Error(result.reason)
                        }
                    }
                },
            )
            is EditState.Saving -> ReadyPanel(
                metadata = s.metadata,
                preview = s.preview,
                ratio = ratio,
                exportAsJpeg = exportAsJpeg,
                isSaving = true,
                onBack = onBack,
                onRatioChange = { ratio = it },
                onOpenOptions = {},
                onSave = {},
            )
            is EditState.Saved -> SavedPanel(
                metadata = s.metadata,
                preview = s.preview,
                ratio = ratio,
                onBack = onBack,
            )
        }

        if (optionsOpen) {
            OptionsSheet(
                exportAsJpeg = exportAsJpeg,
                onExportAsJpegChange = { scope.launch { prefs.setExportAsJpeg(it) } },
                onDismiss = { optionsOpen = false },
            )
        }
    }
}

@Composable
private fun ReadyPanel(
    metadata: PhotoMetadata,
    preview: Bitmap?,
    ratio: Float,
    exportAsJpeg: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onRatioChange: (Float) -> Unit,
    onOpenOptions: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        TopBar(onBack = onBack, onOpenOptions = onOpenOptions)

        Spacer(modifier = Modifier.height(DsqSpacing.md))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            DesqueezeStretch(
                ratio = ratio,
                playOnAppear = true,
            ) { animatedRatio, lineProgress ->
                PhotoFrame(
                    bitmap = preview,
                    sourceWidth = metadata.pixelWidth,
                    sourceHeight = metadata.pixelHeight,
                    ratio = animatedRatio,
                    revealAnimated = true,
                    calibrationLineProgress = lineProgress,
                )
            }
        }

        Spacer(modifier = Modifier.height(DsqSpacing.md))

        MetadataStrip(
            items = buildMetadataItems(metadata, ratio, exportAsJpeg),
        )

        Spacer(modifier = Modifier.height(DsqSpacing.md))

        RatioControl(
            selected = ratio,
            onSelected = onRatioChange,
        )

        Spacer(modifier = Modifier.height(DsqSpacing.lg))

        BottomActionBar(isSaving = isSaving, onSave = onSave)
    }
}

@Composable
private fun SavedPanel(
    metadata: PhotoMetadata,
    preview: Bitmap?,
    ratio: Float,
    onBack: () -> Unit,
) {
    val haptics = Dsq.haptics
    val colors = Dsq.colors
    val alpha = remember { Animatable(1f) }
    val sweepProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Success animation: dim photo, sweep success hairline, thunk haptic
        alpha.animateTo(0.4f, animationSpec = tween(120))
        haptics.saveThunk()
        sweepProgress.animateTo(1f, animationSpec = tween(DsqMotion.DurNormal, easing = DsqMotion.EaseStandard))
        alpha.animateTo(1f, animationSpec = tween(240))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(onBack = onBack, onOpenOptions = null)

        Spacer(modifier = Modifier.height(DsqSpacing.md))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomStart,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    PhotoFrame(
                        bitmap = preview,
                        sourceWidth = metadata.pixelWidth,
                        sourceHeight = metadata.pixelHeight,
                        ratio = ratio,
                        revealAnimated = false,
                        modifier = Modifier.graphicsLayerAlpha(alpha.value),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DsqSpacing.hairline)
                        .background(colors.bg),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(sweepProgress.value)
                            .height(DsqSpacing.hairline)
                            .background(colors.success),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(DsqSpacing.md))
        MetadataStrip(items = buildMetadataItems(metadata, ratio, exportAsJpeg = true))
        Spacer(modifier = Modifier.height(DsqSpacing.lg))
        BottomSavedStrip()
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onOpenOptions: (() -> Unit)?) {
    val colors = Dsq.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = DsqSpacing.screenH, vertical = DsqSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(iconRes = R.drawable.ic_close, contentDescription = "Close", onClick = onBack)
        Spacer(modifier = Modifier.weight(1f))
        if (onOpenOptions != null) {
            IconButton(iconRes = R.drawable.ic_options, contentDescription = "Options", onClick = onOpenOptions)
        }
    }
}

@Composable
private fun IconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val haptics = Dsq.haptics
    Box(
        modifier = Modifier
            .size(DsqSpacing.touch)
            .clip(CircleShape)
            .clickable {
                haptics.tapConfirm()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Dsq.colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun BottomActionBar(isSaving: Boolean, onSave: () -> Unit) {
    val colors = Dsq.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface1)
            .border(DsqSpacing.hairline, colors.divider, androidx.compose.ui.graphics.RectangleShape)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = DsqSpacing.screenH, vertical = DsqSpacing.md),
    ) {
        AnimatedContent(
            targetState = isSaving,
            transitionSpec = {
                (fadeIn(animationSpec = tween(DsqMotion.DurQuick)) togetherWith
                    fadeOut(animationSpec = tween(DsqMotion.DurQuick)))
            },
            label = "saveButton",
        ) { saving ->
            if (saving) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    HairlineProgress()
                    Spacer(modifier = Modifier.height(DsqSpacing.sm))
                    Text(
                        text = stringResource(R.string.saving),
                        style = Dsq.type.label,
                        color = colors.textSecondary,
                    )
                }
            } else {
                PrimaryButton(
                    label = stringResource(R.string.save),
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BottomSavedStrip() {
    val colors = Dsq.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface1)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = DsqSpacing.screenH, vertical = DsqSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "SAVED · Pictures/dsqueez",
            style = Dsq.type.numMicro,
            color = colors.success,
        )
    }
}

@Composable
private fun LoadingPanel(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(onBack = onBack, onOpenOptions = null)
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            HairlineProgress(modifier = Modifier.padding(horizontal = DsqSpacing.xxxl))
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ErrorPanel(reason: FailureReason, onBack: () -> Unit) {
    val text = when (reason) {
        FailureReason.UNSUPPORTED_FORMAT -> stringResource(R.string.error_unsupported)
        FailureReason.NATIVE_ENGINE_MISSING -> stringResource(R.string.error_native_missing)
        FailureReason.READ_FAILED, FailureReason.PROCESS_FAILED, FailureReason.WRITE_FAILED ->
            stringResource(R.string.error_save_failed)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(onBack = onBack, onOpenOptions = null)
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsqSpacing.screenH),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = text,
                style = Dsq.type.title,
                color = Dsq.colors.error,
            )
            Spacer(modifier = Modifier.height(DsqSpacing.xl))
            PrimaryButton(
                label = "Back",
                onClick = onBack,
                style = app.dsqueez.ui.components.PrimaryButtonStyle.Outlined,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun buildMetadataItems(
    metadata: PhotoMetadata,
    ratio: Float,
    exportAsJpeg: Boolean,
): List<MetadataItem> {
    val outW = metadata.desqueezedWidth(ratio)
    val outH = metadata.pixelHeight
    val outFormatLabel = if (exportAsJpeg || metadata.sourceFormat != SourceFormat.HEIC) "JPEG" else "HEIC"
    return listOf(
        MetadataItem("SOURCE", "${metadata.pixelWidth}×${metadata.pixelHeight}"),
        MetadataItem("RATIO", "${"%.2f".format(ratio)}×"),
        MetadataItem("OUTPUT", "${outW}×$outH"),
        MetadataItem("FORMAT", outFormatLabel),
    )
}

private fun Modifier.graphicsLayerAlpha(alpha: Float): Modifier =
    this.then(androidx.compose.ui.graphics.graphicsLayer { this.alpha = alpha })
