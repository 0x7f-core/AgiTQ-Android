const C = {
  p1: '#FFFFFF',
  p2: '#AAAAAA',
  cp: '#80dfff',
  upperBand: '#e070c0',
  lowerBand: '#afd485',
  alert: '#ff4d4d',
  extremeFear: '#e8714f',
  fear: '#f0a0a0',
  neutral: '#AAAAAA',
  greed: '#5bb8e8',
  extremeGreed: '#b07cc0'
};

const KST_DATE_TIME = new Intl.DateTimeFormat('en-US', {
  timeZone: 'Asia/Seoul',
  year: '2-digit', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', hourCycle: 'h23'
});

const RATING_LABELS = {
  'extreme fear': '극공포 (Extreme Fear)',
  'fear': '공포 (Fear)',
  'neutral': '중립 (Neutral)',
  'greed': '탐욕 (Greed)',
  'extreme greed': '극탐욕 (Extreme Greed)'
};

function getHDContext(canvasId, W, H) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return null;
  const dpr = Math.min(window.devicePixelRatio || 1, 3);
  const pixelWidth = Math.round(W * dpr);
  const pixelHeight = Math.round(H * dpr);
  if (canvas.width !== pixelWidth) canvas.width = pixelWidth;
  if (canvas.height !== pixelHeight) canvas.height = pixelHeight;
  const aspectRatio = `${W}/${H}`;
  if (canvas.style.aspectRatio !== aspectRatio) canvas.style.aspectRatio = aspectRatio;
  const ctx = canvas.getContext('2d');
  ctx.setTransform(1, 0, 0, 1, 0, 0);
  ctx.clearRect(0, 0, pixelWidth, pixelHeight);
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = 'high';
  return ctx;
}

function etfClass(token) {
  if (token.includes('TQQQ')) return 'token-tqqq';
  if (token.includes('SPYM')) return 'token-spym';
  if (token.includes('SGOV')) return 'token-sgov';
  return '';
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

function fgiColor(v) {
  if (v >= 75) return C.extremeGreed;
  if (v >= 55) return C.greed;
  if (v >= 45) return C.neutral;
  if (v >= 25) return C.fear;
  return C.extremeFear;
}

function translateRating(rating) {
  return RATING_LABELS[String(rating || '').toLowerCase()] || rating || '-';
}

function formatMarketTime(epochSeconds) {
  if (!epochSeconds) return '-';
  const parts = KST_DATE_TIME.formatToParts(new Date(Number(epochSeconds) * 1000));
  const values = {};
  for (const part of parts) values[part.type] = part.value;
  return `${values.year}.${values.month}.${values.day}. ${values.hour}:${values.minute} 기준`;
}

// Original Scriptable drawBandChart: 90 points, upper/lower band and current price only.
function drawBandChart(canvasId, closes, upB, dnB) {
  const W = 500, H = 300;
  const ctx = getHDContext(canvasId, W, H);
  if (!ctx || !Array.isArray(closes) || closes.length < 200) return;

  const SIZE = Math.min(90, closes.length);
  const start = closes.length - SIZE;
  const pSet = new Array(SIZE);
  const uSet = new Array(SIZE).fill(NaN);
  const lSet = new Array(SIZE).fill(NaN);
  let rolling = 0;
  let min = Infinity;
  let max = -Infinity;

  for (let i = 0; i < closes.length; i++) {
    const price = Number(closes[i]);
    rolling += price;
    if (i >= 200) rolling -= Number(closes[i - 200]);
    if (i < start) continue;

    const index = i - start;
    pSet[index] = price;
    min = Math.min(min, price);
    max = Math.max(max, price);
    if (i >= 199) {
      const average = rolling / 200;
      uSet[index] = average * (1 + upB);
      lSet[index] = average * (1 - dnB);
      min = Math.min(min, lSet[index]);
      max = Math.max(max, uSet[index]);
    }
  }

  const range = (max - min) || 1;
  const xStep = 470 / Math.max(1, SIZE - 1);

  const plot = (data, color, width) => {
    ctx.beginPath();
    let started = false;
    for (let i = 0; i < data.length; i++) {
      const value = data[i];
      if (!Number.isFinite(value)) continue;
      const x = 15 + i * xStep;
      const y = 285 - ((value - min) / range) * 270;
      if (!started) {
        ctx.moveTo(x, y);
        started = true;
      } else {
        ctx.lineTo(x, y);
      }
    }
    ctx.strokeStyle = color;
    ctx.lineWidth = width;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.stroke();
  };

  plot(uSet, C.upperBand, 3);
  plot(lSet, C.lowerBand, 3);
  plot(pSet, C.cp, 5);
}

function drawFGIChart(canvasId, data) {
  const W = 480, H = 300;
  const ctx = getHDContext(canvasId, W, H);
  if (!ctx || !Array.isArray(data) || data.length < 2) return;

  const values = [];
  for (let i = Math.max(0, data.length - 90); i < data.length; i++) {
    const value = Number(data[i]?.y);
    if (Number.isFinite(value)) values.push(value);
  }
  if (values.length < 2) return;

  const PAD_L = 8, PAD_R = 8, PAD_T = 12, PAD_B = 12;
  const usableW = W - PAD_L - PAD_R;
  const usableH = H - PAD_T - PAD_B;
  const toY = v => PAD_T + (1 - v / 100) * usableH;
  const toX = i => PAD_L + (i / (values.length - 1)) * usableW;

  ctx.strokeStyle = 'rgba(170,170,170,.20)';
  ctx.lineWidth = 1;
  for (let level = 25; level <= 75; level += 25) {
    ctx.beginPath();
    ctx.moveTo(PAD_L, toY(level));
    ctx.lineTo(W - PAD_R, toY(level));
    ctx.stroke();
  }

  ctx.beginPath();
  values.forEach((v, i) => {
    if (i === 0) ctx.moveTo(toX(i), toY(v));
    else ctx.lineTo(toX(i), toY(v));
  });
  ctx.strokeStyle = 'rgba(255,255,255,.40)';
  ctx.lineWidth = 2;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  ctx.stroke();

  values.forEach((v, i) => {
    ctx.fillStyle = fgiColor(v);
    ctx.beginPath();
    ctx.arc(toX(i), toY(v), 4, 0, Math.PI * 2);
    ctx.fill();
  });
}

function drawGauge(canvasId, value) {
  const W = 320, H = 180;
  const ctx = getHDContext(canvasId, W, H);
  if (!ctx) return;

  const val = Math.max(0, Math.min(100, Number(value)));
  const cx = W / 2, cy = H - 20, radius = 120, arcThick = 10;

  for (let i = 0; i < 120; i++) {
    const t = i / 119;
    const angle = Math.PI + t * Math.PI;
    ctx.fillStyle = fgiColor(t * 100);
    ctx.beginPath();
    ctx.arc(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, arcThick / 2, 0, Math.PI * 2);
    ctx.fill();
  }

  const needleAngle = Math.PI + (val / 100) * Math.PI;
  const needleLen = radius - 8;
  ctx.fillStyle = C.p1;
  for (let i = 0; i < 35; i++) {
    const t = i / 34;
    const nx = cx + Math.cos(needleAngle) * needleLen * t;
    const ny = cy + Math.sin(needleAngle) * needleLen * t;
    const thick = 12 * (1 - t);
    ctx.beginPath();
    ctx.arc(nx, ny, Math.max(.5, thick / 2), 0, Math.PI * 2);
    ctx.fill();
  }

  ctx.beginPath();
  ctx.arc(cx, cy, 8, 0, Math.PI * 2);
  ctx.fill();

  const labelDist = radius + 22;
  const lx = cx + Math.cos(needleAngle) * labelDist;
  const ly = cy + Math.sin(needleAngle) * labelDist;
  ctx.fillStyle = C.p1;
  ctx.font = '700 22px -apple-system,BlinkMacSystemFont,"SF Pro Display",sans-serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(val.toFixed(0), lx, ly);
}

function tokenMarkup(token) {
  if (token.includes('·')) {
    return token.split('·').map(part => `<span class="${etfClass(part)}">${escapeHtml(part)}</span>`).join('<span class="token-dot">·</span>');
  }
  return `<span class="${etfClass(token)}">${escapeHtml(token)}</span>`;
}

function renderSignal(containerId, sig) {
  const el = document.getElementById(containerId);
  if (!el || !sig) return;

  const isAlert = Boolean(sig.alert);
  const rows = (sig.lines || []).slice(0, 2).map(row => {
    const token = row?.[0] ?? '';
    const action = row?.[1] ?? '';
    if (isAlert) return `<div class="sig-row"><span class="token-alert">${escapeHtml(`${token} ${action}`)}</span></div>`;
    return `<div class="sig-row">${tokenMarkup(token)}<span class="token-action"> ${escapeHtml(action)}</span></div>`;
  }).join('');

  const drawdown = Number(sig.drawdown);
  const dd = sig.position === 'ABOVE' && Number.isFinite(drawdown)
    ? `TQQQ 최고점 대비 ${drawdown.toFixed(1)}%`
    : 'TQQQ 최고점 대비 N/A';

  el.innerHTML = `${rows}
    <div class="status-text" style="color:${isAlert ? C.alert : 'var(--status)'}">${escapeHtml(sig.name || '-')}</div>
    <div class="dd-text">${dd}</div>`;
}

function renderFGI(fgi) {
  const ratingEl = document.getElementById('fgi-rating-text');
  const statsEl = document.getElementById('fgi-stats-text');

  if (!fgi || fgi.available === false || !Number.isFinite(Number(fgi.value))) {
    getHDContext('fgiHistoryChart', 480, 300);
    getHDContext('fgiGauge', 320, 180);
    ratingEl.textContent = 'FGI 데이터 없음';
    ratingEl.style.color = C.p2;
    statsEl.textContent = '';
    return;
  }

  const value = Number(fgi.value);
  const avg30 = Number(fgi.avg30);
  drawFGIChart('fgiHistoryChart', fgi.history || []);
  drawGauge('fgiGauge', value);

  ratingEl.textContent = translateRating(fgi.rating);
  ratingEl.style.color = fgiColor(value);

  const avgText = Number.isFinite(avg30) ? avg30.toFixed(0) : '-';
  const avgColor = Number.isFinite(avg30) ? fgiColor(avg30) : C.p2;
  statsEl.innerHTML = `
    <span style="color:${fgiColor(value)}">현재 ${value.toFixed(0)}</span>
    <span class="fgi-slash">&nbsp;/&nbsp;</span>
    <span style="color:${avgColor}">30일 평균 ${avgText}</span>`;
}

let activeRequest = null;
let requestSequence = 0;
let lastSuccessfulRenderAt = 0;
let autoRefreshTimer = null;
const AUTO_REFRESH_MS = 300000;

async function renderDashboard({ force = false } = {}) {
  if (activeRequest && !force) return activeRequest.promise;
  if (activeRequest && force) activeRequest.controller.abort();

  const controller = new AbortController();
  const sequence = ++requestSequence;
  const errorEl = document.getElementById('error-text');
  errorEl.textContent = '';

  const promise = (async () => {
    try {
      const data = await getMarket({ signal: controller.signal, forceRefresh: force });
      if (sequence !== requestSequence) return;
      document.getElementById('spx-updated').textContent = formatMarketTime(data.SPX?.mTime);
      document.getElementById('qqq-updated').textContent = formatMarketTime(data.QQQ?.mTime);

      drawBandChart('spxChart', data.SPX?.closes || [], .025, .025);
      drawBandChart('qqqChart', data.QQQ?.closes || [], .02, .02);
      renderSignal('spx-info', data.SPX?.signal);
      renderSignal('qqq-info', data.QQQ?.signal);
      renderFGI(data.FGI);
      lastSuccessfulRenderAt = Date.now();

      if (data.cache?.stale) {
        errorEl.textContent = '새 데이터 연결 지연: 마지막 정상 데이터를 표시합니다.';
      }
    } catch (error) {
      if (error?.name === 'AbortError') return;
      console.error(error);
      if (sequence === requestSequence) {
        errorEl.textContent = `데이터 연결 오류: ${error?.message || error}`;
      }
    } finally {
      if (activeRequest?.sequence === sequence) activeRequest = null;
    }
  })();

  activeRequest = { controller, promise, sequence };
  return promise;
}

function scheduleAutoRefresh(delay = AUTO_REFRESH_MS) {
  clearTimeout(autoRefreshTimer);
  if (document.hidden) return;
  autoRefreshTimer = setTimeout(async () => {
    await renderDashboard();
    scheduleAutoRefresh();
  }, Math.max(1000, delay));
}

renderDashboard().finally(() => scheduleAutoRefresh());

document.addEventListener('visibilitychange', () => {
  clearTimeout(autoRefreshTimer);
  if (document.hidden) return;
  const elapsed = Date.now() - lastSuccessfulRenderAt;
  if (!lastSuccessfulRenderAt || elapsed >= AUTO_REFRESH_MS) {
    renderDashboard().finally(() => scheduleAutoRefresh());
  } else {
    scheduleAutoRefresh(AUTO_REFRESH_MS - elapsed);
  }
});


const manualRefreshButton = document.getElementById('manual-refresh');
if (manualRefreshButton) {
  manualRefreshButton.addEventListener('click', async () => {
    if (manualRefreshButton.disabled) return;
    manualRefreshButton.disabled = true;
    manualRefreshButton.classList.add('refreshing');
    manualRefreshButton.textContent = '↻ 새로고침 중...';
    try {
      await renderDashboard({ force: true });
    } finally {
      manualRefreshButton.disabled = false;
      manualRefreshButton.classList.remove('refreshing');
      manualRefreshButton.textContent = '↻ 새로고침';
      scheduleAutoRefresh();
    }
  });
}
