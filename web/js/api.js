const API_BASE = 'https://agitq-api.0x7f-core.workers.dev';
const API_TIMEOUT_MS = 20000;

async function getMarket({ signal, timeoutMs = API_TIMEOUT_MS } = {}){
  const controller = new AbortController();
  const abort = () => controller.abort();
  if (signal?.aborted) abort();
  else signal?.addEventListener('abort', abort, { once: true });
  const timeout = setTimeout(abort, timeoutMs);

  try {
    const r = await fetch(`${API_BASE}/api/market`, {
      cache: 'no-store',
      signal: controller.signal,
    });
    if (!r.ok) throw new Error(`API ${r.status}`);
    const data = await r.json();
    if (!data?.SPX || !data?.QQQ || !data?.TQQQ) {
      throw new Error('API 응답 형식 오류');
    }
    return data;
  } finally {
    clearTimeout(timeout);
    signal?.removeEventListener('abort', abort);
  }
}
