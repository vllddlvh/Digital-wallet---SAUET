// Trạng thái + hành vi của MỘT account. Tự giữ:
//   - token & hạn token (T ~ N(μ, μ·j/3), clip ±j·μ)
//   - knownBalance + balanceSynced (đối soát ở getWallet 200)
//   - K, linkedBanks[], banksAvailableCount (hữu hạn)
//   - mustTopUp, isDead
//
// Không tự tạo account mới; tầng VU (main.js) xử lý thay thế khi isDead.
// Không tự sleep; main.js lo gap.

const MIN_GAP_SEC = 0.001;
const MAX_GAP_FACTOR = 10;   // clamp outlier của exponential ở 10×mean

export class Account {
  constructor({ seq, data, api, mdp, monitor, rng, cfg }) {
    this.seq = seq;
    this.data = data;
    this.api = api;
    this.mdp = mdp;
    this.monitor = monitor;
    this.rng = rng;
    this.cfg = cfg;

    // Identity
    this.username = data.username(seq);
    this.password = data.password();
    this.email = data.email(seq);
    this.fullName = data.fullName(seq);

    // Ràng buộc hữu hạn
    this.K = data.drawK(cfg.mdp.linkedBanks.kMin, cfg.mdp.linkedBanks.kMax);
    this.banksAvailableCount = this.K;
    this.linkedBanks = [];         // [{id, bankCode, bankName, accountNumber, holderName}]

    // Auth
    this.token = null;
    this.tokenExpiresAt = 0;

    // Wallet
    this.walletId = null;
    this.walletNumber = null;
    this.knownBalance = 0;
    this.balanceSynced = false;

    // MDP state
    this.currentZone = 'auth';
    this._currentState = 'auth';
    this.mustTopUp = false;
    this.isDead = false;

    // Lifecycle & timing
    this._registered = false;
    this.lastRttSec = 0;   // để main.js trừ khỏi gap khi sleep
  }

  // ── Lifecycle ────────────────────────────────────────────────────────────

  hasToken() { return this.token !== null; }
  isTokenExpired() { return this.token !== null && Date.now() >= this.tokenExpiresAt; }
  isAlive() { return !this.isDead; }

  register() {
    if (this._registered) throw new Error('register called twice');
    const res = this.api.register({
      username: this.username, password: this.password,
      fullName: this.fullName, email: this.email,
    }, this._currentState);
    this._updateRtt(res);
    if (res.status === 201) this._registered = true;
    return res;
  }

  login() {
    const res = this.api.login({ username: this.username, password: this.password }, this._currentState);
    this._updateRtt(res);
    if (res.status === 200 && res.body?.data?.accessToken) {
      this.token = res.body.data.accessToken;
      const muMs = this.cfg.profile.tokenTimeout.muSec * 1000;
      const j = this.cfg.mdp.tokenTimeoutJitter;
      const T = this.rng.normalClipped(muMs, muMs * j / 3, muMs * (1 - j), muMs * (1 + j));
      this.tokenExpiresAt = Date.now() + T;
    }
    return res;
  }

  logout() {
    this.token = null;
    this.tokenExpiresAt = 0;
  }

  // ── Confirmation ─────────────────────────────────────────────────────────

  getWallet() {
    const res = this.api.getWallet(this.token);
    this._updateRtt(res);

    if (res.status === 401) { this.logout(); return res; }

    if (res.status === 200 && res.body && res.body.data) {
      if (this.walletId === null) {
        this.walletId = res.body.data.id;
        this.walletNumber = res.body.data.walletNumber;
      }
      this._applyInvariantCheck(res.body.data.balance);
    }
    return res;
  }

  getLinkedBanks() {
    const res = this.api.getLinkedBanks(this.token);
    this._updateRtt(res);
    if (res.status === 401) this.logout();
    if (res.status === 200 && Array.isArray(res.body?.data)) {
      this.linkedBanks = res.body.data.map((b) => ({
        id: b.id,
        bankCode: b.bankCode,
        bankName: b.bankName,
        accountNumberMasked: b.accountNumberMasked,
        holderName: b.holderName,
      }));
    }
    return res;
  }

  getHistory() {
    const res = this.api.getTransactions(this.token);
    this._updateRtt(res);
    if (res.status === 401) this.logout();
    return res;
  }

  // ── Transaction ──────────────────────────────────────────────────────────

  linkBank() {
    if (this.banksAvailableCount <= 0) throw new Error('linkBank: no banks available');
    const kIndex = this.K - this.banksAvailableCount;
    const info = this.data.bankInfo(this.seq, kIndex);
    const res = this.api.linkBank(this.token, info);
    this._updateRtt(res);

    if (res.status === 401) { this.logout(); return res; }
    if (res.status === 201 && res.body && res.body.data) {
      this.linkedBanks.push({ id: res.body.data.id, ...info });
      this.banksAvailableCount -= 1;
    }
    return res;
  }

  topUp(amount) {
    if (this.linkedBanks.length === 0) return null;
    const amt = amount ?? this._randomAmount();
    const bank = this.linkedBanks[0];
    const res = this.api.topUp(this.token, {
      linkedBankAccountId: bank.id, amount: amt, description: `topup_${this.seq}`,
    }, [200], this._currentState);
    this._updateRtt(res);
    if (res.status === 401) { this.logout(); return res; }
    if (res.status === 200) {
      this.knownBalance += amt;
      this.mustTopUp = false;   // clear cờ
    }
    return res;
  }

  transfer(receivingWalletNumbers, amount) {
    const d = this._decideAmount(amount);
    if (d.replaceWithTopUp) {
      this.mustTopUp = true;          // bật cờ
      return this.topUp();            // thay action hiện tại bằng topUp
    }
    const target = this.rng.pickOne(receivingWalletNumbers);
    const expected = d.isOverdraw ? [400] : [200];
    const res = this.api.transfer(this.token, {
      targetWalletNumber: target, amount: d.amt, description: `transfer_${this.seq}`,
    }, expected, this._currentState);
    this._updateRtt(res);
    if (res.status === 401) { this.logout(); return res; }
    if (res.status === 200) this.knownBalance -= d.amt;
    else if (res.status === 400 && d.isOverdraw) this.mustTopUp = true;
    return res;
  }

  withdraw(amount) {
    if (this.linkedBanks.length === 0) return null;
    const d = this._decideAmount(amount);
    if (d.replaceWithTopUp) {
      this.mustTopUp = true;
      return this.topUp();
    }
    const bank = this.linkedBanks[0];
    const expected = d.isOverdraw ? [400] : [200];
    const res = this.api.withdraw(this.token, {
      linkedBankAccountId: bank.id, amount: d.amt, description: `withdraw_${this.seq}`,
    }, expected, this._currentState);
    this._updateRtt(res);
    if (res.status === 401) { this.logout(); return res; }
    if (res.status === 200) this.knownBalance -= d.amt;
    else if (res.status === 400 && d.isOverdraw) this.mustTopUp = true;
    return res;
  }

  _decideAmount(explicit) {
    if (explicit !== undefined) {
      return { amt: explicit, isOverdraw: explicit > this.knownBalance };
    }
    const m = this.cfg.mdp.money;
    const deliberate = this.rng.bernoulli(this.cfg.mdp.insufficientProb);

    if (deliberate) {
      const extra = this.rng.int(1, m.overflowExtra);
      return { amt: this.knownBalance + extra, isOverdraw: true };
    }

    // Not deliberate + không đủ → preventive topup (không gửi request fail vô ích)
    if (this.knownBalance < m.minAmount) {
      return { replaceWithTopUp: true };
    }

    const maxValid = Math.min(m.maxAmount, this.knownBalance);
    const amt = this.rng.int(m.minAmount, maxValid);
    return { amt, isOverdraw: false };
  }

  // ── Smoke probe: 3 định dạng exception ───────────────────────────────────

  /**
   * Gọi 1 lần/VU khi PROFILE=smoke. Yêu cầu: đã register() + login() thật.
   * Sinh 3 mẫu: 400 (validation), 401 (no token), 404 (not found).
   */
  probeExceptionFormats() {
    this.api.probeRegisterInvalid();
    this.api.probeMissingAuth();
    this.api.probeNotFound(this.token);
  }

  // ── Gap cho main.js ──────────────────────────────────────────────────────

  /**
   * Gap mục tiêu (giây) tới action kế tiếp.
   * Exponential với mean = stage.accounts / stage.targetRps.
   * Clamp [0.001, 10×mean] để tránh outlier cực đoan.
   */
  nextGapSec(elapsedSec) {
    const stage = this.cfg.profile.stageAt(elapsedSec);
    const mean = stage.accounts / stage.targetRps;
    const g = this.rng.exponential(mean);
    return Math.max(MIN_GAP_SEC, Math.min(g, MAX_GAP_FACTOR * mean));
  }

  /**
   * Sleep có trừ RTT của request vừa rồi, để giữ RPS sát mục tiêu.
   * Trả về gap đã sleep (giây) — hữu ích cho log nếu cần.
   */
  sleepGap(elapsedSec, sleepFn) {
    const gap = this.nextGapSec(elapsedSec);
    const adjusted = Math.max(MIN_GAP_SEC, gap - this.lastRttSec);
    sleepFn(adjusted);
    return adjusted;
  }

  // ── Nội bộ ───────────────────────────────────────────────────────────────

  _updateRtt(res) {
    if (res && res.durationMs !== null) this.lastRttSec = res.durationMs / 1000;
  }

  _randomAmount() {
    return this.rng.int(this.cfg.mdp.money.minAmount, this.cfg.mdp.money.maxAmount);
  }

  _applyInvariantCheck(serverBalance) {
    if (!this.balanceSynced) {
      this.knownBalance = serverBalance;
      this.balanceSynced = true;
      return;
    }
    if (this.knownBalance !== serverBalance) {
      this.monitor.recordInvariantViolation({
        accountSeq: this.seq, username: this.username, walletId: this.walletId,
        field: 'balance', client: this.knownBalance, server: serverBalance,
      });
      this.isDead = true;
    } else {
      this.knownBalance = serverBalance;
    }
  }
}