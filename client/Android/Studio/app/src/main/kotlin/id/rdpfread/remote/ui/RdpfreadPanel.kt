package id.rdpfread.remote.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import id.rdpfread.remote.core.ConnectionProfile
import id.rdpfread.remote.security.CredentialVault
import id.rdpfread.remote.ui.components.XyCard
import id.rdpfread.remote.ui.components.XyField
import id.rdpfread.remote.ui.components.XyPillButton
import id.rdpfread.remote.ui.components.XySectionLabel
import id.rdpfread.remote.ui.components.XySegmented
import id.rdpfread.remote.ui.components.XyStatusDot
import id.rdpfread.remote.ui.components.XyToggleRow
import id.rdpfread.remote.ui.components.XyTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Panel RDPfread: kendali sesi GitHub Actions milik pengguna dari dalam app.
 *
 * - Status live dibaca dari `rdp-status.json` di branch `status` repo publik
 *   `qioceky/RDPfread` (tanpa token).
 * - Dispatch memakai GitHub API + token pribadi pengguna, disimpan di
 *   [CredentialVault] (terenkripsi) bersama kata sandi RDP opsional untuk
 *   sambung 1-tap. Tidak ada backend perantara.
 */

private const val PANEL_STATUS_URL =
    "https://raw.githubusercontent.com/qioceky/RDPfread/status/rdp-status.json"
private const val PANEL_DISPATCH_URL =
    "https://api.github.com/repos/qioceky/RDPfread/actions/workflows/rdp.yml/dispatches"
private const val VAULT_PANEL_TOKEN = "rdpfread.panel.github_token"
private const val VAULT_PANEL_RDP_PASSWORD = "rdpfread.panel.rdp_password"

private val PANEL_RUNNERS =
    listOf("windows-2022", "windows-2025", "windows-latest", "ubuntu-24.04", "ubuntu-22.04", "ubuntu-latest")
private val PANEL_PRESETS =
    listOf("minimal", "ringan", "web", "dev", "full", "desktop", "game", "optimize")
private val PANEL_DEBLOAT = listOf("tidak", "berat", "full")
private val PANEL_FPS = listOf("standar", "120")
private val PANEL_LINUX_MODE = listOf("desktop", "polos")

internal data class RdpfreadNodeStatus(
    val active: Boolean,
    val hostname: String,
    val ip: String,
    val dns: String,
    val user: String,
    val preset: String,
    val os: String,
    val runNumber: String,
    val runUrl: String,
    val startedAtMs: Long,
    val expiresAtMs: Long,
) {
    val host: String get() = dns.ifBlank { ip }
    val live: Boolean get() = active && expiresAtMs > System.currentTimeMillis()
}

private fun parseIsoUtcMs(value: String?): Long {
    if (value.isNullOrBlank()) return 0L
    return runCatching {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        fmt.parse(value)?.time ?: 0L
    }.getOrDefault(0L)
}

private fun formatWib(ms: Long): String {
    if (ms <= 0L) return "—"
    val fmt = SimpleDateFormat("dd MMM HH:mm", Locale("id", "ID"))
    fmt.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    return fmt.format(ms) + " WIB"
}

private fun formatRemaining(expiresAtMs: Long): String {
    val diff = expiresAtMs - System.currentTimeMillis()
    if (diff <= 0L) return "berakhir"
    val totalMin = diff / 60_000L
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "$h jam $m mnt lagi" else "$m mnt lagi"
}

private suspend fun fetchRdpfreadStatus(): RdpfreadNodeStatus = withContext(Dispatchers.IO) {
    val conn = (URL(PANEL_STATUS_URL).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 10_000
        setRequestProperty("User-Agent", "RDPfread-Android")
    }
    try {
        check(conn.responseCode in 200..299) { "HTTP ${conn.responseCode}" }
        val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        RdpfreadNodeStatus(
            active = root.optBoolean("active", false),
            hostname = root.optString("hostname"),
            ip = root.optString("tailscale_ip"),
            dns = root.optString("tailscale_dns"),
            user = root.optString("rdp_user"),
            preset = root.optString("preset"),
            os = root.optString("os"),
            runNumber = root.optString("run_number"),
            runUrl = root.optString("run_url"),
            startedAtMs = parseIsoUtcMs(root.optString("started_at")),
            expiresAtMs = parseIsoUtcMs(root.optString("expires_at")),
        )
    } finally {
        conn.disconnect()
    }
}

/** Kirim workflow_dispatch. Mengembalikan null bila sukses, pesan error bila gagal. */
private suspend fun dispatchRdpfread(token: String, inputs: Map<String, String>): String? =
    withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("ref", "main")
            put("inputs", JSONObject(inputs))
        }.toString()
        val conn = (URL(PANEL_DISPATCH_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "RDPfread-Android")
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code == 204) {
                null
            } else {
                val err = runCatching {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }.getOrDefault("")
                val msg = runCatching { JSONObject(err).optString("message") }.getOrDefault("")
                "HTTP $code" + if (msg.isNotBlank()) " — $msg" else ""
            }
        } catch (t: Throwable) {
            t.message ?: "jaringan gagal"
        } finally {
            conn.disconnect()
        }
    }

@Composable
private fun PanelChips(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    )
                    .border(
                        1.dp,
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp),
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value.ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun RdpfreadPanelScreen(
    onBack: () -> Unit,
    onConnect: (ConnectionProfile) -> Unit,
    showNotice: (String) -> Unit,
) {
    val context = LocalContext.current
    val vault = remember { CredentialVault(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf<RdpfreadNodeStatus?>(null) }
    var statusError by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTick) {
        while (true) {
            runCatching { fetchRdpfreadStatus() }
                .onSuccess { status = it; statusError = null }
                .onFailure { statusError = it.message }
            delay(30_000)
            refreshTick++
        }
    }

    // Form dispatch
    var runnerIdx by remember { mutableIntStateOf(0) }
    var presetIdx by remember { mutableIntStateOf(0) }
    var debloatIdx by remember { mutableIntStateOf(0) }
    var fpsIdx by remember { mutableIntStateOf(0) }
    var linuxModeIdx by remember { mutableIntStateOf(0) }
    var optimasi by remember { mutableStateOf(true) }
    var perpanjang by remember { mutableStateOf(true) }
    var dispatching by remember { mutableStateOf(false) }

    // Pengaturan panel
    var tokenField by remember { mutableStateOf(vault.get(VAULT_PANEL_TOKEN) ?: "") }
    var rdpPassField by remember { mutableStateOf(vault.get(VAULT_PANEL_RDP_PASSWORD) ?: "") }

    val isWindows = PANEL_RUNNERS[runnerIdx].startsWith("windows")
    val st = status

    Column(Modifier.fillMaxSize()) {
        XyTopBar(
            title = xy("Panel RDPfread", "RDPfread Panel"),
            onBack = onBack,
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ---------- Status node ----------
            XyCard {
                XySectionLabel(xy("Status Node", "Node Status"))
                Spacer(Modifier.height(10.dp))
                when {
                    st == null && statusError == null -> Text(
                        xy("Memuat status…", "Loading status…"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    st == null && statusError != null -> {
                        Text(
                            xy("Status gagal dimuat: ", "Status failed to load: ") + statusError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(8.dp))
                        XyPillButton(
                            text = xy("Coba Lagi", "Retry"),
                            onClick = { refreshTick++ },
                            primary = false,
                            compact = true,
                        )
                    }
                    st != null -> {
                        val dotColor = when {
                            st.live -> Color(0xFF35D461)
                            st.active -> Color(0xFFFFB020)
                            else -> Color(0xFF9E9E9E)
                        }
                        val dotLabel = when {
                            st.live -> xy("AKTIF", "LIVE")
                            st.active -> xy("BERAKHIR", "ENDED")
                            else -> xy("MATI", "OFFLINE")
                        }
                        XyStatusDot(dotColor, dotLabel)
                        Spacer(Modifier.height(10.dp))
                        StatusRow(xy("Node", "Node"), st.hostname + st.runNumber.takeIf { it.isNotBlank() }?.let { " (#$it)" }.orEmpty())
                        StatusRow("IP Tailscale", st.ip)
                        StatusRow("DNS", st.dns)
                        StatusRow(xy("Pengguna", "User"), st.user)
                        StatusRow("Preset", st.preset)
                        StatusRow("OS", st.os)
                        StatusRow(xy("Mulai", "Started"), formatWib(st.startedAtMs))
                        StatusRow(
                            xy("Berakhir", "Ends"),
                            formatWib(st.expiresAtMs) + if (st.active) " (${formatRemaining(st.expiresAtMs)})" else "",
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            XyPillButton(
                                text = xy("SAMBUNG", "CONNECT"),
                                onClick = {
                                    val pass = vault.get(VAULT_PANEL_RDP_PASSWORD)
                                    onConnect(
                                        ConnectionProfile(
                                            host = st.host,
                                            port = 3389,
                                            username = st.user.ifBlank { "fread" },
                                            password = pass,
                                            label = st.hostname.ifBlank { "RDPfread" },
                                        ),
                                    )
                                },
                                enabled = st.live && st.host.isNotBlank(),
                                modifier = Modifier.weight(1f),
                            )
                            if (st.runUrl.isNotBlank()) {
                                XyPillButton(
                                    text = xy("Run", "Run"),
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(st.runUrl)),
                                            )
                                        }
                                    },
                                    primary = false,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        if (st.active && !st.live) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                xy(
                                    "Sesi tercatat aktif tetapi waktunya sudah lewat — rantai kemungkinan putus. Dispatch sesi baru di bawah.",
                                    "Session marked active but past its end time — the chain may be broken. Dispatch a new session below.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFB020),
                            )
                        }
                    }
                }
            }

            // ---------- Dispatch ----------
            XyCard {
                XySectionLabel(xy("Jalankan Sesi Baru", "Start New Session"))
                Spacer(Modifier.height(10.dp))
                Text(
                    xy("Runner / OS", "Runner / OS"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                PanelChips(PANEL_RUNNERS, runnerIdx) { runnerIdx = it }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Preset",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                PanelChips(PANEL_PRESETS, presetIdx) { presetIdx = it }
                if (isWindows) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Debloat",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    XySegmented(
                        options = PANEL_DEBLOAT.map {
                            when (it) {
                                "tidak" -> xy("Tidak", "None")
                                "berat" -> xy("Berat", "Heavy")
                                else -> "Full"
                            }
                        },
                        selectedIndex = debloatIdx,
                        onSelect = { debloatIdx = it },
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "FPS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    XySegmented(
                        options = listOf(xy("Standar", "Standard"), "120"),
                        selectedIndex = fpsIdx,
                        onSelect = { fpsIdx = it },
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        xy("Mode Linux", "Linux Mode"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    XySegmented(
                        options = listOf("Desktop", xy("Polos", "Plain")),
                        selectedIndex = linuxModeIdx,
                        onSelect = { linuxModeIdx = it },
                    )
                }
                Spacer(Modifier.height(6.dp))
                XyToggleRow(
                    title = xy("Optimasi", "Optimization"),
                    subtitle = xy("Bebaskan storage + debloat ringan", "Free storage + light debloat"),
                    checked = optimasi,
                    onCheckedChange = { optimasi = it },
                )
                XyToggleRow(
                    title = xy("Perpanjang otomatis", "Auto-extend"),
                    subtitle = xy("Estafet sesi berikutnya otomatis", "Hand over to the next session automatically"),
                    checked = perpanjang,
                    onCheckedChange = { perpanjang = it },
                )
                Spacer(Modifier.height(10.dp))
                XyPillButton(
                    text = if (dispatching) xy("Mengirim…", "Sending…") else xy("Dispatch Sesi", "Dispatch Session"),
                    onClick = {
                        val token = vault.get(VAULT_PANEL_TOKEN)?.trim().orEmpty()
                        if (token.isBlank()) {
                            showNotice(
                                xyNow(
                                    "Isi token GitHub dulu di Pengaturan Panel di bawah.",
                                    "Fill in the GitHub token in Panel Settings below first.",
                                ),
                            )
                            return@XyPillButton
                        }
                        dispatching = true
                        val inputs = mutableMapOf(
                            "runner" to PANEL_RUNNERS[runnerIdx],
                            "preset" to PANEL_PRESETS[presetIdx],
                            "optimasi" to if (optimasi) "ya" else "tidak",
                            "perpanjang_otomatis" to if (perpanjang) "ya" else "tidak",
                            "renewal" to "tidak",
                        )
                        if (isWindows) {
                            inputs["debloat"] = PANEL_DEBLOAT[debloatIdx]
                            inputs["fps"] = PANEL_FPS[fpsIdx]
                        } else {
                            inputs["linux_mode"] = PANEL_LINUX_MODE[linuxModeIdx]
                        }
                        scope.launch {
                            val error = dispatchRdpfread(token, inputs)
                            dispatching = false
                            if (error == null) {
                                showNotice(
                                    xyNow(
                                        "Dispatch terkirim — node siap dalam 1–3 menit, status di atas akan berubah.",
                                        "Dispatch sent — node ready in 1–3 minutes, status above will update.",
                                    ),
                                )
                                refreshTick++
                            } else {
                                showNotice(
                                    xyNow("Dispatch gagal: ", "Dispatch failed: ") + error,
                                )
                            }
                        }
                    },
                    enabled = !dispatching,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    xy(
                        "Kalau sesi sedang aktif, dispatch baru mengantre sebagai estafet berikutnya (tidak membatalkan sesi berjalan).",
                        "If a session is active, a new dispatch queues as the next relay (it does not cancel the running session).",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ---------- Pengaturan panel ----------
            XyCard {
                XySectionLabel(xy("Pengaturan Panel", "Panel Settings"))
                Spacer(Modifier.height(10.dp))
                XyField(
                    value = tokenField,
                    onValueChange = { tokenField = it },
                    label = xy("Token GitHub (PAT)", "GitHub Token (PAT)"),
                    hint = xy(
                        "Fine-grained: repo RDPfread, Actions Read & Write. Atau classic: scope workflow.",
                        "Fine-grained: RDPfread repo, Actions Read & Write. Or classic: workflow scope.",
                    ),
                    isPassword = true,
                )
                Spacer(Modifier.height(10.dp))
                XyField(
                    value = rdpPassField,
                    onValueChange = { rdpPassField = it },
                    label = xy("Kata Sandi RDP (opsional)", "RDP Password (optional)"),
                    hint = xy(
                        "Diisi = SAMBUNG langsung tanpa tanya. Sama dengan secret RDP_PASSWORD.",
                        "Filled = CONNECT goes straight in. Same as the RDP_PASSWORD secret.",
                    ),
                    isPassword = true,
                )
                Spacer(Modifier.height(12.dp))
                XyPillButton(
                    text = xy("Simpan Aman", "Save Securely"),
                    onClick = {
                        val t = tokenField.trim()
                        val p = rdpPassField
                        if (t.isBlank()) vault.remove(VAULT_PANEL_TOKEN) else vault.put(VAULT_PANEL_TOKEN, t)
                        if (p.isBlank()) vault.remove(VAULT_PANEL_RDP_PASSWORD) else vault.put(VAULT_PANEL_RDP_PASSWORD, p)
                        showNotice(
                            xyNow(
                                "Pengaturan panel tersimpan terenkripsi.",
                                "Panel settings saved encrypted.",
                            ),
                        )
                    },
                    primary = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
