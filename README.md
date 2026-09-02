# AgiTQ-Android

Scriptable v3.0-γ strategy port for Android, Web and Cloudflare Worker.

## Structure

- `web/` — GitHub Pages dashboard
- `android/` — Android app + independent SPX/QQQ/FGI home-screen widgets
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

The Worker fetches Yahoo Finance and CNN FGI data and returns one validated JSON payload to both the website and Android widgets. It keeps a five-minute edge cache and a seven-day last-known-good response for temporary upstream failures.

Yahoo and CNN requests run concurrently. SPX/QQQ and TQQQ histories are aligned by the New York trading date so small live-quote timestamp differences cannot drop the current session from the Scriptable strategy calculation.

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

The `build-apk.yml` workflow runs Android unit tests, lint, JavaScript syntax checks, builds `android/app/build/outputs/apk/release/app-release.apk`, and uploads it as a GitHub Actions artifact.

## Important

The strategy logic, Korean labels, colors, chart ranges and signal thresholds intentionally stay close to the supplied Scriptable code. The original Scriptable source is reference-only and is not modified.

Android stores only validated snapshots. Automatic and manual refreshes make one API request and then update all three widgets from the same snapshot. If a refresh fails, the last valid display remains in place. Automatic refresh is routed only through WorkManager and runs during the US regular session; manual refresh remains available at any time.

Release APKs use R8/resource shrinking, and the dashboard pauses its five-minute timer while hidden. These optimizations do not change the displayed Scriptable values or UI proportions.
