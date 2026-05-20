package app.dsqueez

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.dsqueez.share.parseIncomingUris
import app.dsqueez.ui.DsqueezApp
import app.dsqueez.ui.theme.DsqueezTheme

class MainActivity : ComponentActivity() {

    private var incomingUris by mutableStateOf<List<Uri>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        incomingUris = parseIncomingUris(intent)

        setContent {
            DsqueezTheme {
                DsqueezApp(
                    incomingUris = incomingUris,
                    onClearSelection = { incomingUris = emptyList() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val parsed = parseIncomingUris(intent)
        if (parsed.isNotEmpty()) incomingUris = parsed
    }
}
