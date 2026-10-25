
export class MdpEngine {
  constructor(mdpCfg, rng) {
    this.cfg = mdpCfg;
    this.rng = rng;
    this.transitions = mdpCfg.transitions;
    this.confirmationWeights = mdpCfg.weightsFor('confirmation');
    this.transactionWeights = mdpCfg.weightsFor('transaction');
  }

  /** Zone kế tiếp, có xét token hết hạn (force về Auth). */
  nextZone(currentZone, account) {
    if (account.isTokenExpired() && currentZone !== 'auth') return 'auth';
    if (currentZone === 'auth') return account.hasToken() ? 'confirmation' : 'auth';;
    if (currentZone === 'confirmation') return 'transaction';
    if (currentZone === 'transaction') {
      const stay = this.rng.bernoulli(this.transitions.transactionToTransaction);
      return stay ? 'transaction' : 'confirmation';
    }
    return 'auth';
  }

  /** Danh sách action cần thực thi khi vào `zone`. */
  chooseActions(zone, account) {
    if (zone === 'auth') {
      return account.hasToken() ? ['logout', 'login'] : ['login'];
    }
    if (zone === 'confirmation') {
      return [this._pickFromWeights(this.confirmationWeights)];
    }
    if (zone === 'transaction') {
      return [this._chooseTransactionAction(account)];
    }
    throw new Error(`[mdp] unknown zone: "${zone}"`);
  }

  _chooseTransactionAction(account) {
    // (1) Bắt buộc TopUp sau overdraw.
    if (account.mustTopUp) return 'topUp';

    const w = { ...this.transactionWeights };

    // (2) LinkBank giảm tuyến tính → 0 khi cạn K.
    w.linkBank = (w.linkBank || 0) * (account.banksAvailableCount / account.K);

    // (3) TopUp/Withdraw cần ≥1 bank.
    if (account.linkedBanks.length === 0) {
      w.topUp = 0;
      w.withdraw = 0;
    }

    // Transfer luôn có trọng số gốc > 0 → không bao giờ rỗng.
    return this._pickFromWeights(w);
  }

  _pickFromWeights(weights) {
    const items = [], ws = [];
    for (const k of Object.keys(weights)) {
      if (weights[k] > 0) { items.push(k); ws.push(weights[k]); }
    }
    if (items.length === 0) throw new Error('[mdp] no action available');
    return this.rng.weightedChoice(items, ws);
  }
}