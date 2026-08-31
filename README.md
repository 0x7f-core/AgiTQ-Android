# AgiTQ-Android

Scriptable v3.0-γ strategy port for Android, Web and Cloudflare Worker.

## Structure

- `web/` — GitHub Pages dashboard
- `android/` — Android app + home-screen widget
- `cloudflare/` — Cloudflare Worker API proxy
- `.github/workflows/` — GitHub Actions for web deployment and APK builds

## Strategy

- 200 SPX: 200-day SMA ±2.5%, Multi TS -25%
- 200 QQQ: 200-day SMA ±2.0%, Single TS -25%
- CNN Fear & Greed Index
- TQQQ drawdown from the current cycle peak

## Setup

1. Deploy `cloudflare/worker.js` to Cloudflare Workers.
2. Put the Worker URL into `web/js/api.js` and `android/.../AgiTQConfig.kt`.
3. Enable GitHub Pages using the `deploy-web.yml` workflow.
4. Run `build-apk.yml` to create the Android APK artifact.
