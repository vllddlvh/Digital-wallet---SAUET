// k6/lib/api.js
//
// 1 hàm / endpoint. Tự ghi Monitor. KHÔNG tự đánh giá đúng/sai ở tầng logic
// (chỉ ghi nhận outcome); tầng Account quyết định state tiếp theo.

import http from 'k6/http';
import { check } from 'k6';

export class ApiClient {
  constructor(baseUrl, monitor) {
    this.baseUrl = baseUrl;
    this.monitor = monitor;
  }

  _request(method, path, { token, body, requestName, expected, state }) {
    const url = `${this.baseUrl}${path}`;
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    let res = null, actual = null, error = null;
    try {
      res = http.request(method, url, body ? JSON.stringify(body) : null, { headers });
      actual = res.status;
    } catch (e) {
      error = e && e.message ? e.message : String(e);
    }

    let parsedBody = null;
    if (res && res.body) {
      try { parsedBody = JSON.parse(res.body); } catch (_) { parsedBody = res.body; }
    }

    // Detail: chỉ ghi khi failure (body hoặc message network error).
    let detail = '';
    const expectedIncludes = (v) => v !== null && v !== undefined && expected.indexOf(v) >= 0;
    const isFailure = error || !expectedIncludes(actual);
    if (isFailure) {
      if (error) {
        detail = error;
      } else if (parsedBody !== null) {
        detail = typeof parsedBody === 'string' ? parsedBody : JSON.stringify(parsedBody);
      }
    }

    this.monitor.recordRequest({
      request: requestName, state, expected, actual, error, detail,
    });

    const resForCheck = res ?? { status: 0 };
    check(resForCheck, {
      [`${requestName}_status`]: (r) => expected.indexOf(r.status) >= 0,
    });

    return {
      status: actual,
      body:   parsedBody,
      durationMs: res ? res.timings.duration : null,
      error,
    };
  }

  // ── Public ───────────────────────────────────────────────────────────────

  register(payload) {
    return this._request('POST', '/api/v1/auth/register',
      { requestName: 'register', expected: [201], body: payload });
  }

  login(payload) {
    return this._request('POST', '/api/v1/auth/login',
      { requestName: 'login', expected: [200], body: payload });
  }

  getWallet(token) {
    return this._request('GET', '/api/v1/wallets/me',
      { requestName: 'viewBalance', expected: [200], token });
  }

  getLinkedBanks(token) {
    return this._request('GET', '/api/v1/linked-banks',
      { requestName: 'viewAccounts', expected: [200], token });
  }

  getTransactions(token) {
    return this._request('GET', '/api/v1/transactions',
      { requestName: 'viewHistory', expected: [200], token });
  }

  linkBank(token, payload) {
    return this._request('POST', '/api/v1/linked-banks',
      { requestName: 'linkBank', expected: [201], token, body: payload });
  }

  topUp(token, payload, expected = [200]) {
    return this._request('POST', '/api/v1/transactions/topup',
      { requestName: 'topUp', expected, token, body: payload });
  }

  transfer(token, payload, expected = [200]) {
    return this._request('POST', '/api/v1/transactions/transfer',
      { requestName: 'transfer', expected, token, body: payload });
  }

  withdraw(token, payload, expected = [200]) {
    return this._request('POST', '/api/v1/transactions/withdraw',
      { requestName: 'withdraw', expected, token, body: payload });
  }

  unlinkBank(token, id) {
    return this._request('DELETE', `/api/v1/linked-banks/${id}`,
      { requestName: 'unlinkBank', expected: [200], token });
  }


  probeRegisterInvalid() {
    return this._request('POST', '/api/v1/auth/register', {
      requestName: 'probe_register_400',
      expected: [400],
      body: { username: 'probe', password: '123', fullName: 'probe', email: 'bad' },
    });
  }
  probeMissingAuth() {
    return this._request('GET', '/api/v1/wallets/me', {
      requestName: 'probe_wallet_401',
      expected: [401],
    });
  }
  probeNotFound(token) {
    return this._request('DELETE', '/api/v1/linked-banks/2147483647', {
      requestName: 'probe_unlink_404',
      expected: [404],
      token,
    });
  }
}