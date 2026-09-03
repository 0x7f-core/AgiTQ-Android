const CACHE_TTL = 300;
const STALE_CACHE_TTL = 7 * 24 * 60 * 60;
// Two attempts must finish before the Android/Web 20-second client timeout.
const UPSTREAM_TIMEOUT_MS = 8000;
const UPSTREAM_ATTEMPTS = 2;
const API_VERSION = '4.38';

const NEW_YORK_DATE = new Intl.DateTimeFormat('en-US', {
  timeZone: 'America/New_York',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});

const YAHOO = {
  SPX: 'https://query1.finance.yahoo.com/v8/finance/chart/^GSPC?range=500d&interval=1d',
  QQQ: 'https://query1.finance.yahoo.com/v8/finance/chart/QQQ?range=500d&interval=1d',
  TQQQ: 'https://query1.finance.yahoo.com/v8/finance/chart/TQQQ?range=500d&interval=1d',
};

// CNN's Fear & Greed endpoint is an undocumented endpoint and can return
// HTTP 418 to non-browser-looking requests. We therefore use a browser-like
// header set and the dated/start-date form of the endpoint.
const FGI_URL = 'https://production.dataviz.cnn.io/index/fearandgreed/graphdata/2021-02-01';

const BROWSER_HEADERS = {
  'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
  'Accept': 'application/json, text/plain, */*',
  'Accept-Language': 'en-US,en;q=0.9',
  'Origin': 'https://www.cnn.com',
  'Referer': 'https://www.cnn.com/',
};

const cors = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type',
  'Cache-Control': `public, max-age=${CACHE_TTL}`,
};

function json(data, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      ...cors,
      'Content-Type': 'application/json; charset=utf-8',
      ...extraHeaders,
    },
  });
}

function cacheRequest(url, kind) {
  const key = new URL(url);
  key.search = '';
  key.pathname = `/__agitq_cache/${kind}`;
  return new Request(key.toString(), { method: 'GET' });
}

async function fetchWithRetry(url, options = {}, attempts = UPSTREAM_ATTEMPTS) {
  let lastError;
  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      const response = await fetch(url, {
        ...options,
        signal: AbortSignal.timeout(UPSTREAM_TIMEOUT_MS),
      });
      if (!response.ok) throw new Error(`${new URL(url).hostname} ${response.status}`);
      return response;
    } catch (error) {
      lastError = error;
      if (attempt < attempts) await new Promise(resolve => setTimeout(resolve, 250 * attempt));
    }
  }
  throw lastError;
}

function analyzeSignal(sig, tqqq, period, mode, upB, dnB) {
  let position = 'BELOW';
  let localPeak = 0;
  let tsArmed = true;
  let daysAbove = 0;
  let isAlert = false;
  let alertMsg = '';
  let rollingSum = 0;
  let latestSma = null;

  for (let i = 0; i < sig.closes.length; i++) {
    const cSig = sig.closes[i];
    rollingSum += cSig;
    if (i >= period) rollingSum -= sig.closes[i - period];
    if (i < period - 1) continue;

    const cSma = rollingSum / period;
    latestSma = cSma;
    const cTqqq = tqqq.closes[i];
    if (!Number.isFinite(cTqqq)) continue;

    const bUp = cSma * (1 + upB);
    const bDown = cSma * (1 - dnB);
    let todayPos = position;
    if (cSig >= bUp) todayPos = 'ABOVE';
    else if (cSig < bDown) todayPos = 'BELOW';

    if (position === 'BELOW' && todayPos === 'ABOVE') {
      localPeak = cTqqq;
      tsArmed = true;
      daysAbove = 1;
      isAlert = false;
    } else if (position === 'ABOVE' && todayPos === 'BELOW') {
      localPeak = 0;
      daysAbove = 0;
      isAlert = false;
    } else if (todayPos === 'ABOVE') {
      daysAbove++;
      if (cTqqq > localPeak) localPeak = cTqqq;

      let triggerTs = false;
      if (cTqqq <= localPeak * 0.75) {
        if (mode === 'single' && tsArmed) {
          triggerTs = true;
          tsArmed = false;
        } else if (mode === 'multi') {
          triggerTs = true;
          localPeak = cTqqq;
        }
      }

      isAlert = triggerTs;
      alertMsg = triggerTs ? '🚨 긴급대피 발동 (TS -25%)' : '';
    }

    position = todayPos;
  }

  const cur = tqqq.closes[tqqq.closes.length - 1];
  const dd = localPeak > 0 ? ((cur - localPeak) / localPeak) * 100 : 0;

  if (position === 'BELOW') return {
    name: '하단 밴드 이탈 (전량 탈출)', alert: false,
    lines: [['TQQQ·SPYM', '전량 매도'], ['SGOV', '대피 완료']],
    drawdown: null, position, daysAbove, sma: latestSma,
  };
  if (isAlert) return {
    name: alertMsg, alert: true,
    lines: [['TQQQ', '절반 매도'], ['SPYM', '즉시 전환']],
    drawdown: dd, position, daysAbove, sma: latestSma,
  };
  if (daysAbove === 1) return { name: '상단 밴드 돌파 1일 차', alert:false, lines:[['TQQQ','1/3 매수'],['SGOV','일부 매도']], drawdown:dd, position, daysAbove, sma:latestSma };
  if (daysAbove === 2) return { name: '상단 밴드 돌파 2일 차', alert:false, lines:[['TQQQ','2/3 매수'],['SGOV','추가 매도']], drawdown:dd, position, daysAbove, sma:latestSma };
  if (daysAbove === 3) return { name: '상단 밴드 돌파 3일 차', alert:false, lines:[['TQQQ','풀매수'],['SGOV','전량 매도']], drawdown:dd, position, daysAbove, sma:latestSma };
  return { name:`상단 밴드 위 ${daysAbove}일 차 (추세 유지)`, alert:false, lines:[['TQQQ','보유 유지'],['SPYM','추가 매수']], drawdown:dd, position, daysAbove, sma:latestSma };
}

function marketDateKey(timestamp) {
  return NEW_YORK_DATE.format(new Date(Number(timestamp) * 1000));
}

// Yahoo 종목별 누락 거래일이나 실시간 시각 차이가 있어도 같은 뉴욕 거래일끼리만
// 전략 계산에 사용한다. 정상 응답에서는 원본 Scriptable의 인덱스 결과와 동일하다.
function leveragedCloseIndex(tqqq) {
  const byDate = new Map();
  for (let index = 0; index < tqqq.timestamps.length; index++) {
    byDate.set(marketDateKey(tqqq.timestamps[index]), tqqq.closes[index]);
  }
  return byDate;
}

function alignForSignal(sig, tqqq, tqqqByDate = leveragedCloseIndex(tqqq)) {
  const timestamps = [];
  const sigCloses = [];
  const tqqqCloses = [];

  sig.timestamps.forEach((timestamp, index) => {
    const leveragedClose = tqqqByDate.get(marketDateKey(timestamp));
    const signalClose = sig.closes[index];
    if (Number.isFinite(signalClose) && Number.isFinite(leveragedClose)) {
      timestamps.push(timestamp);
      sigCloses.push(signalClose);
      tqqqCloses.push(leveragedClose);
    }
  });

  if (sigCloses.length < 200) throw new Error('Insufficient aligned market history');
  return {
    sig: { closes: sigCloses, timestamps },
    tqqq: { closes: tqqqCloses, timestamps },
  };
}

async function yahoo(url) {
  const r = await fetchWithRetry(url, {
    headers: {
      'User-Agent': 'Mozilla/5.0',
      'Accept': 'application/json',
    },
  });
  const j = await r.json();
  const res = j.chart.result?.[0];
  if (!res) throw new Error('Yahoo empty result');
  const q = res.indicators.quote[0];
  const closes = [], timestamps = [];
  (q.close || []).forEach((v, i) => {
    if (v != null && v > 0) {
      closes.push(v);
      timestamps.push(res.timestamp[i]);
    }
  });

  const mTime = Number(res.meta.regularMarketTime);
  const price = Number(res.meta.regularMarketPrice);
  if (!Number.isFinite(mTime) || mTime <= 0 || !Number.isFinite(price) || price <= 0) {
    throw new Error('Yahoo invalid market metadata');
  }
  if (timestamps.length && marketDateKey(timestamps[timestamps.length - 1]) === marketDateKey(mTime)) {
    closes[closes.length-1] = price;
  } else {
    closes.push(price);
    timestamps.push(mTime);
  }

  if (closes.length < 200 || closes.length !== timestamps.length) {
    throw new Error('Yahoo insufficient history');
  }

  return { closes, timestamps, mTime, price };
}

function fgiRating(v) {
  if (v >= 75) return 'Extreme Greed';
  if (v >= 55) return 'Greed';
  if (v >= 45) return 'Neutral';
  if (v >= 25) return 'Fear';
  return 'Extreme Fear';
}

async function fetchFGI() {
  const r = await fetchWithRetry(FGI_URL, { headers: BROWSER_HEADERS });
  const j = await r.json();

  // The dated endpoint normally contains historical data. Keep only the
  // latest 90 points, matching the original Scriptable widget.
  const data = j.fear_and_greed_historical?.data || [];
  if (data.length > 0) {
    const history = [];
    for (let index = Math.max(0, data.length - 90); index < data.length; index++) {
      const item = data[index];
      const x = item.x ?? item.date;
      const y = Number(item.y);
      if (x != null && Number.isFinite(y)) {
        history.push({ x, y, rating: item.rating || fgiRating(y) });
      }
    }
    if (history.length) return history;
  }

  // Fallback for a response that exposes only the current aggregate object.
  const current = j.fear_and_greed;
  if (current && Number.isFinite(Number(current.score))) {
    return [{
      x: current.timestamp ?? Date.now(),
      y: Number(current.score),
      rating: current.rating || fgiRating(Number(current.score)),
    }];
  }

  throw new Error('CNN response format changed');
}

export { analyzeSignal, alignForSignal, marketDateKey };

export default {
  async fetch(request, env, ctx) {
    if (request.method === 'OPTIONS') return new Response(null, { headers: cors });

    const url = new URL(request.url);

    if (url.pathname === '/' || url.pathname === '/health') {
      return json({ ok:true, service:'AgiTQ API', version:API_VERSION });
    }

    if (url.pathname !== '/api/market') {
      return json({ error:'Not found' }, 404);
    }

    const cache = caches.default;
    const forceRefresh = url.searchParams.get('refresh') === '1';
    const freshKey = cacheRequest(request.url, 'market-fresh');
    const staleKey = cacheRequest(request.url, 'market-last-good');
    if (!forceRefresh) {
      const cached = await cache.match(freshKey);
      if (cached) {
        const headers = new Headers(cached.headers);
        headers.set('X-AgiTQ-Cache', 'fresh');
        return new Response(cached.body, { status: cached.status, headers });
      }
    }

    let staleDataPromise;
    const readStaleData = () => {
      staleDataPromise ??= cache.match(staleKey)
        .then(response => response ? response.json() : null)
        .catch(() => null);
      return staleDataPromise;
    };

    try {
      // CNN과 Yahoo를 동시에 시작해 최악 응답 시간을 한 재시도 구간 안으로 제한한다.
      const [[spx, qqq, tqqq], fgiResult] = await Promise.all([
        Promise.all([
          yahoo(YAHOO.SPX),
          yahoo(YAHOO.QQQ),
          yahoo(YAHOO.TQQQ),
        ]),
        fetchFGI().then(
          history => ({ history, error: null }),
          error => ({ history: [], error: error?.message || String(error) }),
        ),
      ]);

      // FGI is kept separate so a temporary CNN block does not take down
      // the entire market API. This is important for Android widgets.
      let fgi = fgiResult.history;
      const fgiError = fgiResult.error;
      if (fgiError) {
        const staleData = await readStaleData();
        if (staleData?.FGI?.available && Array.isArray(staleData.FGI.history)) {
          fgi = staleData.FGI.history;
        }
      }

      const tqqqByDate = leveragedCloseIndex(tqqq);
      const spxAligned = alignForSignal(spx, tqqq, tqqqByDate);
      const qqqAligned = alignForSignal(qqq, tqqq, tqqqByDate);
      const spxSignal = analyzeSignal(spxAligned.sig, spxAligned.tqqq, 200, 'multi', .025, .025);
      const qqqSignal = analyzeSignal(qqqAligned.sig, qqqAligned.tqqq, 200, 'single', .02, .02);
      const latest = fgi[fgi.length-1];
      const averageCount = Math.min(30, fgi.length);
      let averageSum = 0;
      for (let index = fgi.length - averageCount; index < fgi.length; index++) {
        averageSum += fgi[index].y;
      }
      const avg30 = averageCount ? averageSum / averageCount : null;

      const payload = {
        version:`v${API_VERSION}`,
        updated:new Date().toISOString(),
        SPX:{ price:spx.price, closes:spx.closes, timestamps:spx.timestamps, mTime:spx.mTime, signal:spxSignal },
        QQQ:{ price:qqq.price, closes:qqq.closes, timestamps:qqq.timestamps, mTime:qqq.mTime, signal:qqqSignal },
        TQQQ:{ price:tqqq.price, closes:tqqq.closes, timestamps:tqqq.timestamps, mTime:tqqq.mTime },
        FGI:{
          value:latest?.y ?? null,
          rating:latest?.rating ?? null,
          avg30,
          history:fgi,
          available: fgi.length > 0,
          error: fgiError,
        },
      };

      const response = json(payload, 200, {
        'X-AgiTQ-Cache': forceRefresh ? 'upstream-forced' : 'upstream',
      });
      const lastGood = json(payload, 200, {
        'Cache-Control': `public, max-age=${STALE_CACHE_TTL}`,
        'X-AgiTQ-Cache': 'last-good',
      });
      ctx.waitUntil(Promise.all([
        cache.put(freshKey, response.clone()),
        cache.put(staleKey, lastGood),
      ]));
      return response;
    } catch (e) {
      const staleData = await readStaleData();
      if (staleData) {
        return json({
          ...staleData,
          cache: { stale: true, reason: e.message },
        }, 200, {
          'Cache-Control': 'public, max-age=60',
          'X-AgiTQ-Cache': 'stale',
          'Warning': '110 - "Response is stale"',
        });
      }
      return json({ error:'market_data_failed', message:e.message }, 502);
    }
  }
};
