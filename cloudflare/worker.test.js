import assert from 'node:assert/strict';
import test from 'node:test';
import worker from './worker.js';

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
  assert.equal(firstData.version, 'v4.23');
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
  globalThis.fetch = async () => { throw new Error('offline'); };

  const stale = await worker.fetch(request, {}, { waitUntil() {} });
  const staleData = await stale.json();
  assert.equal(stale.status, 200);
  assert.equal(stale.headers.get('X-AgiTQ-Cache'), 'stale');
  assert.equal(staleData.cache.stale, true);
  assert.equal(staleData.SPX.price, firstData.SPX.price);
});
