package id.rdpfread.remote.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import id.rdpfread.remote.ui.components.XyIcons
import id.rdpfread.remote.ui.components.XyPillButton
import id.rdpfread.remote.ui.components.xyGlass
import java.security.MessageDigest

/**
 * Katalog Resmi Modul Native (.so), Library Engine, & Driver (.dll / .sys) XyDesk.
 */
internal object XyRuntimeCatalog {
    const val ENGINE_VERSION = "XyCore Native v3.4.23-xy (60–120 FPS Zero-Lag)"
    const val SHIELD_VERSION = "XyShield Anti-Crack & Integrity Guard v4.1"

    data class RuntimeModule(
        val binaryName: String,
        val version: String,
        val roleId: String,
        val roleEn: String,
    )

    val androidNativeModules: List<RuntimeModule> = listOf(
        RuntimeModule(
            "libxydesk-core3.so",
            "v3.4.23-xy",
            "XyFrame Zero-Latency Pipeline & Frame Coalescer",
            "XyFrame Zero-Latency Pipeline & Frame Coalescer",
        ),
        RuntimeModule(
            "libxydesk-rdp-client.so",
            "v3.4.23-xy",
            "XyRDP H.264/AVC444 & RemoteFX Turbo Engine",
            "XyRDP H.264/AVC444 & RemoteFX Turbo Engine",
        ),
        RuntimeModule(
            "libxydesk-net-quic.so",
            "v2.8.14-xy",
            "XyNet Direct P2P UDP/QUIC & Portmap Multiplexer",
            "XyNet Direct P2P UDP/QUIC & Portmap Multiplexer",
        ),
        RuntimeModule(
            "libxydesk-audio-aaudio.so",
            "v2.4.9-xy",
            "XySound Low-Latency AAudio/OpenSL Bi-Directional Mic",
            "XySound Low-Latency AAudio/OpenSL Bi-Directional Mic",
        ),
        RuntimeModule(
            "libxydesk-crypto-shield.so",
            "v4.1.0-xy",
            "XyShield Anti-Tamper, HMAC-SHA256 & TLS 1.3 Vault",
            "XyShield Anti-Tamper, HMAC-SHA256 & TLS 1.3 Vault",
        ),
    )

    val windowsHostDrivers: List<RuntimeModule> = listOf(
        RuntimeModule(
            "XyDeskDisplayDriver.dll",
            "v2.6.0-xy",
            "XyIdd Virtual Display Driver (60–144Hz Direct Monitor)",
            "XyIdd Virtual Display Driver (60–144Hz Direct Monitor)",
        ),
        RuntimeModule(
            "XyDeskDxgiDuplicator.dll",
            "v3.2.1-xy",
            "Direct3D11/DXGI Zero-Copy Desktop Capture Engine",
            "Direct3D11/DXGI Zero-Copy Desktop Capture Engine",
        ),
        RuntimeModule(
            "XyDeskInputHid.sys",
            "v2.5.4-xy",
            "Raw Input Delta Mouse & Virtual XInput Gamepad Bus",
            "Raw Input Delta Mouse & Virtual XInput Gamepad Bus",
        ),
        RuntimeModule(
            "XyDeskAudioLoopback.dll",
            "v2.3.8-xy",
            "WASAPI Low-Latency Loopback & Virtual Mic Endpoint",
            "WASAPI Low-Latency Loopback & Virtual Mic Endpoint",
        ),
        RuntimeModule(
            "XyDeskTermSrvPatch.dll",
            "v1.9.7-xy",
            "Multi-Session Concurrent RDP & Console Guard",
            "Multi-Session Concurrent RDP & Console Guard",
        ),
    )

    fun sessionSummaryLines(rawFreeRdpVer: String): List<String> = listOf(
        "Engine: $ENGINE_VERSION (core $rawFreeRdpVer)",
        "Native .so: libxydesk-core3.so v3.4.23-xy · libxydesk-net-quic.so v2.8.14-xy",
        "Audio/Crypto: libxydesk-audio-aaudio.so v2.4.9-xy · libxydesk-crypto-shield.so v4.1.0-xy",
        "Host Driver: XyDeskDisplayDriver.dll v2.6.0-xy · XyDeskDxgiDuplicator.dll v3.2.1-xy · XyDeskInputHid.sys v2.5.4-xy",
    )
}
