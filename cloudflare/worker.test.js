import assert from 'node:assert/strict';
import test from 'node:test';
import worker, { alignForSignal, analyzeSignal, marketDateKey } from './worker.js';

class MemoryCache {
  constructor() {
    this.entries = new Map();
  }

  async match(request) {
    return this.entries.get(request.url)?.clone();
  }

  async put(request, response) {
    this.entries.set(request.url, response.clone());
  }
}

function yahooPayload(base) {
  const timestamps = Array.from({ length: 260 }, (_, index) => 1_700_000_000 + index * 86_400);
  const closes = timestamps.map((_, index) => base + index * 0.5);
  return {
    chart: {
      result: [{
        timestamp: timestamps,
        meta: {
          regularMarketTime: timestamps.at(-1),
          regularMarketPrice: closes.at(-1),
        },
        indicators: { quote: [{ close: closes }] },
      }],
    },
  };
}

function scriptableSignalReference(sigCloses, tqqqCloses, mode, upB, dnB) {
  const period = 200;
  const moving = new Array(sigCloses.length).fill(NaN);
  for (let i = period - 1; i < sigCloses.length; i++) {
    let sum = 0;
    for (let j = 0; j < period; j++) sum += sigCloses[i - j];
    moving[i] = sum / period;
  }

  let position = 'BELOW';
  let localPeak = 0;
  let armed = true;
  let daysAbove = 0;
  let alert = false;
  let alertMessage = '';

  for (let i = 0; i < sigCloses.length; i++) {
    if (!Number.isFinite(moving[i])) continue;
    let today = position;
    if (sigCloses[i] >= moving[i] * (1 + upB)) today = 'ABOVE';
    else if (sigCloses[i] < moving[i] * (1 - dnB)) today = 'BELOW';

    if (position === 'BELOW' && today === 'ABOVE') {
      localPeak = tqqqCloses[i];
      armed = true;
      daysAbove = 1;
      alert = false;
    } else if (position === 'ABOVE' && today === 'BELOW') {
      localPeak = 0;
      daysAbove = 0;
      alert = false;
    } else if (today === 'ABOVE') {
      daysAbove++;
      if (tqqqCloses[i] > localPeak) localPeak = tqqqCloses[i];
      let trigger = false;
      if (tqqqCloses[i] <= localPeak * 0.75) {
        if (mode === 'single' && armed) {
          trigger = true;
          armed = false;
        } else if (mode === 'multi') {
          trigger = true;
          localPeak = tqqqCloses[i];
        }
      }
      alert = trigger;
      alertMessage = trigger ? '🚨 긴급대피 발동 (TS -25%)' : '';
    }
    position = today;
  }

  const current = tqqqCloses.at(-1);
  const drawdown = localPeak > 0 ? ((current - localPeak) / localPeak) * 100 : 0;
  if (position === 'BELOW') return { name:'하단 밴드 이탈 (전량 탈출)', alert:false, lines:[['TQQQ·SPYM','전량 매도'],['SGOV','대피 완료']], drawdown:null, position, daysAbove };
  if (alert) return { name:alertMessage, alert:true, lines:[['TQQQ','절반 매도'],['SPYM','즉시 전환']], drawdown, position, daysAbove };
  if (daysAbove === 1) return { name:'상단 밴드 돌파 1일 차', alert:false, lines:[['TQQQ','1/3 매수'],['SGOV','일부 매도']], drawdown, position, daysAbove };
  if (daysAbove === 2) return { name:'상단 밴드 돌파 2일 차', alert:false, lines:[['TQQQ','2/3 매수'],['SGOV','추가 매도']], drawdown, position, daysAbove };
  if (daysAbove === 3) return { name:'상단 밴드 돌파 3일 차', alert:false, lines:[['TQQQ','풀매수'],['SGOV','전량 매도']], drawdown, position, daysAbove };
  return { name:`상단 밴드 위 ${daysAbove}일 차 (추세 유지)`, alert:false, lines:[['TQQQ','보유 유지'],['SPYM','추가 매수']], drawdown, position, daysAbove };
}

test('market response is cached and last-good data survives an upstream outage', async () => {
  const cache = new MemoryCache();
  globalThis.caches = { default: cache };

  let upstreamCalls = 0;
  globalThis.fetch = async url => {
    upstreamCalls++;
    const value = String(url);
    if (value.includes('fearandgreed')) {
      const data = Array.from({ length: 90 }, (_, index) => ({
        x: 1_700_000_000_000 + index * 86_400_000,
        y: 30 + index / 3,
        rating: 'Neutral',
      }));
      return Response.json({ fear_and_greed_historical: { data } });
    }
    if (value.includes('TQQQ')) return Response.json(yahooPayload(40));
    if (value.includes('QQQ')) return Response.json(yahooPayload(300));
    return Response.json(yahooPayload(4_000));
  };

  const pending = [];
  const ctx = { waitUntil: promise => pending.push(promise) };
  const request = new Request('https://example.test/api/market');
  const first = await worker.fetch(request, {}, ctx);
  const firstData = await first.json();
  await Promise.all(pending);

  assert.equal(first.status, 200);
  assert.equal(firstData.version, 'v4.38');
  assert.equal(firstData.SPX.closes.length, 260);
  assert.equal(firstData.FGI.available, true);
  assert.equal(upstreamCalls, 4);

  const second = await worker.fetch(request, {}, { waitUntil() {} });
  assert.equal(second.headers.get('X-AgiTQ-Cache'), 'fresh');
  assert.equal(upstreamCalls, 4);

  const forcedPending = [];
  const forced = await worker.fetch(
    new Request('https://example.test/api/market?refresh=1'),
    {},
    { waitUntil: promise => forcedPending.push(promise) },
  );
  await Promise.all(forcedPending);
  assert.equal(forced.status, 200);
  assert.equal(forced.headers.get('X-AgiTQ-Cache'), 'upstream-forced');
  assert.equal(upstreamCalls, 8);

  const afterForced = await worker.fetch(request, {}, { waitUntil() {} });
  assert.equal(afterForced.headers.get('X-AgiTQ-Cache'), 'fresh');
  assert.equal(upstreamCalls, 8);

  cache.entries.delete('https://example.test/__agitq_cache/market-fresh');
  const partialPending = [];
  globalThis.fetch = async url => {
    upstreamCalls++;
    const value = String(url);
    if (value.includes('fearandgreed')) throw new Error('CNN offline');
    if (value.includes('TQQQ')) return Response.json(yahooPayload(40));
    if (value.includes('QQQ')) return Response.json(yahooPayload(300));
    return Response.json(yahooPayload(4_000));
  };
  const partial = await worker.fetch(
    request,
    {},
    { waitUntil: promise => partialPending.push(promise) },
  );
  const partialData = await partial.json();
  await Promise.all(partialPending);
  assert.equal(partial.status, 200);
  assert.equal(partialData.FGI.available, true);
  assert.equal(partialData.FGI.value, firstData.FGI.value);
  assert.match(partialData.FGI.error, /CNN offline/);

  cache.entries.delete('https://example.test/__agitq_cache/market-fresh');
  globalThis.fetch = async () => { throw new Error('offline'); };

  const stale = await worker.fetch(request, {}, { waitUntil() {} });
  const staleData = await stale.json();
  assert.equal(stale.status, 200);
  assert.equal(stale.headers.get('X-AgiTQ-Cache'), 'stale');
  assert.equal(staleData.cache.stale, true);
  assert.equal(staleData.SPX.price, firstData.SPX.price);
});

test('signal histories align by New York trading date, not quote second', () => {
  const start = Date.UTC(2025, 0, 2, 17, 0, 0) / 1000;
  const sigTimestamps = Array.from({ length: 205 }, (_, index) => start + index * 86_400);
  const tqqqTimestamps = sigTimestamps.map(timestamp => timestamp + 7_200);
  const sig = { timestamps: sigTimestamps, closes: sigTimestamps.map((_, index) => 100 + index) };
  const tqqq = { timestamps: tqqqTimestamps, closes: tqqqTimestamps.map((_, index) => 40 + index) };

  assert.equal(marketDateKey(sigTimestamps.at(-1)), marketDateKey(tqqqTimestamps.at(-1)));
  const aligned = alignForSignal(sig, tqqq);
  assert.equal(aligned.sig.closes.length, 205);
  assert.equal(aligned.tqqq.closes.at(-1), 244);
});

test('SPX multi and QQQ single strategies match the Scriptable state machine', () => {
  const closes = Array.from({ length: 260 }, (_, index) => index < 205 ? 100 : 130);
  const tqqq = Array.from({ length: 260 }, (_, index) => 40 + index * 0.25);
  tqqq[259] = 20;

  for (const strategy of [
    { mode:'multi', band:.025 },
    { mode:'single', band:.02 },
  ]) {
    const expected = scriptableSignalReference(closes, tqqq, strategy.mode, strategy.band, strategy.band);
    const actual = analyzeSignal(
      { closes },
      { closes: tqqq },
      200,
      strategy.mode,
      strategy.band,
      strategy.band,
    );
    const expectedSma = closes.slice(-200).reduce((sum, value) => sum + value, 0) / 200;
    assert.equal(actual.sma, expectedSma);
    assert.deepEqual(
      { ...actual, sma: undefined },
      { ...expected, sma: undefined },
    );
  }
});
