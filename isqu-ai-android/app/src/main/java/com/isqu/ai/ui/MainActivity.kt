package com.isqu.ai.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.isqu.ai.engine.IsquTfliteEngine
import com.isqu.ai.notifications.InferenceNotifier
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var engine: IsquTfliteEngine
    private lateinit var notifier: InferenceNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        engine = IsquTfliteEngine(this)
        notifier = InferenceNotifier(this)

        setContent {
            val autoDark = isSystemInDarkTheme()
            val colorScheme = if (autoDark) {
                darkColorScheme(
                    background = Color(0xFF0A0D12),
                    surface = Color(0xFF11161F),
                    primary = Color(0xFF10B981)
                )
            } else {
                lightColorScheme(
                    background = Color(0xFFF8FAFC),
                    surface = Color(0xFFFFFFFF),
                    primary = Color(0xFF059669)
                )
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    IsquMainScreen(engine = engine, notifier = notifier)
                }
            }
        }
    }

    override fun onDestroy() {
        engine.close()
        super.onDestroy()
    }
}

@Composable
fun IsquMainScreen(engine: IsquTfliteEngine, notifier: InferenceNotifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeModelLabel by remember { mutableStateOf("ISQU-Base (hanando/isqu)") }
    var searchQuery by remember { mutableStateOf("") }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val meta = engine.importCustomModel(uri, "Custom TFLite Model")
                activeModelLabel = meta.displayName
                notifier.notifyModelResult(
                    "Model Kustom Dimuat",
                    "${meta.displayName} aktif dengan ${meta.delegateActive}"
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding()) {
        Text(text = "ISQU AI — $activeModelLabel", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Cari riwayat chat...") }
        )
        Button(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
            Text("Import Model (.tflite)")
        }
    }
}

fun copyChatToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("ISQU AI Chat", text))
}
