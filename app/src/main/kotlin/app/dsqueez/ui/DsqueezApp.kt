package app.dsqueez.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.dsqueez.ui.screens.BatchScreen
import app.dsqueez.ui.screens.EditScreen
import app.dsqueez.ui.screens.EmptyScreen
import app.dsqueez.ui.theme.Dsq

@Composable
fun DsqueezApp(
    incomingUris: List<Uri>,
    onClearSelection: () -> Unit,
) {
    // Local state is the picked-by-button case; incomingUris is the share/view case.
    var pickedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val effectiveUris = if (incomingUris.isNotEmpty()) incomingUris else pickedUris

    val clearAll = {
        pickedUris = emptyList()
        onClearSelection()
    }

    BackHandler(enabled = effectiveUris.isNotEmpty()) { clearAll() }

    Box(modifier = Modifier.fillMaxSize().background(Dsq.colors.bg)) {
        when {
            effectiveUris.isEmpty() -> EmptyScreen(onUrisPicked = { pickedUris = it })
            effectiveUris.size == 1 -> EditScreen(uri = effectiveUris.first(), onBack = clearAll)
            else -> BatchScreen(uris = effectiveUris, onBack = clearAll)
        }
    }
}
