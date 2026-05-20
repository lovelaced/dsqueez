package app.dsqueez.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import app.dsqueez.photo.PhotoMetadata
import app.dsqueez.photo.PhotoSource
import app.dsqueez.photo.Pipeline
import app.dsqueez.photo.SaveResult
import app.dsqueez.settings.UserPrefs
import app.dsqueez.ui.components.PrimaryButton
import app.dsqueez.ui.components.SUPPORTED_RATIOS
import app.dsqueez.ui.theme.Dsq
import app.dsqueez.ui.theme.DsqSpacing
import androidx.compose.foundation.clickable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private enum class ItemStatus { QUEUED, PROCESSING, DONE, FAIL }

@Composable
fun BatchScreen(
    uris: List<Uri>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember { UserPrefs(context.applicationContext) }
    val exportAsJpeg by prefs.exportAsJpeg.collectAsState(initial = true)
    val ratio = SUPPORTED_RATIOS.first()

    val statusMap = remember { mutableStateMapOf<Uri, ItemStatus>().apply { uris.forEach { put(it, ItemStatus.QUEUED) } } }
    val metaMap = remember { mutableStateMapOf<Uri, PhotoMetadata>() }
    var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Load metadata up front so the list shows real dimensions.
    LaunchedEffect(uris) {
        uris.forEach { uri ->
            runCatching { PhotoSource.readMetadata(context, uri) }
                .onSuccess { metaMap[uri] = it }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Dsq.colors.bg)) {
        TopBar(onBack)

        Text(
            text = "${uris.size} PHOTOS · ${"%.2f".format(ratio)}×",
            style = Dsq.type.micro,
            color = Dsq.colors.textTertiary,
            modifier = Modifier.padding(horizontal = DsqSpacing.screenH, vertical = DsqSpacing.sm),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(DsqSpacing.sm),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = DsqSpacing.screenH,
                vertical = DsqSpacing.sm,
            ),
        ) {
            items(uris) { uri ->
                BatchRow(
                    uri = uri,
                    metadata = metaMap[uri],
                    status = statusMap[uri] ?: ItemStatus.QUEUED,
                    outputWidthForRatio = { (it.pixelWidth * ratio + 0.5f).toInt() },
                )
            }
        }

        BottomBar(
            enabled = !working && uris.all { statusMap[it] != ItemStatus.DONE },
            onSaveAll = {
                working = true
                scope.launch {
                    val sem = Semaphore(permits = 2)
                    val jobs = uris.map { uri ->
                        async(Dispatchers.Default) {
                            sem.withPermit {
                                val md = metaMap[uri]
                                    ?: runCatching { PhotoSource.readMetadata(context, uri) }
                                        .getOrNull()
                                        ?.also { metaMap[uri] = it }
                                if (md == null || !md.supported) {
                                    statusMap[uri] = ItemStatus.FAIL
                                    return@withPermit
                                }
                                statusMap[uri] = ItemStatus.PROCESSING
                                val res = Pipeline.process(context, md, ratio, exportAsJpeg)
                                statusMap[uri] = when (res) {
                                    is SaveResult.Success -> ItemStatus.DONE
                                    is SaveResult.Failure -> ItemStatus.FAIL
                                }
                            }
                        }
                    }
                    jobs.awaitAll()
                    working = false
                }
            },
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    val haptics = Dsq.haptics
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = DsqSpacing.screenH, vertical = DsqSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(DsqSpacing.touch)
                .clip(CircleShape)
                .clickable {
                    haptics.tapConfirm()
                    onBack()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "Close",
                tint = Dsq.colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun BatchRow(
    uri: Uri,
    metadata: PhotoMetadata?,
    status: ItemStatus,
    outputWidthForRatio: (PhotoMetadata) -> Int,
) {
    val colors = Dsq.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(colors.surface1)
            .border(DsqSpacing.hairline, colors.divider, RoundedCornerShape(2.dp))
            .padding(horizontal = DsqSpacing.md, vertical = DsqSpacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metadata?.displayName ?: uri.lastPathSegment ?: "photo",
                style = Dsq.type.body,
                color = colors.textPrimary,
                maxLines = 1,
            )
            if (metadata != null) {
                Spacer(modifier = Modifier.height(DsqSpacing.xs))
                Text(
                    text = "${metadata.pixelWidth}×${metadata.pixelHeight} → ${outputWidthForRatio(metadata)}×${metadata.pixelHeight}",
                    style = Dsq.type.numMicro,
                    color = colors.textTertiary,
                )
            }
        }
        StatusBadge(status)
    }
}

@Composable
private fun StatusBadge(status: ItemStatus) {
    val (text, color) = when (status) {
        ItemStatus.QUEUED -> stringResource(R.string.batch_status_queued) to Dsq.colors.textTertiary
        ItemStatus.PROCESSING -> stringResource(R.string.batch_status_processing) to Dsq.colors.accent
        ItemStatus.DONE -> stringResource(R.string.batch_status_done) to Dsq.colors.success
        ItemStatus.FAIL -> stringResource(R.string.batch_status_fail) to Dsq.colors.error
    }
    Text(
        text = text,
        style = Dsq.type.micro,
        color = color,
    )
}

@Composable
private fun BottomBar(enabled: Boolean, onSaveAll: () -> Unit) {
    val colors = Dsq.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface1)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = DsqSpacing.screenH, vertical = DsqSpacing.md),
    ) {
        PrimaryButton(
            label = stringResource(R.string.batch_save_all),
            onClick = onSaveAll,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

