# RDPfread XD — fork of XyDesk Remote

Fork of [xykal/XyDesk-Remote](https://github.com/xykal/XyDesk-Remote) (Apache-2.0),
rebranded as **RDPfread** (`id.rdpfread.remote`). Upstream license & NOTICE files
are preserved (LICENSE, XYDESK-REMOTE-NOTICE.md). Not affiliated with XyVerse.

## Removed in this fork
- Global Chat (screens, outbox, backup, stickers) and its OneSignal push stack
- Community Feed + Official Channel screens; bottom nav is now Home/Profile only
- AdMob (`play-services-ads`) and all advertising hooks
- XyDesk backend integration: RdpFree cloud-RDP screens/API, CloudSync,
  AppUpdate + OTA data packs, presence heartbeat, anonymous session stats
  reporter, portal status fetch, community jokes/leaderboard/founder systems,
  feedback dialog, GitHub OAuth login for RdpFree
- "XyShield" AppChannelGate signer gate + SignerMark (blocked non-official builds)
- In-session floating chat overlay and Discord voice stage bar
- Remote-script installers (On-Mic / host setup `irm|iex` shortcuts)
- Their hosted backend code (`api/`, `web/`, `cloudflare/`) and non-APK workflows

## Kept
- Full RDP client on their FreeRDP fork: tabbed in-session panel
  (Screen/Input/Buttons/Session), custom HUD buttons + gamepad mapping,
  file transfer, session recorder, Wake-on-LAN, multi-monitor, SSH terminal
- PC Stream (direct/LAN). Internet pairing via upstream signaling hubs and
  "global PC ID" resolution are disabled (LAN discovery fallback remains)
- Encrypted on-device credential storage; no account, no telemetry

## Notes
- PC Stream hub defaults in `core-pc-stream` were emptied in the post-release
  tidy commit (no upstream worker is contacted by default).
- Host agent sources remain under `host-agent/` for future PC Stream use.
