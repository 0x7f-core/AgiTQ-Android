const API_BASE = 'https://YOUR-WORKER.workers.dev';
async function getMarket(){
  const r=await fetch(`${API_BASE}/api/market`,{cache:'no-store'});
  if(!r.ok) throw new Error(`API ${r.status}`);
  return r.json();
}
