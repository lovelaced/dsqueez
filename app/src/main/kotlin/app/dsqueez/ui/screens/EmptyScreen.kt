package app.dsqueez.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.dsqueez.R
import app.dsqueez.ui.components.PrimaryButton
import app.dsqueez.ui.components.PrimaryButtonStyle
import app.dsqueez.ui.theme.Dsq
import app.dsqueez.ui.theme.DsqSpacing

@Composable
fun EmptyScreen(
    onUrisPicked: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
    ) { uris -> if (uris.isNotEmpty()) onUrisPicked(uris) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = DsqSpacing.xxxl),
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_lens_glyph),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
            Spacer(modifier = Modifier.height(DsqSpacing.xl))
            Text(
                text = stringResource(R.string.empty_hero),
                style = Dsq.type.display,
                color = Dsq.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(DsqSpacing.md))
            Text(
                text = stringResource(R.string.empty_subtitle),
                style = Dsq.type.micro,
                color = Dsq.colors.textTertiary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(DsqSpacing.xxl))
            PrimaryButton(
                label = stringResource(R.string.empty_cta),
                onClick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                        ),
                    )
                },
                style = PrimaryButtonStyle.Outlined,
                modifier = Modifier.widthIn(min = 200.dp),
            )
        }
    }
}
