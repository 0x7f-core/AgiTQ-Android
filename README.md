# AgiTQ-Android

Scriptable v3.0-γ strategy port for Android, Web and Cloudflare Worker.

## Structure

- `web/` — GitHub Pages dashboard
- `android/` — Android app + home-screen widget
- `cloudflare/` — Cloudflare Worker API proxy and shared strategy calculation
- `.github/workflows/` — GitHub Actions for web deployment and APK builds

## Strategy preserved from Scriptable

- Small: CNN Fear & Greed gauge
- Medium: QQQ 200-day SMA ±2.0% band + Single TS -25%
- Large: SPX 200-day SMA ±2.5% band + Multi TS -25% + CNN FGI
- TQQQ drawdown from the current cycle peak
- Korean action/status messages and the original buy/sell stages

## First setup

### 1. Cloudflare Worker

Deploy `cloudflare/worker.js` as a Cloudflare Worker. The public endpoint must be:

`https://YOUR-WORKER.workers.dev/api/market`

The Worker fetches Yahoo Finance and CNN FGI data and returns one JSON payload to both the website and Android widget.

### 2. Connect the web dashboard

Edit `web/js/api.js` and replace:

`https://YOUR-WORKER.workers.dev`

with your actual Worker URL.

### 3. Connect the Android app/widget

Edit `android/app/src/main/java/com/agitq/android/AgiTQConfig.kt` and replace the same placeholder Worker URL.

Also edit the GitHub Pages URL in `android/app/src/main/java/com/agitq/android/MainActivity.kt` if your GitHub username/repository path differs.

### 4. GitHub Pages

The `deploy-web.yml` workflow publishes `web/` to GitHub Pages after pushes to `main`. In the repository settings, set Pages source to **GitHub Actions** if GitHub asks for a source.

### 5. APK

The `build-apk.yml` workflow builds `android/app/build/outputs/apk/release/app-release.apk` and uploads it as a GitHub Actions artifact.

## Important

This is the first Android/Web port. The strategy logic is intentionally kept close to the supplied Scriptable code. Android launcher widgets have platform-specific layout limitations, so the first widget implementation prioritizes the same data, status and action information. Chart/bitmap rendering can be upgraded in the next pass to more closely reproduce the Scriptable charts and FGI gauge.
