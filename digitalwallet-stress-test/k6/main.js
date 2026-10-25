// Executor: ramping-vus. Mỗi VU = 1 account (1 session = 1 iteration).
// - __ITER === 0 : VU tạo account, chạy session liên tục tới testEndTime.
// - __ITER >  0  : VU đã xong session (vì test đã hết); sleep dài để k6 chờ
//                  gracefulStop, tránh busy-loop iteration rỗng.
// - Account bị dead (invariant) → VU tạo account mới ngay trong session loop.

import http from 'k6/http';
import { sleep } from 'k6';

import { loadConfig }      from './lib/config.js';
import { generateRunId, DataFactory } from './lib/data.js';
import { Random }          from './lib/random.js';
import { Monitor }         from './lib/metrics.js';
import { ApiClient }       from './lib/api.js';
import { MdpEngine }       from './lib/mdp.js';
import { Account }         from './lib/account.js';

const PROFILE  = __ENV.PROFILE  || 'smoke';
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Init context: cfg, monitor, options.
const cfg     = loadConfig(PROFILE);
const monitor = new Monitor(cfg);                 // Rate threshold khai báo 1 lần toàn cục

export const options = {
  scenarios: {
    default: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: cfg.profile.k6Stages(),
      gracefulRampDown: '30s',
      gracefulStop: '60s',
    },
  },
  // Bỏ qua response body lớn (ViewHistory) trong http_received tính tải client.
  // Nếu muốn đo băng thông thì bỏ hai dòng discardResponseBodies.
  discardResponseBodies: false,
  noConnectionReuse: false,
};

// ─────────────────────────────────────────────────────────────────────────────
// setup(): chuẩn bị receiving wallets (register+login+getWallet batch).
// Chạy 1 lần trước khi VU nào khởi động.
// ─────────────────────────────────────────────────────────────────────────────

export function setup() {
  const runId    = generateRunId();
  const rng      = new Random();
  const data     = new DataFactory(runId, rng);
  const api      = new ApiClient(BASE_URL, monitor);

  const n = cfg.profile.receivingWallets;
  const recv = [];
  for (let i = 0; i < n; i++) {
    const seq = data.nextSeq();
    recv.push({
      username: data.username(`recv_${seq}`),
      password: data.password(),
      fullName: `Receiver ${seq}`,
      email:    data.email(`recv_${seq}`),
    });
  }

  const JSON_HDR = { 'Content-Type': 'application/json' };

  // 1) Register batch (parallel). `headers` trong object form của http.batch
  // phải nằm trong `params`, không phải top-level.
  const regs = http.batch(recv.map((w) => ({
    method: 'POST',
    url: `${BASE_URL}/api/v1/auth/register`,
    body: JSON.stringify(w),
    params: { headers: JSON_HDR },
  })));
  const regBad = regs.filter((r) => r.status !== 201);
  if (regBad.length > 0) {
    for (let i = 0; i < Math.min(3, regBad.length); i++) {
      const r = regBad[i];
      console.error(`[setup] register failed #${i}: status=${r.status} body=${(r.body || '').toString().slice(0, 300)}`);
    }
    throw new Error(`setup: ${regBad.length}/${regs.length} register failed`);
  }

  // 2) Login batch.
  const logins = http.batch(recv.map((w) => ({
    method: 'POST',
    url: `${BASE_URL}/api/v1/auth/login`,
    body: JSON.stringify({ username: w.username, password: w.password }),
    params: { headers: JSON_HDR },
  })));
  const tokens = logins.map((r) => {
    try { return JSON.parse(r.body).data.accessToken; } catch (_) { return null; }
  });
  if (tokens.some((t) => !t)) throw new Error('setup: some logins failed');

  // 3) getWallet batch → walletNumber.
  const walletRes = http.batch(tokens.map((t) => ({
    method: 'GET',
    url: `${BASE_URL}/api/v1/wallets/me`,
    params: { headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${t}`,
    } },
  })));
  const walletNumbers = walletRes.map((r) => {
    try { return JSON.parse(r.body).data.walletNumber; } catch (_) { return null; }
  });
  if (walletNumbers.some((w) => !w)) throw new Error('setup: some getWallet failed');

  // 4) Smoke probe: 400/401/404.
  if (cfg.profile.name === 'smoke' && tokens.length > 0) {
    api.probeRegisterInvalid();
    api.probeMissingAuth();
    api.probeNotFound(tokens[0]);
  }

  const now = Date.now();
  return {
    runId,
    receivingWalletNumbers: walletNumbers,
    startTime: now,
    testEndTime: now + cfg.profile.totalDurationSec * 1000,
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// default(): 1 session / VU. VU tự chứa state (rng, data, api) — không share.
// ─────────────────────────────────────────────────────────────────────────────

export default function (data) {
  if (__ITER > 0) { sleep(3600); return; }

  const rng   = new Random();
  const dataF = new DataFactory(data.runId, rng);
  const api   = new ApiClient(BASE_URL, monitor);
  const mdp   = new MdpEngine(cfg.mdp, rng);

  let accCounter = 0;
  const makeAccount = () => new Account({
    seq: `vu${__VU}_${++accCounter}`, data: dataF, api, mdp, monitor, rng, cfg,
  });

  // Bootstrap: register → login → linkBank(1). Log qua ApiClient với state='bootstrap'.
  const bootstrap = (acc) => {
    acc._currentState = 'bootstrap';
    acc.register();
    if (!acc._registered) return false;
    acc.login();
    if (!acc.hasToken()) return false;
    acc.linkBank();
    return true;
  };

  let account = makeAccount();
  if (!bootstrap(account)) {
    for (let i = 0; i < 3; i++) {
      sleep(1);
      account = makeAccount();
      if (bootstrap(account)) break;
    }
    if (!account._registered || !account.hasToken()) return;
  }

  let currentZone   = 'auth';
  const startTime   = data.startTime;
  const testEndTime = data.testEndTime;

  while (Date.now() < testEndTime) {
    if (!account.isAlive()) {
      account = makeAccount();
      if (!bootstrap(account)) break;
      currentZone = 'auth';
      continue;
    }

    const elapsed  = (Date.now() - startTime) / 1000;
    const nextZone = mdp.nextZone(currentZone, account);
    const actions  = mdp.chooseActions(nextZone, account);

    for (const action of actions) {
      dispatch(account, action, nextZone, data.receivingWalletNumbers);
    }
    currentZone = nextZone;

    if (Date.now() >= testEndTime) break;
    account.sleepGap(elapsed, sleep);
  }
}

function dispatch(account, action, state, receivingWalletNumbers) {
  account._currentState = state;   // ← gán trước khi gọi
  switch (action) {
    case 'login':        return account.login();
    case 'logout':       return account.logout();  // không tạo request, không log
    case 'viewBalance':  return account.getWallet();
    case 'viewAccounts': return account.getLinkedBanks();
    case 'viewHistory':  return account.getHistory();
    case 'linkBank':     return account.linkBank();
    case 'topUp':        return account.topUp();
    case 'transfer':     return account.transfer(receivingWalletNumbers);
    case 'withdraw':     return account.withdraw();
    default: throw new Error(`[main] unknown action: "${action}"`);
  }
}