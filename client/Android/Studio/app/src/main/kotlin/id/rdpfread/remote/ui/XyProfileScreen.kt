package id.rdpfread.remote.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.rdpfread.remote.ui.components.XyCard
import id.rdpfread.remote.ui.components.XyDialog
import id.rdpfread.remote.ui.components.XyGlassCard
import id.rdpfread.remote.ui.components.XyGlassRow
import id.rdpfread.remote.ui.components.XyGlassSectionLabel
import id.rdpfread.remote.ui.components.XyGlassTile
import id.rdpfread.remote.ui.components.XyIconPill
import id.rdpfread.remote.ui.components.XyIcons
import id.rdpfread.remote.ui.components.XyLogo
import id.rdpfread.remote.ui.components.XyOverlay
import id.rdpfread.remote.ui.components.XyPillButton
import id.rdpfread.remote.ui.components.XySegmented
import id.rdpfread.remote.ui.components.XyTileGrid
import id.rdpfread.remote.ui.components.XyTopBar
import kotlinx.coroutines.launch
import id.rdpfread.remote.ui.theme.XyThemeState

/**
 * Layar Profil — dashboard kartu.
 *
 * Enam seksi yang dulu berdesakan di nav bawah sekarang jadi ubin di grid,
 * dikelompokkan dengan judul seksi supaya hierarkinya jelas tanpa perlu
 * menggulir jauh. Gaya kartunya memakai `XyGlass`,
 * sesuai permintaan pengguna agar seluruh app satu bahasa visual.
 */
@Composable
internal fun XyProfileScreen(
    onMenu: () -> Unit,
    onOpenSection: (XySection) -> Unit,
    appPrefs: AppPrefs,
    deviceCount: Int,
    onShowLog: () -> Unit,
) {
    val context = LocalContext.current
    // BuildConfig tidak dipakai di lapisan UI; versi dibaca dari PackageManager
    // supaya tetap benar tanpa menambah konfigurasi build.
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
    }
    // Mode tema sebagai state lokal supaya segmen langsung berpindah; nilai
    // sebenarnya tetap disimpan lewat XyThemeState.set (prefs + state global).
    var themeMode by remember {
        mutableIntStateOf(
            if (XyThemeState.mode == Int.MIN_VALUE) XyThemeState.FOLLOW_SYSTEM else XyThemeState.mode,
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        XyTopBar(
            title = xy("Profil", "Profile"),
            actions = {
                XyIconPill(XyIcons.Menu, onMenu, flat = true, contentDescription = "Menu")
            },
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---- Kartu Utama Profil (Ronde 54: terpadu, bersih, tidak menumpuk) ----
            ProfileHeaderCard(versionName = versionName, deviceCount = deviceCount)

            // ---- seksi sebagai grid kartu ----
            XyGlassSectionLabel(xy("Pengaturan", "Settings"))
            XyTileGrid(columns = 2, spacing = 10.dp) {
                XySection.entries.forEach { section ->
                    // sectionTitle() @Composable; dipanggil di sini, di luar
                    // lambda item {} yang disimpan lalu dipanggil belakangan.
                    val title = sectionTitle(section)
                    val subtitle = sectionSubtitle(section)
                    item {
                        XyGlassTile(
                            icon = sectionIcon(section),
                            title = title,
                            subtitle = subtitle,
                            onClick = { onOpenSection(section) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // ---- tema ----
            XyGlassSectionLabel(xy("Tampilan", "Appearance"))
            XyGlassCard(padding = 14.dp) {
                Text(
                    xy("Mode tema", "Theme mode"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    xy(
                        "Berlaku seketika di semua layar, tanpa memuat ulang app.",
                        "Applies instantly on every screen, without reloading the app.",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                XySegmented(
                    options = listOf(
                        xy("Ikut sistem", "Follow system"),
                        xy("Gelap", "Dark"),
                        xy("Terang", "Light"),
                    ),
                    selectedIndex = themeMode,
                    onSelect = { index ->
                        themeMode = index
                        XyThemeState.set(appPrefs, index)
                    },
                )
            }

            // ---- lainnya ----
            XyGlassSectionLabel(xy("Lainnya", "More"))
            XyGlassRow(
                icon = XyIcons.Terminal,
                title = xy("Log koneksi", "Connection log"),
                onClick = onShowLog,
            )
            // ---- identitas app: footer ringkas, bukan kartu (rapi & clean) ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                XyLogo(modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    buildString {
                        append(xy("Versi", "Version"))
                        append(" ")
                        append(versionName ?: "-")
                        append("  ·  ")
                        append(xy("{0} perangkat tersimpan", "{0} saved devices", deviceCount))
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Kartu identitas lokal RDPfread: logo, versi, jumlah perangkat. Tanpa akun, tanpa cloud. */
@Composable
private fun ProfileHeaderCard(versionName: String?, deviceCount: Int) {
    XyGlassCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            XyLogo(modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "RDPfread",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProfileChip("v${versionName ?: "-"}")
                    ProfileChip(xy("{0} perangkat", "{0} devices", deviceCount))
                }
            }
        }
    }
}

/** Ikon per seksi — sama dengan pemetaan lama di nav bawah enam slot. */
private fun sectionIcon(section: XySection): ImageVector = when (section) {
    XySection.PERANGKAT -> XyIcons.Monitor
    XySection.TAMPILAN -> XyIcons.Fit
    XySection.KREDENSIAL -> XyIcons.Lock
    XySection.UMUM -> XyIcons.Sliders
    XySection.KEAMANAN -> XyIcons.Shield
    XySection.TENTANG -> XyIcons.Info
}

/** Keterangan singkat tiap seksi (ronde 45) supaya kartu tidak polos judul. */
@Composable
private fun sectionSubtitle(section: XySection): String = when (section) {
    XySection.PERANGKAT -> xy("Daftar & koneksi PC", "PC list & connections")
    XySection.TAMPILAN -> xy("Tema & gaya menu", "Theme & menu style")
    XySection.KREDENSIAL -> xy("Akun & kredensial RDP", "Accounts & RDP credentials")
    XySection.UMUM -> xy("Bahasa & perilaku app", "Language & app behavior")
    XySection.KEAMANAN -> xy("Kunci perangkat & sesi", "Device & session locks")
    XySection.TENTANG -> xy("Versi & kredit", "Version & credits")
}

/** Pil kecil berteks untuk deret info (versi, jumlah perangkat). */
@Composable
private fun ProfileChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
