package id.rdpfread.remote.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import id.rdpfread.remote.core.ConnectionLog
import id.rdpfread.remote.ui.components.XyCard
import id.rdpfread.remote.ui.components.XyEmptyState
import id.rdpfread.remote.ui.components.XyErrorState
import id.rdpfread.remote.ui.components.XyIconPill
import id.rdpfread.remote.ui.components.XyIcons
import id.rdpfread.remote.ui.components.XyLoadingSkeletonCard
import id.rdpfread.remote.ui.components.XyLoadingSpinnerState
import id.rdpfread.remote.ui.components.XyPillButton
import id.rdpfread.remote.ui.components.XySectionLabel
import id.rdpfread.remote.ui.components.XySuccessStateBanner
import id.rdpfread.remote.ui.components.XyTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Satu layar status: versi aplikasi, status server rilis, izin mikrofon, dan
 * diagnostik yang bisa disalin. Tujuannya supaya pertanyaan "yang rusak di mana"
 * bisa dijawab tanpa buka log lewat adb.
 */
@Composable
fun StatusScreen(
    onBack: () -> Unit,
    onShowLog: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "-"
    }
    val versionCode = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toString()
        }.getOrNull() ?: "-"
    }
    val abi = remember { Build.SUPPORTED_ABIS.firstOrNull() ?: "-" }
    var micGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        XyTopBar(
            title = xy("Status", "Status"),
            onBack = onBack,
            actions = {
                XyIconPill(XyIcons.Info, onShowLog, flat = true, contentDescription = xy("Log sesi", "Session log"))
            },
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            XySectionLabel(xy("Aplikasi ini", "This app"))
            XyCard {
                KeyValue("Versi", "$versionName ($versionCode)")
                KeyValue("ABI", abi)
                KeyValue("Paket", context.packageName)
                KeyValue(
                    xy("Mikrofon", "Microphone"),
                    if (micGranted) {
                        xy("izin diberikan", "permission granted")
                    } else {
                        xy("izin belum diberikan", "permission not granted")
                    },
                )
            }

            XySectionLabel(xy("Kalau audio/mic bermasalah", "If audio/mic misbehaves"))
            XyCard {
                Text(
                    text = xy(
                        "1. Mode audio \"Putar di perangkat ini\" = suara + mic lewat kanal RDP (tanpa host agent).\n" +
                            "2. Mode \"Putar di komputer remote\" = lewat XyDeskRemoteHost.exe di PC (UDP 4433).\n" +
                            "3. Mic tidak jalan: cek izin mikrofon di baris atas, lalu di PC pastikan " +
                            "fDisableAudioCapture=0 pada RDP-Tcp.",
                        "1. Audio mode \"Play on this device\" = sound + mic over the RDP channel (no host agent).\n" +
                            "2. \"Play on the remote computer\" = through XyDeskRemoteHost.exe on the PC (UDP 4433).\n" +
                            "3. Mic not working: check the permission row above, then make sure the PC has " +
                            "fDisableAudioCapture=0 on RDP-Tcp.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = "XyVerse Technology Global",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun KeyValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Ambil field JSON sederhana tanpa parser: cukup untuk ringkasan status. */
private fun String.stringField(key: String): String? =
    Regex("\"$key\"\\s*:\\s*\"?([^,\"}\\n]+)\"?").find(this)?.groupValues?.get(1)?.trim()

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("RDPfread XD diagnostics", text))
}

