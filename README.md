# RDPfread XD

Klien RDP Android untuk Windows dan Windows Server — fork dari
[XyDesk Remote](https://github.com/xykal/XyDesk-Remote) (Apache-2.0) yang
dibersihkan untuk ekosistem RDPfread & QVPS: **tanpa feed, tanpa chat global,
tanpa iklan, tanpa backend/telemetri pihak ketiga**.

Sambung langsung ke PC milikmu — tanpa relay cloud, tanpa akun.

[**Unduh APK**](https://github.com/qioceky/rdpfread-xd/releases) ·
[Daftar perubahan dari upstream](RDPFREAD-FORK.md) · [Panduan build](docs/BUILD.md)

---

## Isi

- **RDP klasik** (FreeRDP 3) ke port 3389: NLA, clipboard, audio, mikrofon,
  panel sesi bertab (Layar/Input/Tombol/Sesi), HUD tombol kustom + gamepad.
- **PC Stream** via host-agent (koneksi langsung/LAN).
- **Terminal SSH**, transfer file dalam sesi, perekam layar sesi,
  Wake-on-LAN, widget sambung cepat.
- Favorit & riwayat sambung cepat tersimpan lokal terenkripsi
  (Room + SQLCipher).

Yang dicabut dari upstream: feed komunitas, chat global + push OneSignal,
AdMob, Firebase, cloud RDP via backend mereka, sinkronisasi cloud, telemetri
sesi, self-update/OTA, dan gerbang sertifikat penanda tangan upstream.
Detail lengkap: [RDPFREAD-FORK.md](RDPFREAD-FORK.md).

**Butuh:** Windows 10/11 Pro, Enterprise, atau Windows Server (versi Home
tidak punya RDP server) · Android 7.0 (API 24) ke atas.
APK rilis saat ini dibangun sebagai debug build CI (per-ABI + universal).

## Lisensi

Apache-2.0, sama seperti upstream. Atribusi & NOTICE dipertahankan —
lihat `LICENSE`, `XYDESK-REMOTE-NOTICE.md`, dan `RDPFREAD-FORK.md`.
